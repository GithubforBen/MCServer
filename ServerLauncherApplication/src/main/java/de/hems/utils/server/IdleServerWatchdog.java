package de.hems.utils.server;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestProxyPlayersEvent;
import de.hems.communication.events.server.RespondProxyPlayersEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.ServerPhase;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Switches servers off that nobody is on any more.
 * <p>
 * A server created for one round of something has no reason to keep a gigabyte of memory once the round is
 * over, and the plugin on it cannot be relied on to notice: the case where a server is forgotten is
 * precisely the case where its plugin crashed, failed to enable or never got a network connection. So the
 * decision is made here, from the proxy's view of who is connected where, which is true regardless of what
 * runs on the server itself.
 * <p>
 * Only servers that were created on the fly are touched. The hub and the survival world are meant to be
 * empty at four in the morning and still be there in the morning, so anything named in {@code autostart}
 * is left alone.
 */
public class IdleServerWatchdog {

    /** How often the proxy is asked who is where. */
    private static final long CHECK_INTERVAL_SECONDS = 60L;
    /** How long to wait for the proxy to answer. */
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(5);
    /** How long a server stays up with nobody on it, unless the config says otherwise. */
    private static final int DEFAULT_IDLE_MINUTES = 10;

    private final ServerHandler servers;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor((runnable) -> {
                Thread thread = new Thread(runnable, "idle-server-watchdog");
                thread.setDaemon(true);
                return thread;
            });
    /** Since when a server has been reported empty, by server name. */
    private final Map<String, Long> emptySince = new HashMap<>();

    public IdleServerWatchdog(ServerHandler servers) {
        this.servers = servers;
        scheduler.scheduleWithFixedDelay(this::check,
                CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * @return how long a server may stay empty before it is stopped, {@code 0} to switch this off
     */
    private int idleMinutes() {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("idle-shutdown-minutes")) {
            config.set("idle-shutdown-minutes", DEFAULT_IDLE_MINUTES);
            config.setComments("idle-shutdown-minutes", List.of(
                    "How many minutes a server that was created on the fly may stay empty before it is",
                    "stopped again. Servers listed in autostart are never stopped. 0 switches this off."));
            Main.getInstance().getConfiguration().save();
        }
        return config.getInt("idle-shutdown-minutes", DEFAULT_IDLE_MINUTES);
    }

    /**
     * @return the servers that are meant to stay up no matter how empty they are
     */
    private Set<String> protectedServers() {
        Set<String> names = new HashSet<>();
        names.add(ListenerAdapter.ServerName.LOBBY.toString());
        for (String name : Main.getInstance().getConfiguration().getConfig().getStringList("autostart")) {
            try {
                names.add(ListenerAdapter.ServerName.normalize(name));
            } catch (IllegalArgumentException e) {
                // an unusable name in the config protects nothing, which is what it already did
            }
        }
        return names;
    }

    /**
     * One round: ask the proxy, then stop whatever has been empty for long enough.
     */
    private void check() {
        try {
            int idleMinutes = idleMinutes();
            if (idleMinutes <= 0) return;
            Map<String, List<String>> connected = askProxy();
            if (connected == null) return;

            long idleMs = idleMinutes * 60L * 1000L;
            long now = System.currentTimeMillis();
            Set<String> known = new HashSet<>();
            Set<String> protectedNames = protectedServers();

            for (ServerInstance instance : servers.getInstances()) {
                String name = instance.getName().toString();
                known.add(name);
                if (instance.getName().isReserved() || protectedNames.contains(name)) continue;
                // a server that is still coming up has not had the chance to be joined yet, and
                // isStarting() keeps that true for the first minutes, which is the grace a server needs
                // between being ready and the players that ordered it actually arriving
                if (instance.getPhase() != ServerPhase.READY || instance.isStarting()) {
                    emptySince.remove(name);
                    continue;
                }
                List<String> players = connected.get(name.toUpperCase(Locale.ROOT));
                if (players == null) {
                    // the proxy does not know this server, so it cannot say it is empty either
                    emptySince.remove(name);
                    continue;
                }
                if (!players.isEmpty()) {
                    emptySince.remove(name);
                    continue;
                }
                Long since = emptySince.putIfAbsent(name, now);
                if (since == null) continue;
                if (now - since < idleMs) continue;
                stop(name, idleMinutes);
            }
            emptySince.keySet().retainAll(known);
        } catch (Exception e) {
            System.out.println("The idle check failed: " + e.getMessage());
        }
    }

    private void stop(String name, int idleMinutes) {
        System.out.println("Stopping " + name + " - nobody has been on it for " + idleMinutes + " minutes.");
        emptySince.remove(name);
        try {
            servers.stop(ListenerAdapter.ServerName.valueOf(name));
        } catch (RuntimeException e) {
            System.out.println("Could not stop the idle server " + name + ": " + e.getMessage());
        }
    }

    /**
     * @return who is on which server, or {@code null} when the proxy did not answer
     */
    private Map<String, List<String>> askProxy() throws InterruptedException {
        if (!ListenerAdapter.isInitialized()) return null;
        RequestProxyPlayersEvent request = new RequestProxyPlayersEvent();
        try {
            ListenerAdapter.sendListeners(request);
        } catch (Exception e) {
            return null;
        }
        RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), ANSWER_TIMEOUT);
        if (!(response instanceof RespondProxyPlayersEvent players)) return null;
        return players.getPlayersPerServer();
    }
}
