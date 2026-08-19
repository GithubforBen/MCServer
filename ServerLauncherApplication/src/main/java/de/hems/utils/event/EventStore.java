package de.hems.utils.event;

import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the events of the network live.
 * <p>
 * Built like {@link de.hems.utils.team.TeamStore}: the launcher is the only node that writes them, so the
 * lobby, survival and the website can never disagree about when something starts.
 */
public class EventStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, EventData> events = new ConcurrentHashMap<>();

    public EventStore() {
        this(new File("./events.yml"));
    }

    public EventStore(File file) {
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
        ConfigurationSection section = config.getConfigurationSection("events");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            EventData event = read(key, entry);
            if (event != null) events.put(event.getId(), event);
        }
        System.out.println("Loaded " + events.size() + " events from " + file.getName());
    }

    /**
     * @param key   the id the event is stored under
     * @param entry the section holding it
     * @return the event, or {@code null} if the entry is unusable
     */
    private static EventData read(String key, ConfigurationSection entry) {
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
        EventType type = EventType.byName(entry.getString("type"));
        // an event of a type this version no longer knows cannot be shown or run, so it is dropped
        if (type == null) return null;
        EventData event = new EventData();
        event.setId(id);
        event.setName(entry.getString("name", "Event"));
        event.setType(type);
        event.setDescription(entry.getString("description"));
        event.setStartsAt(entry.getLong("starts-at"));
        event.setEndsAt(entry.getLong("ends-at"));
        event.setCancelled(entry.getBoolean("cancelled", false));
        event.setApplied(entry.getBoolean("applied", false));
        event.setRevision(entry.getLong("revision", 0L));
        ConfigurationSection settings = entry.getConfigurationSection("settings");
        if (settings != null) {
            for (String settingKey : settings.getKeys(false)) {
                event.setSetting(settingKey, String.valueOf(settings.get(settingKey)));
            }
        }
        return event;
    }

    private void write(EventData event) {
        String path = "events." + event.getId();
        config.set(path + ".name", event.getName());
        config.set(path + ".type", event.getType().name());
        config.set(path + ".description", event.getDescription());
        config.set(path + ".starts-at", event.getStartsAt());
        config.set(path + ".ends-at", event.getEndsAt());
        config.set(path + ".cancelled", event.isCancelled());
        config.set(path + ".applied", event.isApplied());
        config.set(path + ".revision", event.getRevision());
        Map<String, Object> settings = new LinkedHashMap<>(event.getSettings());
        config.set(path + ".settings", settings.isEmpty() ? null : settings);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * @return every event, soonest first, as a fresh list the caller may keep
     */
    public List<EventData> getEvents() {
        List<EventData> all = new ArrayList<>(events.values());
        all.sort(Comparator.comparingLong(EventData::getStartsAt));
        return all;
    }

    /**
     * @param id the event to look up
     * @return that event, or {@code null}
     */
    public EventData getEvent(UUID id) {
        return id == null ? null : events.get(id);
    }

    /**
     * Stores an event.
     * <p>
     * The write is refused when the caller worked from an older revision than the one that is stored, which
     * is what stops the website and the lobby overwriting each other.
     *
     * @param event           the event to store
     * @param createIfMissing whether it may be created
     * @return what happened, for the answer sent back to the caller
     */
    public synchronized Result put(EventData event, boolean createIfMissing) {
        if (event == null || event.getId() == null) {
            return Result.failed("Das Event hat keine Id.");
        }
        if (event.getType() == null) {
            return Result.failed("Das Event hat keinen Typ.");
        }
        if (event.getName() == null || event.getName().isBlank()) {
            return Result.failed("Das Event hat keinen Namen.");
        }
        if (event.getEndsAt() <= event.getStartsAt()) {
            return Result.failed("Das Event endet vor seinem Anfang.");
        }
        EventData existing = events.get(event.getId());
        if (existing == null && !createIfMissing) {
            return Result.failed("Dieses Event gibt es nicht.");
        }
        if (existing != null && event.getRevision() != existing.getRevision()) {
            return Result.failed("Das Event wurde inzwischen woanders geändert. Bitte nochmal öffnen.");
        }
        // a type that may only exist once must not get a second entry, or the End could be opened twice
        if (existing == null && event.getType().isOnlyOnce() && hasType(event.getType())) {
            return Result.failed("Ein Event vom Typ '" + event.getType().getTitle() + "' gibt es schon.");
        }
        event.setRevision(event.getRevision() + 1);
        events.put(event.getId(), event);
        write(event);
        save();
        return Result.ok(event);
    }

    /**
     * @param type the kind to look for
     * @return whether an event of that kind exists and was not cancelled
     */
    public boolean hasType(EventType type) {
        for (EventData event : events.values()) {
            if (event.getType() == type && !event.isCancelled()) return true;
        }
        return false;
    }

    /**
     * @param id the event to remove
     * @return whether it existed
     */
    public synchronized boolean delete(UUID id) {
        if (id == null) return false;
        EventData removed = events.remove(id);
        if (removed == null) return false;
        config.set("events." + id, null);
        save();
        return true;
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param message    what to tell the caller
     * @param event      the event as it is stored now
     */
    public record Result(boolean successful, String message, EventData event) {

        public static Result ok(EventData event) {
            return new Result(true, "Gespeichert.", event);
        }

        public static Result failed(String message) {
            return new Result(false, message, null);
        }
    }
}
