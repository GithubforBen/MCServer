package de.hems.types.server;

import java.io.Serializable;

/**
 * One suggestion of the kind "this server was given far more memory than it ever uses".
 * <p>
 * Every number in here was measured rather than guessed: {@link #getPeakUsedMB()} is the largest resident
 * size the process has actually reached since the launcher started watching it. A suggestion is only ever
 * built when there is such a measurement, because "you could probably take some memory away" without a
 * number behind it is how a server ends up being restarted into an out of memory kill.
 */
public class MemoryAdviceData implements Serializable {

    private static final long serialVersionUID = 4601L;

    private String server;
    private int allocatedMB;
    private int peakUsedMB;
    private int suggestedMB;
    private int extraRounds;

    public MemoryAdviceData() {
    }

    public MemoryAdviceData(String server, int allocatedMB, int peakUsedMB, int suggestedMB, int extraRounds) {
        this.server = server;
        this.allocatedMB = allocatedMB;
        this.peakUsedMB = peakUsedMB;
        this.suggestedMB = suggestedMB;
        this.extraRounds = extraRounds;
    }

    public String getServer() {
        return server;
    }

    /** @return the heap the server was started with */
    public int getAllocatedMB() {
        return allocatedMB;
    }

    /** @return the most it has actually used */
    public int getPeakUsedMB() {
        return peakUsedMB;
    }

    /** @return what it could be given instead */
    public int getSuggestedMB() {
        return suggestedMB;
    }

    /** @return how much would be free afterwards */
    public int getFreedMB() {
        return Math.max(0, allocatedMB - suggestedMB);
    }

    /** @return how many more rounds would fit into what this frees up */
    public int getExtraRounds() {
        return extraRounds;
    }
}
