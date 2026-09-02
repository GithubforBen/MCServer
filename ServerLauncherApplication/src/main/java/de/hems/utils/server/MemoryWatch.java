package de.hems.utils.server;

import de.hems.types.ServerTemplate;
import de.hems.types.server.CapacityData;
import de.hems.types.server.MemoryAdviceData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Watches how much of the machine is promised away, refuses starts that no longer fit, and works out what
 * to do about it.
 * <p>
 * Two things live here because they only make sense together. The first is the budget: every server is
 * started with a fixed heap, and once those add up to what the machine has, the next one must not be
 * started - a box that hands out more heap than it owns does not fail loudly, it starts swapping and every
 * server on it lags at once.
 * <p>
 * The second is the way out of that. Every refusal is counted, and every running server is measured: how
 * much of its heap it actually holds, at its worst, since the launcher started watching. A server that was
 * given four gigabytes and has never gone past one is where the room for the next round is, and that is
 * what {@link #snapshot()} says out loud instead of leaving an admin to guess.
 */
public class MemoryWatch {

    /** How often every running server is measured. */
    private static final long SAMPLE_INTERVAL_MS = 30_000L;
    /** How much of a server's measured peak is left as air when suggesting a smaller heap. */
    private static final double HEADROOM = 1.4d;
    /** Suggestions below this are noise - nobody restarts a server to win 300 MB. */
    private static final int MIN_ADVICE_MB = 512;
    /** Suggested heaps are rounded up to this, because 1433 MB is not a number anybody types. */
    private static final int ADVICE_STEP_MB = 256;
    /** How far back "recently" reaches when the refusals are counted. */
    private static final long RECENT_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L;
    /** What is kept back for the operating system and the launcher when no reserve is configured. */
    private static final int DEFAULT_RESERVE_MB = 2048;

    private final ServerHandler servers;
    private final File file;
    private final YamlConfiguration config;
    /** Server name to the largest resident size it has been measured at, in MB. */
    private final Map<String, Integer> peaks = new ConcurrentHashMap<>();
    /** When a start was refused for want of memory, newest last. */
    private final List<Long> refusals = new CopyOnWriteArrayList<>();
    private int refusedTotal;
    private Thread sampler;

    public MemoryWatch(ServerHandler servers) {
        this(servers, new File("./capacity.yml"));
    }

    public MemoryWatch(ServerHandler servers, File file) {
        this.servers = servers;
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection measured = config.getConfigurationSection("peak-usage-mb");
        if (measured != null) {
            for (String key : measured.getKeys(false)) peaks.put(key, measured.getInt(key, 0));
        }
        refusals.addAll(config.getLongList("refused-at"));
        refusedTotal = config.getInt("refused-total", refusals.size());
    }

    private synchronized void save() {
        config.set("peak-usage-mb", null);
        for (Map.Entry<String, Integer> entry : peaks.entrySet()) {
            config.set("peak-usage-mb." + entry.getKey(), entry.getValue());
        }
        config.set("refused-at", new ArrayList<>(refusals));
        config.set("refused-total", refusedTotal);
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Starts measuring in the background.
     */
    public void start() {
        if (sampler != null) return;
        if (!ProcessMemory.isSupported()) {
            System.out.println("Memory usage cannot be measured on this machine - the server manager will "
                    + "show what is promised, but no recommendations.");
        }
        sampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(SAMPLE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    sample();
                } catch (Exception e) {
                    System.out.println("Could not measure the servers: " + e.getMessage());
                }
            }
        }, "memory-watch");
        sampler.setDaemon(true);
        sampler.start();
    }

    public void stop() {
        if (sampler != null) sampler.interrupt();
        sampler = null;
    }

    /**
     * Measures every running server once and keeps the worst reading per server.
     */
    private void sample() {
        if (!ProcessMemory.isSupported()) return;
        boolean changed = false;
        for (ServerInstance instance : servers.getInstances()) {
            int resident = ProcessMemory.residentMB(instance.getDirectory());
            if (resident <= 0) continue;
            String name = instance.getName().toString();
            Integer peak = peaks.get(name);
            if (peak != null && peak >= resident) continue;
            peaks.put(name, resident);
            changed = true;
        }
        if (changed) save();
    }

    /* ------------------------------------------------------------------ the budget */

    /**
     * @return the physical memory of the machine in MB, {@code 0} when it cannot be read
     */
    public static int machineMemoryMB() {
        try {
            var bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sun) {
                return (int) (sun.getTotalMemorySize() / (1024L * 1024L));
            }
        } catch (Throwable ignored) {
            // a jvm without the sun extension, which is not worth failing over
        }
        return 0;
    }

    /**
     * @return what is kept back for everything that is not a minecraft server
     */
    public int reserveMB() {
        return Math.max(0, MemoryLimits.read("MCSERVER_MEMORY_RESERVE_MB", "reserve-mb", DEFAULT_RESERVE_MB));
    }

    /**
     * How much heap every server together may promise.
     * <p>
     * Configured explicitly where the machine is shared with something else, otherwise everything it has
     * minus the reserve. Without a readable machine size there is nothing to divide up, and rather than
     * inventing a number the budget then simply does not bite.
     *
     * @return the budget in MB, {@link Integer#MAX_VALUE} when there is none
     */
    public int budgetMB() {
        int configured = MemoryLimits.read("MCSERVER_MEMORY_BUDGET_MB", "budget-mb", 0);
        if (configured > 0) return configured;
        int machine = machineMemoryMB();
        if (machine <= 0) return Integer.MAX_VALUE;
        return Math.max(MemoryLimits.FLOOR_MB, machine - reserveMB());
    }

    /**
     * @return how much heap the running servers have been promised altogether
     */
    public int allocatedMB() {
        int total = 0;
        servers.updateInstances();
        for (ServerInstance instance : servers.getInstances()) total += instance.getAllocatedMemoryMB();
        return total;
    }

    /**
     * @param memoryMB the heap a new server wants
     * @return whether it still fits into the budget
     */
    public boolean fits(int memoryMB) {
        int budget = budgetMB();
        if (budget == Integer.MAX_VALUE) return true;
        return allocatedMB() + memoryMB <= budget;
    }

    /**
     * Writes down that a start did not happen for want of memory.
     *
     * @param memoryMB who wanted how much
     * @param who      the player that asked, may be {@code null}
     * @param purpose  what they wanted to start
     */
    public synchronized void recordRefusal(int memoryMB, String who, String purpose) {
        long now = System.currentTimeMillis();
        refusals.add(now);
        refusedTotal++;
        // the list is only ever read as "how often recently", so old entries are dead weight
        refusals.removeIf(stamp -> now - stamp > RECENT_WINDOW_MS);
        save();
        System.out.println("Refused " + memoryMB + " MB for " + (purpose == null ? "a server" : purpose)
                + (who == null ? "" : " (" + who + ")") + ": " + allocatedMB() + " of " + budgetMB()
                + " MB are already promised.");
    }

    /* ------------------------------------------------------------------ the way out */

    /**
     * @return what the machine looks like right now, including what could be freed up
     */
    public CapacityData snapshot() {
        long now = System.currentTimeMillis();
        int recent = 0;
        long last = 0L;
        for (long stamp : refusals) {
            if (now - stamp <= RECENT_WINDOW_MS) recent++;
            if (stamp > last) last = stamp;
        }
        int budget = budgetMB();
        return new CapacityData(machineMemoryMB(), reserveMB(),
                budget == Integer.MAX_VALUE ? 0 : budget, allocatedMB(),
                recent, refusedTotal, last, advice());
    }

    /**
     * Looks for servers that hold far less than they were given.
     *
     * @return the suggestions, largest saving first
     */
    private ArrayList<MemoryAdviceData> advice() {
        ArrayList<MemoryAdviceData> advice = new ArrayList<>();
        int roundSize = ServerTemplate.BEDWARS.getDefaultMemoryMB();
        for (ServerInstance instance : servers.getInstances()) {
            String name = instance.getName().toString();
            Integer peak = peaks.get(name);
            if (peak == null || peak <= 0) continue;
            int allocated = instance.getAllocatedMemoryMB();
            int suggested = suggestFor(peak);
            if (allocated - suggested < MIN_ADVICE_MB) continue;
            advice.add(new MemoryAdviceData(name, allocated, peak, suggested,
                    roundSize <= 0 ? 0 : (allocated - suggested) / roundSize));
        }
        advice.sort(Comparator.comparingInt(MemoryAdviceData::getFreedMB).reversed());
        return advice;
    }

    /**
     * @param peakMB the most a server has been measured holding
     * @return a heap that leaves it room to grow, rounded to something an admin would type
     */
    private static int suggestFor(int peakMB) {
        int wanted = (int) Math.ceil(peakMB * HEADROOM);
        int rounded = ((wanted + ADVICE_STEP_MB - 1) / ADVICE_STEP_MB) * ADVICE_STEP_MB;
        return Math.max(MemoryLimits.FLOOR_MB, rounded);
    }
}
