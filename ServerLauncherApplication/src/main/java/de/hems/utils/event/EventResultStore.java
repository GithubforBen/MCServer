package de.hems.utils.event;

import de.hems.utils.YamlFiles;
import de.hems.types.event.EventResultData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
    /** Events whose game has reported that it is over. */
    private final java.util.Set<UUID> finished = ConcurrentHashMap.newKeySet();

    public EventResultStore() {
        this(new File("./results.yml"));
    }

    public EventResultStore(File file) {
        this.file = file;
        this.config = YamlFiles.load(file);
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
        for (String id : config.getStringList("finished")) {
            try {
                finished.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // an id nobody can read belongs to no event
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
     * Notes that the game of an event is over, so it can be settled.
     *
     * @param eventId the event
     */
    public synchronized void markFinished(UUID eventId) {
        if (eventId == null || !finished.add(eventId)) return;
        writeFinished();
        save();
    }

    /**
     * @param eventId the event
     * @return whether its game has said it is over
     */
    public boolean isFinished(UUID eventId) {
        return eventId != null && finished.contains(eventId);
    }

    private void writeFinished() {
        List<String> ids = new ArrayList<>();
        for (UUID id : finished) ids.add(id.toString());
        config.set("finished", ids);
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
        if (eventId == null) return 0;
        Map<UUID, EventResultData> removed = results.remove(eventId);
        if (finished.remove(eventId)) writeFinished();
        config.set("results." + eventId, null);
        if (removed == null) {
            save();
            return 0;
        }
        save();
        return removed.size();
    }

    private synchronized void save() {
        YamlFiles.saveOrLog(config, file);
    }
}
