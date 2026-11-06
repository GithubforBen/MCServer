package com.benschnorr.survival.featrues.lab.brew;

/**
 * The progress of one brew on one stand, without any Minecraft in it.
 * <p>
 * Time only moves when {@link #advance} is called, so a paused stand (chunk unloaded) costs nothing and
 * a fake clock drives it in {@code LabBrewCheck}. see knowledge: /decisions/lab-brewing-engine.md
 */
public final class BrewState {

    /** What one step did. */
    public enum Step {
        /** Nothing running and nothing to start. */
        IDLE,
        /** A brew started in this step. */
        STARTED,
        /** A brew is running. */
        RUNNING,
        /** The brew ran out in this step: use the ingredients now. */
        FINISHED,
        /** The ingredient went away: the brew is gone and starts over when one returns. */
        RESET
    }

    private final int duration;
    private int progress;
    private boolean running;

    public BrewState(int duration) {
        this.duration = Math.max(1, duration);
    }

    /** Reads {@link #save()}; anything unreadable becomes a fresh state, never an exception. */
    public static BrewState load(String saved, int duration) {
        BrewState state = new BrewState(duration);
        if (saved == null) return state;
        try {
            int value = Integer.parseInt(saved.trim());
            if (value >= 0) {
                state.progress = Math.min(value, state.duration - 1);
                state.running = true;
            }
        } catch (NumberFormatException ignored) {
            // a damaged value costs the brew its progress and nothing else
        }
        return state;
    }

    /** The value to keep in the block; null when there is nothing to keep. */
    public String save() {
        return running ? Integer.toString(progress) : null;
    }

    /**
     * Starts a brew. A second start while one runs does nothing, so no second brew ever runs on a stand.
     *
     * @return true when this call started it
     */
    public boolean start() {
        if (running) return false;
        running = true;
        progress = 0;
        return true;
    }

    /**
     * Moves time on by the given ticks.
     *
     * @param ticks      time that passed while the stand was loaded
     * @param ingredient whether the ingredient is still on top and the brew may run
     */
    public Step advance(int ticks, boolean ingredient) {
        if (!running) {
            if (!ingredient) return Step.IDLE;
            start();
            return Step.STARTED;
        }
        if (!ingredient) {
            running = false;
            progress = 0;
            return Step.RESET;
        }
        progress += Math.max(0, ticks);
        if (progress >= duration) {
            running = false;
            progress = 0;
            return Step.FINISHED;
        }
        return Step.RUNNING;
    }

    public boolean isRunning() {
        return running;
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }

    /** Between 0 and 1. */
    public double fraction() {
        return running ? (double) progress / duration : 0;
    }

    /**
     * Ticks left on a client's arrow of {@code total} ticks, counting down like vanilla's, so the arrow
     * fills as the brew advances. {@code sinceStep} is the time since this state was last advanced.
     */
    public int arrowTicks(int sinceStep, int total) {
        if (!running) return 0;
        double done = Math.min(1.0, (double) (progress + Math.max(0, sinceStep)) / duration);
        return (int) Math.round(total * (1 - done));
    }
}
