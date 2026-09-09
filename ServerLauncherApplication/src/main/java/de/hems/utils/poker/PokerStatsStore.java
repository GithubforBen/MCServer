package de.hems.utils.poker;

import de.hems.types.poker.PokerStatsData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the record of the poker nights lives.
 * <p>
 * Written through on every change, like {@link de.hems.utils.event.RunStore}: these rows are the ranking
 * that pays out prizes at the end of the night, and they are also the receipt for chips that are still on a
 * table. A casino server that dies takes its tables with it, and what makes that survivable is that the
 * launcher already knows how much was sitting in front of whom.
 * <p>
 * The store holds no money and moves none. It is a record; the bits live in
 * {@link de.hems.utils.money.MoneyStore} and move only through it.
 */
public class PokerStatsStore {

    private final File file;
    private final YamlConfiguration config;
    /** Rows by event, then by player. */
    private final Map<UUID, Map<UUID, PokerStatsData>> rows = new ConcurrentHashMap<>();

    public PokerStatsStore() {
        this(new File("./poker.yml"));
    }

    public PokerStatsStore(File file) {
        this.file = file;
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("nights");
        if (section == null) return;
        int count = 0;
        for (String eventKey : section.getKeys(false)) {
            ConfigurationSection ofEvent = section.getConfigurationSection(eventKey);
            if (ofEvent == null) continue;
            UUID eventId;
            try {
                eventId = UUID.fromString(eventKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            for (String playerKey : ofEvent.getKeys(false)) {
                ConfigurationSection entry = ofEvent.getConfigurationSection(playerKey);
                if (entry == null) continue;
                PokerStatsData row = read(eventId, playerKey, entry);
                if (row == null) continue;
                rows.computeIfAbsent(eventId, key -> new ConcurrentHashMap<>()).put(row.getPlayerId(), row);
                count++;
            }
        }
        System.out.println("Loaded " + count + " poker rows from " + file.getName());
    }

    private static PokerStatsData read(UUID eventId, String playerKey, ConfigurationSection entry) {
        UUID playerId;
        try {
            playerId = UUID.fromString(playerKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
        PokerStatsData row = new PokerStatsData(eventId, playerId, entry.getString("name", "?"));
        row.setBoughtIn(entry.getInt("bought-in", 0));
        row.setCashedOut(entry.getInt("cashed-out", 0));
        row.setOpenStack(entry.getInt("open-stack", 0));
        row.setHands(entry.getInt("hands", 0));
        row.setHandsWon(entry.getInt("hands-won", 0));
        row.setBiggestPot(entry.getInt("biggest-pot", 0));
        row.setFirstSeenAt(entry.getLong("first-seen-at", 0L));
        row.setUpdatedAt(entry.getLong("updated-at", 0L));
        return row;
    }

    private void write(PokerStatsData row) {
        String path = "nights." + row.getEventId() + "." + row.getPlayerId();
        config.set(path + ".name", row.getPlayerName());
        config.set(path + ".bought-in", row.getBoughtIn());
        config.set(path + ".cashed-out", row.getCashedOut());
        config.set(path + ".open-stack", row.getOpenStack());
        config.set(path + ".hands", row.getHands());
        config.set(path + ".hands-won", row.getHandsWon());
        config.set(path + ".biggest-pot", row.getBiggestPot());
        config.set(path + ".first-seen-at", row.getFirstSeenAt());
        config.set(path + ".updated-at", row.getUpdatedAt());
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * @return every row of every night, as a fresh list the caller may keep
     */
    public List<PokerStatsData> getRows() {
        List<PokerStatsData> all = new ArrayList<>();
        for (Map<UUID, PokerStatsData> ofEvent : rows.values()) all.addAll(ofEvent.values());
        return all;
    }

    /**
     * @param eventId the night to look at
     * @return its rows
     */
    public List<PokerStatsData> getRowsOf(UUID eventId) {
        if (eventId == null) return List.of();
        Map<UUID, PokerStatsData> ofEvent = rows.get(eventId);
        return ofEvent == null ? List.of() : new ArrayList<>(ofEvent.values());
    }

    public PokerStatsData getRow(UUID eventId, UUID playerId) {
        if (eventId == null || playerId == null) return null;
        Map<UUID, PokerStatsData> ofEvent = rows.get(eventId);
        return ofEvent == null ? null : ofEvent.get(playerId);
    }

    /**
     * Stores a row as the casino server sent it.
     * <p>
     * A whole row rather than a difference, which holds because exactly one server plays a given night and
     * therefore nobody else writes its rows. What is refused is a row from a server that has fallen behind:
     * a write is only taken if it is at least as new as what is stored, so a late message from a dying
     * server cannot resurrect a stack that has already been paid out.
     *
     * @param row the row to store
     * @return the row as it now stands, or {@code null} if the write was refused
     */
    public synchronized PokerStatsData put(PokerStatsData row) {
        if (row == null || row.getEventId() == null || row.getPlayerId() == null) return null;
        PokerStatsData existing = getRow(row.getEventId(), row.getPlayerId());
        if (existing != null && row.getUpdatedAt() < existing.getUpdatedAt()) return null;
        rows.computeIfAbsent(row.getEventId(), key -> new ConcurrentHashMap<>())
                .put(row.getPlayerId(), row);
        write(row);
        save();
        return row;
    }

    /**
     * Marks the chips of a night as no longer being on a table.
     * <p>
     * Called once the launcher has handed the open stacks back, so the same stack can never be paid out
     * twice - not by a second settlement, and not by a casino server that comes back up afterwards.
     *
     * @param eventId the night
     * @return the rows that were changed
     */
    public synchronized List<PokerStatsData> clearOpenStacks(UUID eventId) {
        List<PokerStatsData> changed = new ArrayList<>();
        for (PokerStatsData row : getRowsOf(eventId)) {
            if (row.getOpenStack() <= 0) continue;
            // what was on the table has been handed back, so from here on it counts as cashed out
            row.addCashedOut(row.getOpenStack());
            row.setOpenStack(0);
            write(row);
            changed.add(row);
        }
        if (!changed.isEmpty()) save();
        return changed;
    }

    /**
     * @param eventId the night to forget
     * @return how many rows went with it
     */
    public synchronized int discard(UUID eventId) {
        Map<UUID, PokerStatsData> removed = rows.remove(eventId);
        if (removed == null || removed.isEmpty()) return 0;
        config.set("nights." + eventId, null);
        save();
        return removed.size();
    }
}
