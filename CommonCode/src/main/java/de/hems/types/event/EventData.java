package de.hems.types.event;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One event of the network, as it travels between the launcher, the game servers and the website.
 * <p>
 * The launcher owns these. Everything else keeps a copy and sends changes over, the same way teams work,
 * so an event created in the lobby is known on survival and on the website a moment later.
 */
public class EventData implements Serializable {

    private static final long serialVersionUID = 4300L;

    private UUID id;
    private String name;
    private EventType type;
    private String description;
    /** When it starts and ends, as epoch milliseconds. */
    private long startsAt;
    private long endsAt;
    /** Set by hand when an admin calls the event off, otherwise worked out from the time frame. */
    private boolean cancelled;
    /** Whether the effect of a one-off event has already been applied, so it only ever happens once. */
    private boolean applied;
    /** Free settings of the event, like hardcore or the team size. */
    private Map<String, String> settings = new LinkedHashMap<>();
    /** Bumped on every write, so two servers editing the same event cannot lose one set of changes. */
    private long revision;

    public EventData() {
    }

    /**
     * @param name      what it is called
     * @param type      the kind of event
     * @param startsAt  when it begins, epoch millis
     * @param endsAt    when it is over, epoch millis
     */
    public EventData(String name, EventType type, long startsAt, long endsAt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.type = type;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * @return where this event stands at this moment
     */
    public EventState getState() {
        if (cancelled) return EventState.CANCELLED;
        long now = System.currentTimeMillis();
        if (now < startsAt) return EventState.PLANNED;
        if (now >= endsAt) return EventState.FINISHED;
        return EventState.RUNNING;
    }

    public boolean isRunning() {
        return getState() == EventState.RUNNING;
    }

    /**
     * @return how long until it starts, or {@link Duration#ZERO} once it has
     */
    public Duration getTimeUntilStart() {
        long remaining = startsAt - System.currentTimeMillis();
        return remaining <= 0 ? Duration.ZERO : Duration.ofMillis(remaining);
    }

    /**
     * @return how long it still runs, or {@link Duration#ZERO} if it is not running
     */
    public Duration getTimeUntilEnd() {
        long remaining = endsAt - System.currentTimeMillis();
        return remaining <= 0 ? Duration.ZERO : Duration.ofMillis(remaining);
    }

    /**
     * @param key      the setting to read
     * @param fallback what to answer if it was never set
     * @return the setting
     */
    public String getSetting(String key, String fallback) {
        String value = settings.get(key);
        return value == null ? fallback : value;
    }

    public boolean getFlag(String key, boolean fallback) {
        String value = settings.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public int getNumber(String key, int fallback) {
        try {
            String value = settings.get(key);
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setSetting(String key, String value) {
        settings.put(key, value);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startsAt) {
        this.startsAt = startsAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public Map<String, String> getSettings() {
        if (settings == null) settings = new LinkedHashMap<>();
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings == null ? new LinkedHashMap<>() : settings;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * @return a copy that can be edited without touching the cached original
     */
    public EventData copy() {
        EventData copy = new EventData();
        copy.id = id;
        copy.name = name;
        copy.type = type;
        copy.description = description;
        copy.startsAt = startsAt;
        copy.endsAt = endsAt;
        copy.cancelled = cancelled;
        copy.applied = applied;
        copy.settings = new LinkedHashMap<>(getSettings());
        copy.revision = revision;
        return copy;
    }

    /**
     * Turns a span into something readable, for the tab list and the calendar.
     *
     * @param duration the span
     * @return it written out, like "2 Tage 3 Std" or "5 Min"
     */
    public static String format(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + " Tage " + hours + " Std";
        if (hours > 0) return hours + " Std " + minutes + " Min";
        if (minutes > 0) return minutes + " Min";
        return seconds + " Sek";
    }
}
