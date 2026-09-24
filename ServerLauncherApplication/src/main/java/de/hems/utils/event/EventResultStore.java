package de.hems.utils.event;

import de.hems.types.event.EventResultData;
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
 * The results of the events that rank people by where they finished.
 * <p>
 * Built like the other stores: the launcher is the only one writing, the game server sends lines in, and
 * the file is what survives the arena being switched off before the event is settled.
 */
public class EventResultStore {

    private final File file;
    private final YamlConfiguration config;
    /** Event to player to line. */
    private final Map<UUID, Map<UUID, EventResultData>> results = new ConcurrentHashMap<>();

    public EventResultStore() {
        this(new File("./results.yml"));
    }

    public EventResultStore(File file) {
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
        ConfigurationSection section = config.getConfigurationSection("results");
        if (section == null) return;
        int count = 0;
        for (String eventKey : section.getKeys(false)) {
            ConfigurationSection event = section.getConfigurationSection(eventKey);
            if (event == null) continue;
            for (String playerKey : event.getKeys(false)) {
                ConfigurationSection entry = event.getConfigurationSection(playerKey);
                if (entry == null) continue;
                try {
                    EventResultData row = new EventResultData(UUID.fromString(eventKey),
                            UUID.fromString(playerKey), entry.getString("name", "?"));
                    row.setPlace(entry.getInt("place", 0));
                    row.setKills(entry.getInt("kills", 0));
                    row.setUpdatedAt(entry.getLong("updated-at", 0L));
                    results.computeIfAbsent(row.getEventId(), key -> new ConcurrentHashMap<>())
                            .put(row.getPlayerId(), row);
                    count++;
                } catch (IllegalArgumentException ignored) {
                    // a line whose ids cannot be read belongs to nobody
                }
            }
        }
        System.out.println("Loaded " + count + " result lines from " + file.getName());
    }

    /**
     * Takes lines in, replacing what was there for the same player.
     *
     * @param rows the lines
     * @return the ones that were stored
     */
    public synchronized List<EventResultData> put(List<EventResultData> rows) {
        List<EventResultData> stored = new ArrayList<>();
        for (EventResultData row : rows) {
            if (row == null || row.getEventId() == null || row.getPlayerId() == null) continue;
            results.computeIfAbsent(row.getEventId(), key -> new ConcurrentHashMap<>())
                    .put(row.getPlayerId(), row);
            String path = "results." + row.getEventId() + "." + row.getPlayerId();
            config.set(path + ".name", row.getPlayerName());
            config.set(path + ".place", row.getPlace());
            config.set(path + ".kills", row.getKills());
            config.set(path + ".updated-at", row.getUpdatedAt());
            stored.add(row);
        }
        if (!stored.isEmpty()) save();
        return stored;
    }

    /**
     * @param eventId the event
     * @return its lines, in no particular order
     */
    public List<EventResultData> getRowsOf(UUID eventId) {
        Map<UUID, EventResultData> rows = eventId == null ? null : results.get(eventId);
        return rows == null ? new ArrayList<>() : new ArrayList<>(rows.values());
    }

    /**
     * Forgets an event's lines, once it is settled or deleted.
     *
     * @param eventId the event
     * @return how many lines went
     */
    public synchronized int discard(UUID eventId) {
        Map<UUID, EventResultData> removed = eventId == null ? null : results.remove(eventId);
        if (removed == null) return 0;
        config.set("results." + eventId, null);
        save();
        return removed.size();
    }

    private synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
