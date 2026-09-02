package de.hems.paper.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.money.BalanceUpdatedEvent;
import de.hems.communication.events.money.ChangeBalanceEvent;
import de.hems.communication.events.money.RequestBalancesEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.money.BalanceResult;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The money of the network, as seen from a game server.
 * <p>
 * Bits used to live in {@code configs/money-config.yml} next to the survival server, which meant only that
 * one server could see them and only that one server could spend them. They now belong to the launcher,
 * like the teams and the events do, so the lobby can sell a cosmetic and survival knows about it a moment
 * later.
 * <p>
 * There are two ways to spend, and the difference matters:
 * <ul>
 *   <li>{@link #changeBlocking} asks the launcher and waits for the answer. Nothing is handed over before
 *       the money is actually gone, so this is what a shop uses. It blocks, so it belongs in
 *       {@link PaperContext#async}.</li>
 *   <li>{@link #change} takes the money out of the local copy right away and posts the change afterwards.
 *       It answers immediately and can therefore be called from a click handler, at the price of being a
 *       guess: two servers that empty the same account in the same second both believe they succeeded, and
 *       the launcher refuses the second one and corrects the copy. That is acceptable where it is used -
 *       one player, one server, one shop - and it is not acceptable for anything expensive.</li>
 * </ul>
 */
public final class MoneyService {

    /** How long to wait for the launcher to answer. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    /** How often the whole list is refreshed as a safety net, in ticks. */
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;
    /** How often to retry while the list has never arrived, in ticks. */
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<String, Integer> balances = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private MoneyService() {
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
        ListenerAdapter.register(BalanceUpdatedEvent.class, event -> apply((BalanceUpdatedEvent) event));
        refreshAsync();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, MoneyService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    private static void apply(BalanceUpdatedEvent event) {
        if (event.getHolder() == null) return;
        balances.put(event.getHolder(), event.getBalance());
    }

    /**
     * @return whether the balances have arrived at least once
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @param player the player
     * @return the account name their money is kept under
     */
    public static String holderOf(UUID player) {
        return player == null ? null : player.toString();
    }

    /**
     * @param team the team
     * @return the account name its money is kept under
     */
    public static String holderOf(Team team) {
        return team == null ? null : team.getName();
    }

    /**
     * @param holder an account
     * @return what is on it, {@code 0} for an account that has never held anything
     */
    public static int get(String holder) {
        if (holder == null) return 0;
        Integer amount = balances.get(holder);
        return amount == null ? 0 : amount;
    }

    public static int get(UUID player) {
        return get(holderOf(player));
    }

    public static int get(Team team) {
        return get(holderOf(team));
    }

    /**
     * Moves money and answers right away, out of the local copy.
     * <p>
     * See the class comment for what that costs. Use {@link #changeBlocking} wherever the answer decides
     * whether something valuable is handed over.
     *
     * @param holder       the account
     * @param delta        how much to add, negative to take away
     * @param requireCover whether taking away must fail rather than go below zero
     * @param reason       what it was for, for the launcher's log
     * @return whether the copy could cover it
     */
    public static boolean change(String holder, int delta, boolean requireCover, String reason) {
        if (holder == null) return false;
        int current = get(holder);
        if (requireCover && delta < 0 && current + delta < 0) return false;
        balances.put(holder, current + delta);
        send(holder, delta, requireCover, reason);
        return true;
    }

    /**
     * Moves money and waits for the launcher to confirm it. Blocks, so it must not run on the main thread.
     *
     * @param holder       the account
     * @param delta        how much to add, negative to take away
     * @param requireCover whether taking away must fail rather than go below zero
     * @param reason       what it was for
     * @return what the launcher made of it
     */
    public static BalanceResult changeBlocking(String holder, int delta, boolean requireCover, String reason) {
        if (holder == null) return BalanceResult.failed(null, 0, "Kein Konto angegeben.");
        try {
            if (!ListenerAdapter.isInitialized()) {
                return BalanceResult.failed(holder, get(holder), "Keine Verbindung zum Netzwerk.");
            }
            ChangeBalanceEvent request = new ChangeBalanceEvent(holder, delta, requireCover, reason);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof BalanceResult result)) {
                return BalanceResult.failed(holder, get(holder), "Der Host antwortet nicht.");
            }
            balances.put(holder, result.getBalance());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BalanceResult.failed(holder, get(holder), "Unterbrochen.");
        } catch (Exception e) {
            return BalanceResult.failed(holder, get(holder), e.getMessage());
        }
    }

    /**
     * Moves money in the background and reports back on the main thread.
     *
     * @param holder       the account
     * @param delta        how much to add, negative to take away
     * @param requireCover whether taking away must fail rather than go below zero
     * @param reason       what it was for
     * @param callback     what to do with the answer, on the main thread
     */
    public static void changeAsync(String holder, int delta, boolean requireCover, String reason,
                                   Consumer<BalanceResult> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            BalanceResult result = changeBlocking(holder, delta, requireCover, reason);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Posts a change without waiting for the answer.
     */
    private static void send(String holder, int delta, boolean requireCover, String reason) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new ChangeBalanceEvent(holder, delta, requireCover, reason));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not post the balance change of " + holder + ": " + e.getMessage());
                // the copy said yes and the launcher never heard about it, so the copy is now wrong. The
                // next refresh puts it right rather than leaving somebody with money that does not exist
                refreshAsync();
            }
        });
    }

    /**
     * Fetches every balance in the background.
     */
    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(MoneyService::refreshBlocking);
    }

    /**
     * Fetches every balance. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestBalancesEvent request = new RequestBalancesEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof Map<?, ?> map)) return;
            Map<String, Integer> fresh = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String holder && entry.getValue() instanceof Integer amount) {
                    fresh.put(holder, amount);
                }
            }
            balances.keySet().retainAll(fresh.keySet());
            balances.putAll(fresh);
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the balances: " + e.getMessage());
        }
    }
}
