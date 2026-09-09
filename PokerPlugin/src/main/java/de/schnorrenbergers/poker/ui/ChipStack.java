package de.schnorrenbergers.poker.ui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * A pile of chips on the felt.
 * <p>
 * Every bet at the table is shown as a stack rather than as a number, because a number is something you
 * read and a stack is something you see. Somebody who has shoved is a tower; somebody who limped is two
 * discs. From across the table that is the state of the hand at a glance, and it is the thing that makes a
 * poker table look like a poker table.
 * <p>
 * A stack is broken into denominations the way a real one is - the big chips at the bottom - so a pot of
 * four thousand is a hand's width of colour and not a column into the ceiling.
 */
public final class ChipStack {

    /** What a chip of each colour is worth, biggest first, and what it is drawn out of. */
    private static final int[] VALUES = {1000, 500, 100, 25, 5, 1};
    private static final Material[] COLOURS = {
            Material.BLACK_CONCRETE,
            Material.PURPLE_CONCRETE,
            Material.LIME_CONCRETE,
            Material.GREEN_CONCRETE,
            Material.RED_CONCRETE,
            Material.WHITE_CONCRETE};

    /** How wide a chip is. */
    private static final float DIAMETER = 0.34f;
    /** How thick a chip is. */
    private static final float HEIGHT = 0.045f;
    /**
     * How many chips one column holds before a new one is started next to it.
     * <p>
     * Twelve is about the height of a real stack, and it is also what keeps a big pot from being a spike
     * that hides half the table behind it.
     */
    private static final int PER_COLUMN = 12;
    /** How far apart two columns stand. */
    private static final double COLUMN_SPACING = 0.36d;
    /** The most discs one bet is ever drawn with, however large it is. */
    private static final int MAX_DISCS = 60;

    private final List<BlockDisplay> discs = new ArrayList<>();
    private final Location anchor;
    private int shown = -1;

    /**
     * @param anchor where the middle of the pile sits, on the felt
     */
    public ChipStack(Location anchor) {
        this.anchor = anchor.clone();
    }

    /**
     * Draws a given number of chips, doing nothing when it is already showing that many.
     *
     * @param amount how many chips the pile is worth
     */
    public void set(int amount) {
        if (amount == shown) return;
        shown = amount;
        clear();
        if (amount <= 0) return;

        List<Material> pieces = breakDown(amount);
        for (int i = 0; i < pieces.size(); i++) {
            int column = i / PER_COLUMN;
            int height = i % PER_COLUMN;
            // the columns fan out to the right of the anchor and slightly back, so a pile of five columns
            // reads as a heap rather than as a wall
            double offsetX = (column % 3 - 1) * COLUMN_SPACING;
            double offsetZ = (column / 3) * COLUMN_SPACING;
            Location at = anchor.clone().add(offsetX, height * HEIGHT, offsetZ);
            BlockDisplay disc = Displays.disc(at, pieces.get(i), DIAMETER, HEIGHT);
            if (disc != null) discs.add(disc);
        }
    }

    /**
     * Splits an amount into chips, biggest first.
     *
     * @param amount what the pile is worth
     * @return the colours to stack, bottom first
     */
    private static List<Material> breakDown(int amount) {
        List<Material> pieces = new ArrayList<>();
        int left = amount;
        for (int i = 0; i < VALUES.length && pieces.size() < MAX_DISCS; i++) {
            int count = left / VALUES[i];
            for (int c = 0; c < count && pieces.size() < MAX_DISCS; c++) {
                pieces.add(COLOURS[i]);
            }
            left -= count * VALUES[i];
        }
        // a pot far past the biggest denomination would be a thousand discs, so it stops at a full-looking
        // heap. The number next to it is what says the exact amount
        return pieces;
    }

    /**
     * Takes the pile off the table.
     */
    public void clear() {
        Displays.removeAll(discs);
    }

    /**
     * Takes the pile off and forgets what it was showing, so the next {@link #set} redraws from nothing.
     */
    public void reset() {
        clear();
        shown = -1;
    }

    public Location getAnchor() {
        return anchor.clone();
    }
}
