package de.hems.utils.server;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Brings the servers in {@code autostart} back after a crash.
 * <p>
 * The lobby and survival are the network: with either of them gone, everybody who joins lands nowhere or
 * loses their world until somebody notices and starts it by hand - and a crash at night is noticed in the
 * morning. So a server from that list that is gone without anybody having stopped it is started again.
 * <p>
 * Not forever: a server that dies right after every start has something wrong with it that a fourth start
 * does not fix, and restarting it in a loop only hides that. After {@link #MAX_RESTARTS} starts within
 * {@link #WINDOW_MS} it is left down, and the console says so.
 */
public class AutostartWatchdog {

    /** How often the servers are looked at. */
    private static final long CHECK_INTERVAL_SECONDS = 30L;
    /** How long after the launcher started the first look is - the servers are still coming up before. */
    private static final long FIRST_CHECK_SECONDS = 120L;
    /** How many restarts a server gets within {@link #WINDOW_MS} before it is left alone. */
    private static final int MAX_RESTARTS = 3;
    private static final long WINDOW_MS = 30L * 60L * 1000L;

    private final ServerHandler servers;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "autostart-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    /** When each server was brought back, newest last. */
    private final Map<String, Deque<Long>> restarts = new HashMap<>();

    public AutostartWatchdog(ServerHandler servers) {
        this.servers = servers;
        scheduler.scheduleWithFixedDelay(this::check, FIRST_CHECK_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void check() {
        try {
            YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
            for (String raw : config.getStringList("autostart")) {
                ListenerAdapter.ServerName name;
                try {
                    name = ListenerAdapter.ServerName.valueOf(raw);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (servers.doesInstanceExist(name) || servers.wasStoppedOnPurpose(name)) continue;
                // a server whose process still runs is starting or saving, not crashed
                ServerInstance stale = servers.getInstance(name);
                if (stale != null && stale.isProcessRunning()) continue;
                bringBack(name, config);
            }
        } catch (Throwable e) {
            // anything that escapes ends a scheduled task for good, silently - so nothing may escape
            System.out.println("The autostart check failed: " + e);
        }
    }

    private void bringBack(ListenerAdapter.ServerName name, YamlConfiguration config) {
        long now = System.currentTimeMillis();
        Deque<Long> recent = restarts.computeIfAbsent(name.toString(), key -> new ArrayDeque<>());
        while (!recent.isEmpty() && now - recent.peekFirst() > WINDOW_MS) recent.pollFirst();
        if (recent.size() >= MAX_RESTARTS) {
            if (recent.size() == MAX_RESTARTS) {
                System.out.println(name + " crashed " + MAX_RESTARTS + " times in 30 minutes - it is left down"
                        + " until somebody starts it.");
                recent.addLast(now);
            }
            return;
        }
        recent.addLast(now);
        System.out.println(name + " is gone without having been stopped - starting it again.");
        ServerTemplate template = ServerTemplate.forServerName(name.toString());
        int memory = config.getInt("servers." + name + ".memory", template.getDefaultMemoryMB());
        try {
            servers.startNewInstance(name, template, memory, new FileType.PLUGIN[0]);
        } catch (Exception e) {
            System.out.println("Could not start " + name + " again: " + e.getMessage());
        }
    }
}
