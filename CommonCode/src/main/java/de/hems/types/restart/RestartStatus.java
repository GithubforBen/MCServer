package de.hems.types.restart;

import java.io.Serializable;

/**
 * Whether a restart of the network is scheduled, and how the last update went.
 * <p>
 * The launcher owns it and sends it to every server, which count down on their own: the plan says when,
 * so nothing has to be sent every second.
 */
public class RestartStatus implements Serializable {

    private static final long serialVersionUID = 4400L;

    /** When the restart happens, epoch millis, or {@code 0} when none is scheduled. */
    private long at;
    private RestartMode mode;
    private String requestedBy;
    /** What {@code run.sh} wrote about the last update, or {@code null}. */
    private String lastUpdate;

    public RestartStatus() {
    }

    public RestartStatus(long at, RestartMode mode, String requestedBy, String lastUpdate) {
        this.at = at;
        this.mode = mode;
        this.requestedBy = requestedBy;
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return whether a restart is scheduled
     */
    public boolean isScheduled() {
        return at > 0 && mode != null;
    }

    /**
     * @return seconds until it happens, never below zero
     */
    public long getSecondsLeft() {
        return Math.max(0L, (at - System.currentTimeMillis() + 999L) / 1000L);
    }

    public long getAt() {
        return at;
    }

    public RestartMode getMode() {
        return mode;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param seconds a span
     * @return it written like "4:05" or "12 s"
     */
    public static String format(long seconds) {
        if (seconds < 60) return seconds + " s";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60) + " min";
    }
}
