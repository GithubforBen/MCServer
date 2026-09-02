package de.hems.types.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * How much room the machine still has, and what could be done about it when it has none.
 * <p>
 * This is what stands between a player pressing "start a round" and a machine that starts swapping: a
 * round is two gigabytes of heap, and a box that has already promised all of its memory away cannot give
 * out another two. The refusals are counted, because a number - "37 rounds could not be started this week"
 * - is what turns "the server feels full" into something an admin can act on.
 */
public class CapacityData implements Serializable {

    private static final long serialVersionUID = 4602L;

    private int totalMachineMB;
    private int reserveMB;
    private int budgetMB;
    private int allocatedMB;
    private int refusedRecently;
    private int refusedTotal;
    private long lastRefusedAt;
    private ArrayList<MemoryAdviceData> advice = new ArrayList<>();

    public CapacityData() {
    }

    public CapacityData(int totalMachineMB, int reserveMB, int budgetMB, int allocatedMB,
                        int refusedRecently, int refusedTotal, long lastRefusedAt,
                        ArrayList<MemoryAdviceData> advice) {
        this.totalMachineMB = totalMachineMB;
        this.reserveMB = reserveMB;
        this.budgetMB = budgetMB;
        this.allocatedMB = allocatedMB;
        this.refusedRecently = refusedRecently;
        this.refusedTotal = refusedTotal;
        this.lastRefusedAt = lastRefusedAt;
        this.advice = advice == null ? new ArrayList<>() : advice;
    }

    /** @return the physical memory of the machine, {@code 0} when it could not be read */
    public int getTotalMachineMB() {
        return totalMachineMB;
    }

    /** @return what is kept back for the operating system and the launcher itself */
    public int getReserveMB() {
        return reserveMB;
    }

    /** @return how much heap the servers together may promise */
    public int getBudgetMB() {
        return budgetMB;
    }

    /** @return how much of the budget is promised right now */
    public int getAllocatedMB() {
        return allocatedMB;
    }

    /** @return what is left of the budget */
    public int getFreeMB() {
        return Math.max(0, budgetMB - allocatedMB);
    }

    /**
     * @param memoryMB the heap a new server would want
     * @return whether it still fits
     */
    public boolean fits(int memoryMB) {
        return getFreeMB() >= memoryMB;
    }

    /** @return how many starts were refused for want of memory in the last week */
    public int getRefusedRecently() {
        return refusedRecently;
    }

    /** @return how many starts were refused for want of memory ever */
    public int getRefusedTotal() {
        return refusedTotal;
    }

    /** @return when the last one was refused, {@code 0} when none ever was */
    public long getLastRefusedAt() {
        return lastRefusedAt;
    }

    /**
     * @return the servers that were measured using far less than they were given, largest saving first
     */
    public List<MemoryAdviceData> getAdvice() {
        return advice == null ? List.of() : advice;
    }

    /** @return how much memory following every suggestion would free up */
    public int getFreeableMB() {
        int total = 0;
        for (MemoryAdviceData entry : getAdvice()) total += entry.getFreedMB();
        return total;
    }
}
