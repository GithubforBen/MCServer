package de.hems.utils.ticket;

import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import de.hems.types.discord.AccountLink;
import de.hems.utils.Configuration;
import de.hems.utils.bot.verification.AccountLinkStore;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Moves the tickets of the old system out of {@code main-config.yml} into {@code tickets.yml}, once.
 * <p>
 * The old ones had a text and at most one answer; both become messages of the conversation. They keep
 * their numbers, so a player who remembers "ticket 3" still finds it.
 */
public final class TicketMigration {

    private TicketMigration() {
    }

    /**
     * @param configuration the main config the old tickets are in
     * @param store         where they go
     * @param links         to put a name and a minecraft account to the discord account that wrote them
     */
    public static void run(Configuration configuration, TicketStore store, AccountLinkStore links) {
        YamlConfiguration config = configuration.getConfig();
        if (!config.contains("tickets")) return;
        int moved = 0;
        for (int id : config.getIntegerList("tickets")) {
            String path = "ticket-" + id;
            if (!config.contains(path) || store.get(id) != null) continue;
            TicketData ticket = new TicketData();
            ticket.setId(id);
            ticket.setType(TicketType.byName(config.getString(path + ".type")));
            ticket.setTitle(config.getString(path + ".title", "Ticket"));
            ticket.setStatus(TicketStatus.byName(config.getString(path + ".status")));
            // the old system kept the discord account of the author, nothing else
            String author = config.getString(path + ".author");
            ticket.setDiscordId(author);
            AccountLink link = author == null ? null : links.byDiscord(author);
            if (link != null) {
                ticket.setMinecraftId(link.getMinecraftId());
                ticket.setAuthorName(link.getMinecraftName());
            } else {
                ticket.setAuthorName(author == null ? "?" : "Discord-Nutzer " + author);
            }
            long now = System.currentTimeMillis();
            ticket.setCreatedAt(now);
            ticket.setUpdatedAt(now);
            ticket.getMessages().add(new TicketMessage(ticket.getAuthorName(), false, TicketSource.DISCORD,
                    config.getString(path + ".description", ""), now));
            String response = config.getString(path + ".response");
            if (response != null && !response.isBlank() && !response.equals("-")) {
                ticket.getMessages().add(new TicketMessage("Admin", true, TicketSource.DISCORD, response, now));
            }
            ticket.setSeenByAuthor(ticket.getMessages().size());
            store.put(ticket);
            config.set(path, null);
            moved++;
        }
        config.set("tickets", null);
        configuration.save();
        System.out.println("Moved " + moved + " tickets from main-config.yml to tickets.yml.");
    }
}
