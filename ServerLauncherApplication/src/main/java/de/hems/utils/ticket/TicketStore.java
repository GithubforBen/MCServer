package de.hems.utils.ticket;

import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import de.hems.utils.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the tickets live: {@code tickets.yml} next to the launcher.
 * <p>
 * They used to be written into {@code main-config.yml}, the file with the proxy secret and the website
 * account, and numbered by counting them - two tickets in the same second got the same number and the
 * second overwrote the first. Here the next number is its own counter, taken under a lock.
 * <p>
 * What goes in and what comes out are copies: a ticket is changed by the discord bot, the website and the
 * network at once, and one of them sending a ticket off while another adds a message to the same list
 * would break the send.
 */
public class TicketStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<Integer, TicketData> tickets = new ConcurrentHashMap<>();

    public TicketStore() {
        this(new File("./tickets.yml"));
    }

    public TicketStore(File file) {
        this.file = file;
        this.config = YamlFiles.load(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("tickets");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            try {
                TicketData ticket = read(Integer.parseInt(key), entry);
                tickets.put(ticket.getId(), ticket);
            } catch (NumberFormatException ignored) {
                // a ticket without a number cannot be asked for
            }
        }
        System.out.println("Loaded " + tickets.size() + " tickets from " + file.getName());
    }

    private static TicketData read(int id, ConfigurationSection entry) {
        TicketData ticket = new TicketData();
        ticket.setId(id);
        ticket.setType(TicketType.byName(entry.getString("type")));
        ticket.setTitle(entry.getString("title", "Ticket"));
        ticket.setStatus(TicketStatus.byName(entry.getString("status")));
        ticket.setCreatedAt(entry.getLong("created-at"));
        ticket.setUpdatedAt(entry.getLong("updated-at"));
        ticket.setAuthorName(entry.getString("author-name", "?"));
        String minecraft = entry.getString("minecraft-id");
        if (minecraft != null && !minecraft.isBlank()) {
            try {
                ticket.setMinecraftId(UUID.fromString(minecraft));
            } catch (IllegalArgumentException ignored) {
                // the ticket still stands, it is just not reachable from the game
            }
        }
        ticket.setDiscordId(entry.getString("discord-id"));
        ticket.setAssignee(entry.getString("assignee"));
        ticket.setContext(entry.getString("context"));
        ticket.setThreadId(entry.getString("thread-id"));
        ticket.setSeenByAuthor(entry.getInt("seen-by-author"));
        List<TicketMessage> messages = new ArrayList<>();
        for (Map<?, ?> line : entry.getMapList("messages")) {
            Object source = line.get("source");
            TicketSource from = TicketSource.DISCORD;
            if (source != null) {
                try {
                    from = TicketSource.valueOf(source.toString());
                } catch (IllegalArgumentException ignored) {
                    // an unknown source is still a message
                }
            }
            Object at = line.get("at");
            messages.add(new TicketMessage(String.valueOf(line.get("author")),
                    Boolean.parseBoolean(String.valueOf(line.get("staff"))), from,
                    String.valueOf(line.get("text")), at instanceof Number number ? number.longValue() : 0L));
        }
        ticket.setMessages(messages);
        return ticket;
    }

    private void write(TicketData ticket) {
        String path = "tickets." + ticket.getId();
        config.set(path + ".type", ticket.getType().name());
        config.set(path + ".title", ticket.getTitle());
        config.set(path + ".status", ticket.getStatus().name());
        config.set(path + ".created-at", ticket.getCreatedAt());
        config.set(path + ".updated-at", ticket.getUpdatedAt());
        config.set(path + ".author-name", ticket.getAuthorName());
        config.set(path + ".minecraft-id", ticket.getMinecraftId() == null ? null : ticket.getMinecraftId().toString());
        config.set(path + ".discord-id", ticket.getDiscordId());
        config.set(path + ".assignee", ticket.getAssignee());
        config.set(path + ".context", ticket.getContext());
        config.set(path + ".thread-id", ticket.getThreadId());
        config.set(path + ".seen-by-author", ticket.getSeenByAuthor());
        List<Map<String, Object>> messages = new ArrayList<>();
        for (TicketMessage message : ticket.getMessages()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("author", message.getAuthor());
            line.put("staff", message.isStaff());
            line.put("source", message.getSource().name());
            line.put("text", message.getText());
            line.put("at", message.getAt());
            messages.add(line);
        }
        config.set(path + ".messages", messages);
    }

    /**
     * @return the next free ticket number, which is used up by asking
     */
    public synchronized int nextId() {
        int next = Math.max(config.getInt("next-id", 1), highestId() + 1);
        config.set("next-id", next + 1);
        return next;
    }

    private int highestId() {
        int highest = 0;
        for (int id : tickets.keySet()) highest = Math.max(highest, id);
        return highest;
    }

    /**
     * Stores a ticket, replacing the one with its number.
     *
     * @param ticket the ticket
     */
    public synchronized void put(TicketData ticket) {
        tickets.put(ticket.getId(), ticket.copy());
        write(ticket);
        YamlFiles.saveOrLog(config, file);
    }

    public TicketData get(int id) {
        TicketData ticket = tickets.get(id);
        return ticket == null ? null : ticket.copy();
    }

    /**
     * @return every ticket, newest change first
     */
    public List<TicketData> all() {
        List<TicketData> all = new ArrayList<>();
        for (TicketData ticket : tickets.values()) all.add(ticket.copy());
        all.sort(Comparator.comparingLong(TicketData::getUpdatedAt).reversed());
        return all;
    }

    /**
     * @param threadId a discord thread
     * @return the ticket it belongs to, or {@code null}
     */
    public TicketData byThread(String threadId) {
        if (threadId == null) return null;
        for (TicketData ticket : tickets.values()) {
            if (threadId.equals(ticket.getThreadId())) return ticket.copy();
        }
        return null;
    }
}
