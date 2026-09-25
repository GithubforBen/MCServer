package de.hems.paper.event;

import de.hems.paper.NetworkSync;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.RequestRunsEvent;
import de.hems.communication.events.event.RunUpdatedEvent;
import de.hems.communication.events.event.SaveRunEvent;
import de.hems.paper.PaperContext;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import de.hems.types.event.UhcSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The attempts at the run events, as seen from a game server.
 * <p>
 * Same shape as {@link EventService}: the launcher owns them, everyone keeps a copy, and every change is
 * announced. That is what lets a leaderboard on the lobby update while the race is still running on a
 * completely different server.
 */
public final class RunService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;

    private static final Map<UUID, RunData> runs = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private RunService() {
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
        ListenerAdapter.register(RunUpdatedEvent.class, event -> apply((RunUpdatedEvent) event));
        NetworkSync.keepFresh(plugin, RunService::refreshBlocking, () -> loaded, REFRESH_INTERVAL_TICKS);
    }

    private static void apply(RunUpdatedEvent event) {
        if (event.getRunId() == null) return;
        if (event.isDeleted()) {
            runs.remove(event.getRunId());
            return;
        }
        runs.put(event.getRunId(), event.getRun());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @param eventId the event to look at
     * @return its runs, fastest finished first
     */
    public static List<RunData> getRunsOf(UUID eventId) {
        List<RunData> found = new ArrayList<>();
        for (RunData run : runs.values()) {
            if (eventId.equals(run.getEventId())) found.add(run);
        }
        found.sort(leaderboardOrder());
        return found;
    }

    /**
     * @param eventId the event to look at
     * @return only the runs that count, fastest first
     */
    public static List<RunData> getLeaderboard(UUID eventId) {
        List<RunData> ranked = new ArrayList<>();
        for (RunData run : getRunsOf(eventId)) {
            if (run.isRanked()) ranked.add(run);
        }
        return ranked;
    }

    /**
     * The order a leaderboard is read in: finished runs by time, everything else behind them.
     *
     * @return the comparator
     */
    public static Comparator<RunData> leaderboardOrder() {
        return (left, right) -> {
            if (left.isRanked() != right.isRanked()) return left.isRanked() ? -1 : 1;
            if (left.isRanked()) {
                return Long.compare(left.getElapsedTicks(), right.getElapsedTicks());
            }
            return Long.compare(right.getStartedAt(), left.getStartedAt());
        };
    }

    /**
     * @param eventId the event to look at
     * @param player  the player to look for
     * @return the run that player is in right now, or {@code null}
     */
    public static RunData getActiveRunOf(UUID eventId, UUID player) {
        for (RunData run : runs.values()) {
            if (!eventId.equals(run.getEventId())) continue;
            // a paused run is still that player's run - it is waiting for them to come back
            if (!run.isOpen()) continue;
            if (run.getParticipants().contains(player)) return run;
        }
        return null;
    }

    /**
     * @param eventId the event to look at
     * @param player  the player to count for
     * @return how many attempts that player has already made
     */
    public static int countRunsOf(UUID eventId, UUID player) {
        int count = 0;
        for (RunData run : runs.values()) {
            if (eventId.equals(run.getEventId()) && run.getParticipants().contains(player)) count++;
        }
        return count;
    }

    /**
     * Whether a player may still start a run of this event.
     *
     * @param event  the event
     * @param player the player
     * @return whether they have attempts left
     */
    public static boolean hasRunsLeft(EventData event, UUID player) {
        return countRunsOf(event.getId(), player) < new UhcSettings(event).getMaxRuns();
    }

    /**
     * Stores a run. Fire and forget - the launcher announces it and the copy here is updated straight away
     * so the player sees the change without waiting for the round trip.
     *
     * @param run the run to store
     */
    public static void save(RunData run) {
        if (run == null || run.getId() == null) return;
        runs.put(run.getId(), run);
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new SaveRunEvent(run));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not save the run: " + e.getMessage());
            }
        });
    }

    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(RunService::refreshBlocking);
    }

    /**
     * Fetches the full list. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        RequestRunsEvent request = new RequestRunsEvent();
        List<RunData> list = NetworkSync.fetchList(request, TIMEOUT, RunData.class);
        if (list == null) return;
        Map<UUID, RunData> fresh = new ConcurrentHashMap<>();
        for (RunData run : list) {
            if (run.getId() == null) continue;
            fresh.put(run.getId(), run);
        }
        NetworkSync.replace(runs, fresh);
        loaded = true;
    }
}
