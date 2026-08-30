package de.schnorrenbergers.lobby.parkour;

/**
 * One player's attempt at one course.
 * <p>
 * Kept in memory only. A run that is interrupted by a restart is a run that did not happen - the only
 * thing worth keeping is the time somebody actually finished in, and that goes into the file.
 */
public class ParkourRun {

    private final ParkourCourse course;
    private final long startedAt;
    /** How many checkpoints are behind the runner, which is also the index of the next one. */
    private int reached;

    public ParkourRun(ParkourCourse course) {
        this.course = course;
        this.startedAt = System.currentTimeMillis();
    }

    public ParkourCourse getCourse() {
        return course;
    }

    /**
     * @return how long the run has been going, in milliseconds
     */
    public long elapsed() {
        return System.currentTimeMillis() - startedAt;
    }

    public int getReached() {
        return reached;
    }

    public void reachedOneMore() {
        reached++;
    }

    /**
     * @return where a fall puts the runner back: the last checkpoint, or the start when there is none yet
     */
    public ParkourPoint lastSafeSpot() {
        ParkourPoint checkpoint = course.getCheckpoint(reached - 1);
        return checkpoint != null ? checkpoint : course.getStart();
    }
}
