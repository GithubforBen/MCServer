package de.hems.utils.server;

import java.util.ArrayList;
import java.util.List;

/**
 * What a game server's java is started with besides its jar.
 * <p>
 * A server started with nothing but a heap limit lets the collector choose for itself, and what it chooses
 * suits a batch job: a small young generation that fills up fast, and pauses of a tenth of a second while
 * it is emptied - which a player sees as the world standing still for two ticks. These are the flags the
 * minecraft community has settled on for G1 ("Aikar's flags"): a large young generation, because nearly
 * everything a tick allocates is dead by the next one, and a heap that is as big at the start as it will
 * ever be, so it is never resized in the middle of a game.
 * <p>
 * Left out on purpose is {@code AlwaysPreTouch}, which claims the whole heap from the system at start. A
 * launcher that runs half a dozen servers on one machine must not have each of them take its maximum
 * whether it needs it or not.
 */
public final class JvmFlags {

    /** Above this, the collector is given a bigger young generation and bigger regions. */
    private static final int LARGE_HEAP_MB = 12 * 1024;

    private JvmFlags() {
    }

    /**
     * @param memoryMB how much heap the server gets
     * @return the arguments to put in front of {@code -jar}, each one word
     */
    public static List<String> forGameServer(int memoryMB) {
        boolean large = memoryMB > LARGE_HEAP_MB;
        List<String> flags = new ArrayList<>(List.of(
                "-Xms" + memoryMB + "m",
                "-Xmx" + memoryMB + "m",
                // a java that has dropped one of these since must still start: a server that is tuned a
                // little worse is better than one that is not there
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+UseG1GC",
                "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+DisableExplicitGC",
                "-XX:G1NewSizePercent=" + (large ? 40 : 30),
                "-XX:G1MaxNewSizePercent=" + (large ? 50 : 40),
                "-XX:G1HeapRegionSize=" + (large ? "16M" : "8M"),
                "-XX:G1ReservePercent=" + (large ? 15 : 20),
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=" + (large ? 20 : 15),
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1"));
        return flags;
    }
}
