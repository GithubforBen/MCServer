package de.hems.paper;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.communication.events.types.RespondDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * What every service that keeps a copy of the launcher's data does to keep it current.
 * <p>
 * Events, runs, rounds, teams, money, cosmetics, account links, poker rows: each of them keeps a local copy
 * so nothing on the main thread has to ask the network, and each of them used to schedule the same three
 * things by hand - load now, try again every two seconds until the first load worked (the network may not
 * be connected yet when a plugin starts), and reload every few minutes as a safety net for an update that
 * got lost. That lives here now; a service only says how it loads and how it knows it has.
 */
public final class NetworkSync {

    /** How often to try while the first load has not worked yet, in ticks. */
    public static final long STARTUP_RETRY_TICKS = 40L;
    /** How often a copy is reloaded as a safety net, in ticks, unless a service needs it sooner. */
    public static final long DEFAULT_REFRESH_TICKS = 20L * 300L;

    private NetworkSync() {
    }

    /**
     * Keeps a copy fresh: loads it now, retries until the first load worked, then reloads now and then.
     *
     * @param plugin       the plugin the background work belongs to
     * @param refresh      loads the whole copy, blocking - called off the main thread only
     * @param loaded       whether the first load has worked
     * @param refreshTicks how often to reload once loaded
     */
    public static void keepFresh(Plugin plugin, Runnable refresh, BooleanSupplier loaded, long refreshTicks) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, refresh);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded.getAsBoolean()) {
                task.cancel();
                return;
            }
            refresh.run();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, refresh, refreshTicks, refreshTicks);
    }

    /**
     * Asks for a list and keeps the entries of the expected type. Blocks.
     *
     * @param request what to ask
     * @param timeout how long to wait
     * @param type    what the entries are
     * @return the entries, or {@code null} when there was no usable answer - which is not the same as an
     *         empty list, and must not wipe a copy
     */
    public static <T> List<T> fetchList(EventFoundationData request, Duration timeout, Class<T> type) {
        RespondDataEvent response = ListenerAdapter.ask(request, timeout);
        if (response == null || !(response.getData() instanceof List<?> list)) return null;
        List<T> entries = new ArrayList<>();
        for (Object entry : list) {
            if (type.isInstance(entry)) entries.add(type.cast(entry));
        }
        return entries;
    }

    /**
     * Swaps a copy for a fresh one without a moment in which it is empty.
     * <p>
     * Clearing first and filling after - which is what the services did - leaves a window in which another
     * thread sees nothing at all: a calendar that shows "no events" for a tick, a team that briefly does not
     * exist. Dropping what is gone and then overwriting the rest never does.
     *
     * @param target the copy that is read by others
     * @param fresh  what it should hold now
     */
    public static <K, V> void replace(Map<K, V> target, Map<K, V> fresh) {
        target.keySet().retainAll(fresh.keySet());
        target.putAll(fresh);
    }
}
