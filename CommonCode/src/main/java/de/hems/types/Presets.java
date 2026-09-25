package de.hems.types;

/**
 * Stepping through a list of preset values, the way every number in the menus is changed.
 * <p>
 * A number is never typed in: a click moves to the next preset, a right click to the one before, and the
 * end wraps around to the start. A value that is not a preset at all - one edited on the website, say -
 * moves to the nearest one instead of jumping to the first.
 */
public final class Presets {

    private Presets() {
    }

    /**
     * @param values  the presets, in order
     * @param current where we are now
     * @param forward whether to go up or down
     * @return the next preset
     */
    public static int step(int[] values, int current, boolean forward) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[wrap(i + (forward ? 1 : -1), values.length)];
        }
        int nearest = values[0];
        for (int value : values) {
            if (Math.abs(value - current) < Math.abs(nearest - current)) nearest = value;
        }
        return nearest;
    }

    /**
     * @param values  the presets, in order
     * @param current where we are now
     * @param forward whether to go up or down
     * @return the next preset
     */
    public static long step(long[] values, long current, boolean forward) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[wrap(i + (forward ? 1 : -1), values.length)];
        }
        long nearest = values[0];
        for (long value : values) {
            if (Math.abs(value - current) < Math.abs(nearest - current)) nearest = value;
        }
        return nearest;
    }

    private static int wrap(int index, int length) {
        return ((index % length) + length) % length;
    }
}
