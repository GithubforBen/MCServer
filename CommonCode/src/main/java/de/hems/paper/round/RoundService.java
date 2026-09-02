package de.hems.paper.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.round.DeleteRoundEvent;
import de.hems.communication.events.round.RequestRoundsEvent;
import de.hems.communication.events.round.RespondRoundSaveEvent;
import de.hems.communication.events.round.RoundPolicyUpdatedEvent;
import de.hems.communication.events.round.RoundUpdatedEvent;
import de.hems.communication.events.round.SaveRoundEvent;
import de.hems.communication.events.round.SaveRoundPolicyEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundPolicy;
import de.hems.types.round.RoundSnapshot;
import de.hems.types.round.RoundState;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The self started rounds of the network, as seen from a game server.
 * <p>
 * Same shape as {@link de.hems.paper.team.TeamService}: the launcher owns the list, this keeps a copy that
 * is kept current by announcements, and every write goes to the launcher rather than into the copy. The
 * lobby reads it to draw the round list, and a bedwars server reads exactly one entry out of it - its own,
 * looked up by its own name - to find out what it is supposed to be playing.
 */
public final class RoundService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long REFRESH_INTERVAL_TICKS = 20L * 60L;
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<UUID, RoundData> rounds = new ConcurrentHashMap<>();
    private static volatile RoundPolicy policy = new RoundPolicy();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private RoundService() {
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
        ListenerAdapter.register(RoundUpdatedEvent.class, event -> apply((RoundUpdatedEvent) event));
        ListenerAdapter.register(RoundPolicyUpdatedEvent.class, event -> {
            RoundPolicy updated = ((RoundPolicyUpdatedEvent) event).getPolicy();
            if (updated != null) policy = updated;
        });
        refreshAsync();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, RoundService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    private static void apply(RoundUpdatedEvent event) {
        if (event.getRoundId() == null) return;
        if (event.isDeleted()) {
            rounds.remove(event.getRoundId());
            return;
        }
        rounds.put(event.getRoundId(), event.getRound());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @return the rules self started rounds run under
     */
    public static RoundPolicy getPolicy() {
        return policy;
    }

    /**
     * @return every round the launcher knows
     */
    public static List<RoundData> getRounds() {
        return new ArrayList<>(rounds.values());
    }

    /**
     * @return the rounds players can still be sent to, oldest first
     */
    public static List<RoundData> getOpenRounds() {
        List<RoundData> open = new ArrayList<>();
        for (RoundData round : rounds.values()) {
            if (round.getState().isOpen()) open.add(round);
        }
        open.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        return open;
    }

    public static RoundData get(UUID id) {
        return id == null ? null : rounds.get(id);
    }

    /**
     * @param serverName a server
     * @return the round running on it, or {@code null}
     */
    public static RoundData byServer(String serverName) {
        if (serverName == null) return null;
        String wanted = serverName.toUpperCase(Locale.ROOT);
        for (RoundData round : rounds.values()) {
            String on = round.getServerName();
            if (on != null && on.toUpperCase(Locale.ROOT).equals(wanted)) return round;
        }
        return null;
    }

    /**
     * @param player a player
     * @return how many rounds of theirs are still alive
     */
    public static int openOf(UUID player) {
        int open = 0;
        for (RoundData round : rounds.values()) {
            if (round.isOwner(player) && round.getState().isAlive()) open++;
        }
        return open;
    }

    /**
     * @return how many self started rounds are alive across the network
     */
    public static int aliveRounds() {
        int alive = 0;
        for (RoundData round : rounds.values()) {
            if (round.getState().isAlive()) alive++;
        }
        return alive;
    }

    /**
     * @param player a player
     * @return when they last started a round, {@code 0} when they never did
     */
    public static long lastStartOf(UUID player) {
        long last = 0L;
        for (RoundData round : rounds.values()) {
            if (round.isOwner(player) && round.getCreatedAt() > last) last = round.getCreatedAt();
        }
        return last;
    }

    /* ------------------------------------------------------------------ writing */

    /**
     * Stores a round and waits for the answer. Blocks, so it must not run on the main thread.
     *
     * @param round the round
     * @return whether the launcher took it
     */
    public static boolean saveBlocking(RoundData round) {
        if (round == null) return false;
        try {
            if (!ListenerAdapter.isInitialized()) return false;
            SaveRoundEvent request = new SaveRoundEvent(round);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondRoundSaveEvent saved) || !saved.isSuccessful()) return false;
            if (response.getData() instanceof RoundData stored) rounds.put(stored.getId(), stored);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not store the round: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stores a round in the background.
     *
     * @param round    the round
     * @param callback what to do with the answer, on the main thread, may be {@code null}
     */
    public static void saveAsync(RoundData round, Consumer<Boolean> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            boolean stored = saveBlocking(round);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(stored));
        });
    }

    /**
     * Marks a round as being over. The server it ran on is stopped by whoever owns that decision.
     *
     * @param round the round
     */
    public static void endAsync(RoundData round) {
        if (round == null) return;
        RoundData ended = round.copy();
        ended.setState(RoundState.ENDED);
        saveAsync(ended, null);
    }

    /**
     * Removes a round from the list.
     *
     * @param id the round
     */
    public static void deleteAsync(UUID id) {
        if (id == null || !PaperContext.hasPlugin()) return;
        rounds.remove(id);
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new DeleteRoundEvent(id));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not delete round " + id + ": " + e.getMessage());
            }
        });
    }

    /**
     * Changes the rules. Only ever called for an operator.
     *
     * @param updated the new rules
     */
    public static void savePolicyAsync(RoundPolicy updated) {
        if (updated == null || !PaperContext.hasPlugin()) return;
        policy = updated;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new SaveRoundPolicyEvent(updated));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not store the round rules: " + e.getMessage());
            }
        });
    }

    /* ------------------------------------------------------------------ reading */

    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(RoundService::refreshBlocking);
    }

    /**
     * Fetches the whole list. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestRoundsEvent request = new RequestRoundsEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof RoundSnapshot snapshot)) return;
            Map<UUID, RoundData> fresh = new ConcurrentHashMap<>();
            for (RoundData round : snapshot.getRounds()) {
                if (round.getId() != null) fresh.put(round.getId(), round);
            }
            rounds.keySet().retainAll(fresh.keySet());
            rounds.putAll(fresh);
            policy = snapshot.getPolicy();
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the rounds: " + e.getMessage());
        }
    }
}
