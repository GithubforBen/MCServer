package de.hems.paper.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.DeleteEventEvent;
import de.hems.communication.events.event.EventUpdatedEvent;
import de.hems.communication.events.event.RequestEventsEvent;
import de.hems.communication.events.event.RespondEventSaveEvent;
import de.hems.communication.events.event.SaveEventEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The events of the network, as seen from a game server.
 * <p>
 * Built the same way as the teams: the launcher owns them, every server keeps a copy so the tab list and the
 * calendar never touch the network, and the launcher announces every change so an event created in the lobby
 * shows up on survival a moment later.
 */
public final class EventService {

    /** How long to wait for the launcher to answer. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    /** How often the whole list is refreshed as a safety net, in ticks. */
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;
    /** How often to retry while the list has never arrived, in ticks. */
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<UUID, EventData> events = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private EventService() {
    }

    /**
     * Starts keeping the local copy up to date.
     *
     * @param plugin the plugin the background work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(EventUpdatedEvent.class, event -> apply((EventUpdatedEvent) event));
        refreshAsync();
        // the network may not be connected yet when this plugin loads, so try again quickly until it is
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, EventService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    /**
     * Takes over what the launcher announced.
     *
     * @param event the announcement
     */
    private static void apply(EventUpdatedEvent event) {
        if (event.getEventUuid() == null) return;
        if (event.isDeleted()) {
            events.remove(event.getEventUuid());
            return;
        }
        events.put(event.getEventUuid(), event.getEvent());
    }

    /**
     * @return whether the list has arrived at least once
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @return every event of the network, soonest first
     */
    public static List<EventData> getEvents() {
        List<EventData> all = new ArrayList<>(events.values());
        all.sort(Comparator.comparingLong(EventData::getStartsAt));
        return all;
    }

    /**
     * @param id the event to look up
     * @return that event, or {@code null}
     */
    public static EventData getEvent(UUID id) {
        return id == null ? null : events.get(id);
    }

    /**
     * @return the events that are running right now, soonest first
     */
    public static List<EventData> getRunning() {
        List<EventData> running = new ArrayList<>();
        for (EventData event : getEvents()) {
            if (event.getState() == EventState.RUNNING) running.add(event);
        }
        return running;
    }

    /**
     * @return the events that are still to come, soonest first
     */
    public static List<EventData> getUpcoming() {
        List<EventData> upcoming = new ArrayList<>();
        for (EventData event : getEvents()) {
            if (event.getState() == EventState.PLANNED) upcoming.add(event);
        }
        return upcoming;
    }

    /**
     * @return the event that starts next, or {@code null} if nothing is planned
     */
    public static EventData getNext() {
        List<EventData> upcoming = getUpcoming();
        return upcoming.isEmpty() ? null : upcoming.getFirst();
    }

    /**
     * Whether a one-off event has already happened. This is what opens the End for good.
     *
     * @param type the kind of event to ask about
     * @return whether an event of that kind is running or has finished without being cancelled
     */
    public static boolean hasHappened(EventType type) {
        for (EventData event : events.values()) {
            if (event.getType() != type || event.isCancelled()) continue;
            EventState state = event.getState();
            if (state == EventState.RUNNING || state == EventState.FINISHED) return true;
        }
        return false;
    }

    /**
     * Fetches the full list in the background.
     */
    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(EventService::refreshBlocking);
    }

    /**
     * Fetches the full list. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestEventsEvent request = new RequestEventsEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof List<?> list)) return;
            Map<UUID, EventData> fresh = new ConcurrentHashMap<>();
            for (Object entry : list) {
                if (!(entry instanceof EventData event) || event.getId() == null) continue;
                fresh.put(event.getId(), event);
            }
            events.clear();
            events.putAll(fresh);
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the events: " + e.getMessage());
        }
    }

    /**
     * Stores an event on the launcher.
     *
     * @param event           the event to store, carrying the revision it was read at
     * @param createIfMissing whether it may be created
     * @param callback        what to do with the result, on the main thread
     */
    public static void saveAsync(EventData event, boolean createIfMissing, Consumer<Result> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            Result result = saveBlocking(event, createIfMissing);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Stores an event and waits for the answer. Blocks, so it must not run on the main thread.
     *
     * @param event           the event to store
     * @param createIfMissing whether it may be created
     * @return what the launcher made of it
     */
    public static Result saveBlocking(EventData event, boolean createIfMissing) {
        try {
            SaveEventEvent request = new SaveEventEvent(event, createIfMissing);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondEventSaveEvent saved)) {
                return new Result(false, "Der Hauptserver hat nicht geantwortet.", null);
            }
            if (saved.isSuccessful() && saved.getData() instanceof EventData stored) {
                events.put(stored.getId(), stored);
                return new Result(true, saved.getMessage(), stored);
            }
            return new Result(false, saved.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, "Unterbrochen.", null);
        } catch (Exception e) {
            return new Result(false, "Konnte nicht gespeichert werden: " + e.getMessage(), null);
        }
    }

    /**
     * Removes an event on the launcher.
     *
     * @param id the event to remove
     */
    public static void deleteAsync(UUID id) {
        if (!PaperContext.hasPlugin() || id == null) return;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new DeleteEventEvent(id));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not delete the event: " + e.getMessage());
            }
        });
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param message    what to tell the player
     * @param event      the event as it is stored now, or {@code null} if it was refused
     */
    public record Result(boolean successful, String message, EventData event) {
    }
}
