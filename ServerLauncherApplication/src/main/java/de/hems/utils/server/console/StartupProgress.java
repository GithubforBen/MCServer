package de.hems.utils.server.console;

import de.hems.types.ServerPhase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads how far a server is with starting up out of its console output.
 * <p>
 * The port is no help here: paper binds it while it is still loading worlds, so a socket that answers only
 * proves that the process is alive, not that it will let anybody in. The console on the other hand says
 * exactly what the server is doing, and that is what a waiting player wants to be told - "terrain wird
 * gebaut, 40%" rather than a spinner that means nothing.
 */
public class StartupProgress {

    /** Paper counts the spawn area up in percent while it generates it. */
    private static final Pattern SPAWN_AREA = Pattern.compile("Preparing spawn area:\\s*(\\d{1,3})%");
    /**
     * The line that means the server is finished. Matched with the opening bracket of the boot time,
     * because paper also writes {@code Done preparing level "world" (3.887s)} halfway through.
     */
    private static final Pattern DONE = Pattern.compile("\\bDone \\(\\d");

    /**
     * How long a reachable server that never said a word is given before it counts as up. Long enough for
     * a small world to be generated, because without a console there is nothing better to go on than the
     * port - which paper opens before it loads anything.
     */
    private static final long SILENT_FALLBACK_MS = 30_000L;
    /** How long a reachable server that talks but never says it is done is given. */
    private static final long TALKING_FALLBACK_MS = 90_000L;

    private volatile ServerPhase phase = ServerPhase.QUEUED;
    private volatile int percent;
    /** Whether any console output has been seen at all, which tells a broken pipe from a slow server. */
    private volatile boolean sawOutput;
    /** Since when the port has been answering without interruption, {@code 0} when it is not. */
    private volatile long reachableSince;

    /**
     * Feeds one console line in.
     *
     * @param line the line, already free of terminal escape codes
     */
    public void onLine(String line) {
        if (line == null || line.isBlank()) return;
        sawOutput = true;
        if (phase == ServerPhase.READY || phase == ServerPhase.STOPPING) return;

        if (DONE.matcher(line).find()) {
            phase = ServerPhase.READY;
            percent = 100;
            return;
        }
        Matcher spawnArea = SPAWN_AREA.matcher(line);
        if (spawnArea.find()) {
            phase = ServerPhase.GENERATING;
            percent = Math.min(100, Integer.parseInt(spawnArea.group(1)));
            return;
        }
        if (line.contains("Preparing level")) {
            phase = ServerPhase.GENERATING;
            return;
        }
        if (phase == ServerPhase.QUEUED) phase = ServerPhase.STARTING;
    }

    /**
     * The process was handed to tmux, so the server is no longer only queued.
     */
    public void onLaunched() {
        phase = ServerPhase.STARTING;
        percent = 0;
    }

    /**
     * A stop was asked for - no new players belong on this server from here on.
     */
    public void onStopping() {
        phase = ServerPhase.STOPPING;
        percent = 0;
    }

    /**
     * Starts over, for a server that is being restarted.
     */
    public void reset() {
        phase = ServerPhase.QUEUED;
        percent = 0;
        sawOutput = false;
        reachableSince = 0L;
    }

    /**
     * Accepts that the server is up without having seen it say so.
     * <p>
     * The console arrives through a tmux pipe, and a pipe that never got set up - or that lost the one line
     * that matters - would otherwise leave a perfectly healthy server stuck at "starting" forever, and with
     * it every player waiting to be warped there. A port that keeps answering is the second opinion: a
     * server that never said a word is given a few seconds, one that is talking but has not reported being
     * done is given long enough for a real world generation to finish first.
     *
     * @param reachable whether the server answers on its port
     */
    public void onProbed(boolean reachable) {
        if (!reachable) {
            reachableSince = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (reachableSince == 0L) reachableSince = now;
        if (!phase.isStartingUp()) return;
        if (now - reachableSince < (sawOutput ? TALKING_FALLBACK_MS : SILENT_FALLBACK_MS)) return;
        phase = ServerPhase.READY;
        percent = 100;
    }

    public ServerPhase getPhase() {
        return phase;
    }

    public int getPercent() {
        return percent;
    }
}
