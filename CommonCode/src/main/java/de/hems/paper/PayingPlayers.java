package de.hems.paper;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.configs.RequestDataFromConfigEvent;
import de.hems.communication.events.types.RespondDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Knows which players pay for the server.
 * <p>
 * The list lives in the config of the launcher, so it has to be fetched over the network. This class never
 * blocks and never guesses: {@link #isPaying(UUID)} answers from a snapshot that a background task keeps up
 * to date, and a failed refresh leaves the previous snapshot untouched. Treating a slow answer as "does not
 * pay" is what used to downgrade paying players at random.
 * <p>
 * It lives in the shared code rather than in one plugin because more than one feature depends on it - the
 * chunk limiter and the size of a team's backpack both ask the same question.
 */
public final class PayingPlayers {

    /** How long a snapshot is used before it is refreshed. */
    private static final long REFRESH_INTERVAL_MS = 60_000L;
    /** How long to wait before retrying after a refresh did not produce an answer. */
    private static final long RETRY_INTERVAL_MS = 5_000L;
    /** The shortest time between two refreshes, so a burst of joins does not become a burst of requests. */
    private static final long MIN_REFRESH_INTERVAL_MS = 2_000L;
    /** How long the background refresh waits for the launcher to answer. */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    /** The config key the launcher stores the paying players under. */
    private static final String CONFIG_KEY = "paying-players";

    private static volatile Set<UUID> snapshot = Collections.emptySet();
    private static volatile boolean loaded = false;
    private static volatile long nextRefreshAt = 0L;
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);

    private PayingPlayers() {
    }

    /**
     * Answers from the cached snapshot without ever touching the network, so this is safe to call from the
     * main thread as often as needed.
     *
     * @param player the player to look up
     * @return whether that player pays for the server
     */
    public static boolean isPaying(Player player) {
        return player != null && isPaying(player.getUniqueId());
    }

    /**
     * @param uuid the player to look up
     * @return whether that player pays for the server
     */
    public static boolean isPaying(UUID uuid) {
        return uuid != null && snapshot.contains(uuid);
    }

    /**
     * Whether the list was received at least once. Callers use this to avoid acting on an answer that has
     * not arrived yet.
     *
     * @return whether a snapshot is available
     */
    public static boolean isKnown() {
        return loaded;
    }

    /**
     * @return the players that currently pay, as far as this server knows
     */
    public static Set<UUID> getPayingPlayers() {
        return snapshot;
    }

    /**
     * Counts how many of the given players pay.
     *
     * @param players the players to count
     * @return how many of them are on the list
     */
    public static int countPaying(Iterable<UUID> players) {
        int paying = 0;
        for (UUID uuid : players) {
            if (isPaying(uuid)) paying++;
        }
        return paying;
    }

    /**
     * Whether more than half of the given players pay. Ties count as "not a majority", so a two player team
     * needs both of them.
     *
     * @param players the players to weigh up
     * @return whether the paying ones are in the majority
     */
    public static boolean isMajorityPaying(java.util.Collection<UUID> players) {
        if (players == null || players.isEmpty()) return false;
        return countPaying(players) * 2 > players.size();
    }

    /**
     * Brings the next refresh forward, so a change made in the meantime is picked up quickly. It does not
     * drop the expiry outright: twenty players joining at once would otherwise become twenty requests.
     */
    public static void invalidate() {
        long soon = System.currentTimeMillis() + MIN_REFRESH_INTERVAL_MS;
        if (soon < nextRefreshAt) nextRefreshAt = soon;
    }

    /**
     * Fetches the list again if the snapshot has expired. Returns immediately - the fetch runs async.
     */
    public static void refreshIfDue() {
        if (System.currentTimeMillis() < nextRefreshAt) return;
        refreshNow();
    }

    /**
     * Starts a refresh in the background, unless one is already running.
     */
    public static void refreshNow() {
        if (!refreshing.compareAndSet(false, true)) return;
        try {
            if (!PaperContext.hasPlugin() || !PaperContext.getPlugin().isEnabled()) {
                refreshing.set(false);
                return;
            }
            PaperContext.async(PayingPlayers::fetch);
        } catch (RuntimeException e) {
            // the plugin was disabled between the check and the call - the flag must not stay stuck
            refreshing.set(false);
        }
    }

    /**
     * Asks the launcher for the list and stores the answer. Runs off the main thread; a missing or
     * unusable answer keeps the previous snapshot alive instead of emptying it.
     */
    private static void fetch() {
        try {
            if (!ListenerAdapter.isInitialized()) {
                nextRefreshAt = System.currentTimeMillis() + RETRY_INTERVAL_MS;
                return;
            }
            RequestDataFromConfigEvent request = new RequestDataFromConfigEvent(CONFIG_KEY);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), RESPONSE_TIMEOUT);
            if (response == null) {
                nextRefreshAt = System.currentTimeMillis() + RETRY_INTERVAL_MS;
                return;
            }
            Set<UUID> parsed = parse(response.getData());
            if (parsed == null) {
                nextRefreshAt = System.currentTimeMillis() + RETRY_INTERVAL_MS;
                return;
            }
            snapshot = parsed;
            loaded = true;
            nextRefreshAt = System.currentTimeMillis() + REFRESH_INTERVAL_MS;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            nextRefreshAt = System.currentTimeMillis() + RETRY_INTERVAL_MS;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not refresh the paying players: " + e.getMessage());
            nextRefreshAt = System.currentTimeMillis() + RETRY_INTERVAL_MS;
        } finally {
            refreshing.set(false);
        }
    }

    /**
     * Turns whatever the launcher sent into a set of uuids. Entries that are not uuids are skipped rather
     * than making the whole list unusable.
     *
     * @param data the payload of the response
     * @return the paying players, or {@code null} if the payload was not a list at all
     */
    private static Set<UUID> parse(Object data) {
        if (data == null) {
            // the key is simply not set yet - that is a valid answer meaning "nobody pays"
            return Collections.emptySet();
        }
        if (!(data instanceof List<?> list)) return null;
        Set<UUID> uuids = new HashSet<>();
        for (Object entry : list) {
            if (entry == null) continue;
            if (entry instanceof UUID uuid) {
                uuids.add(uuid);
                continue;
            }
            try {
                uuids.add(UUID.fromString(entry.toString().trim()));
            } catch (IllegalArgumentException ignored) {
                // not a uuid - ignore this entry, the rest of the list stays usable
            }
        }
        return Collections.unmodifiableSet(uuids);
    }
}
