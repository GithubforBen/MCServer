package de.schnorrenbergers.bedwars.game;

import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * Which blocks players put there.
 * <p>
 * The rule of the game mode is that you may only break what somebody built, and there is no way to read
 * that off a block - so it is remembered as it is placed. Positions are packed into a single long because
 * a busy round places tens of thousands of blocks, and a set of objects for that is a lot of memory for a
 * question that is only ever "yes or no".
 */
public final class BlockTracker {

    private final Set<Long> placed = new HashSet<>();

    /**
     * @param block a block somebody just placed
     */
    public void remember(Block block) {
        placed.add(key(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * @param block a block somebody wants to break
     * @return whether it was placed during this round
     */
    public boolean wasPlaced(Block block) {
        return placed.contains(key(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * @param block a block that is gone
     */
    public void forget(Block block) {
        placed.remove(key(block.getX(), block.getY(), block.getZ()));
    }

    public int size() {
        return placed.size();
    }

    public void clear() {
        placed.clear();
    }

    /**
     * Packs a position into one long: 26 bits for x and z, 12 for y - the same shape minecraft itself uses
     * for block positions, which covers every coordinate a map can have.
     */
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
