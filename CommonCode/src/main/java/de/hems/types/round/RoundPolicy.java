package de.hems.types.round;

import java.io.Serializable;

/**
 * What players are allowed to start on their own.
 * <p>
 * Every knob here exists because there is a moment where the answer has to be no. During an event the
 * machine belongs to the event; shortly before one it is about to; one person should not be able to fill
 * the whole network with rounds nobody joins; and when the memory is gone it is gone regardless of what
 * anybody is allowed. The last one is not in here - it is measured, not configured.
 */
public class RoundPolicy implements Serializable {

    private static final long serialVersionUID = 4702L;

    /** How long a round may sit in the list without a server behind it before it is cleaned up. */
    public static final long STALE_AFTER_MS = 30L * 60L * 1000L;

    private boolean selfStartEnabled = false;
    private int maxPerPlayer = 1;
    private int cooldownSeconds = 300;
    private int maxRounds = 4;
    private boolean blockWhileEventRunning = true;
    private int blockBeforeEventMinutes = 15;
    private int memoryMB = 0;

    public RoundPolicy() {
    }

    /**
     * @return whether anybody but an operator may start a round
     */
    public boolean isSelfStartEnabled() {
        return selfStartEnabled;
    }

    public void setSelfStartEnabled(boolean selfStartEnabled) {
        this.selfStartEnabled = selfStartEnabled;
    }

    /**
     * @return how many rounds one player may have open at once
     */
    public int getMaxPerPlayer() {
        return Math.max(1, maxPerPlayer);
    }

    public void setMaxPerPlayer(int maxPerPlayer) {
        this.maxPerPlayer = Math.max(1, Math.min(10, maxPerPlayer));
    }

    /**
     * @return how long a player has to wait between two starts, in seconds
     */
    public int getCooldownSeconds() {
        return Math.max(0, cooldownSeconds);
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, Math.min(3600, cooldownSeconds));
    }

    /**
     * @return how many self started rounds may run at once across the network, {@code 0} for no limit
     */
    public int getMaxRounds() {
        return Math.max(0, maxRounds);
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = Math.max(0, Math.min(64, maxRounds));
    }

    /**
     * @return whether a running event blocks self started rounds
     */
    public boolean isBlockWhileEventRunning() {
        return blockWhileEventRunning;
    }

    public void setBlockWhileEventRunning(boolean blockWhileEventRunning) {
        this.blockWhileEventRunning = blockWhileEventRunning;
    }

    /**
     * @return how long before an event starts nobody may start their own round any more, in minutes
     */
    public int getBlockBeforeEventMinutes() {
        return Math.max(0, blockBeforeEventMinutes);
    }

    public void setBlockBeforeEventMinutes(int blockBeforeEventMinutes) {
        this.blockBeforeEventMinutes = Math.max(0, Math.min(240, blockBeforeEventMinutes));
    }

    /**
     * @return the heap a round server gets, {@code 0} for whatever the template says
     */
    public int getMemoryMB() {
        return Math.max(0, memoryMB);
    }

    public void setMemoryMB(int memoryMB) {
        this.memoryMB = Math.max(0, Math.min(16384, memoryMB));
    }

    public RoundPolicy copy() {
        RoundPolicy copy = new RoundPolicy();
        copy.selfStartEnabled = selfStartEnabled;
        copy.maxPerPlayer = maxPerPlayer;
        copy.cooldownSeconds = cooldownSeconds;
        copy.maxRounds = maxRounds;
        copy.blockWhileEventRunning = blockWhileEventRunning;
        copy.blockBeforeEventMinutes = blockBeforeEventMinutes;
        copy.memoryMB = memoryMB;
        return copy;
    }
}
