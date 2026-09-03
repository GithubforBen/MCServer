package de.schnorrenbergers.bedwars.game.timeline;

import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.game.BlockTracker;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * What the dragons and the withers of the sudden death have in common.
 * <p>
 * Both belong to a team, and both are there to make the map smaller: from the moment the event starts the
 * arena stops being a place to hide in, because there is less of it every minute. Two listeners and both
 * mobs need to ask the same two questions, so they are answered in one place.
 */
public final class SuddenDeath {

    private SuddenDeath() {
    }

    /**
     * @param game   the round
     * @param entity anything at all
     * @return the team whose sudden death mob it is, {@code null} when it is not one of them
     */
    public static @Nullable GameTeam ownerOf(@Nullable Game game, @Nullable Entity entity) {
        if (game == null || entity == null) return null;
        GameTeam owner = game.getDragons() == null ? null : game.getDragons().ownerOf(entity);
        if (owner != null) return owner;
        return game.getWithers() == null ? null : game.getWithers().ownerOf(entity);
    }

    /**
     * @param game   the round
     * @param entity anything at all
     * @return whether it is one of the mobs of the sudden death
     */
    public static boolean isOurs(@Nullable Game game, @Nullable Entity entity) {
        return ownerOf(game, entity) != null;
    }

    /**
     * Takes a ball of blocks out of the world.
     * <p>
     * Everything goes except what {@code sudden-death.indestructible} keeps: this is the one rule of the
     * round that is allowed to touch the map itself, and it is the whole point of the event - the arena
     * shrinks until what is left of it is not worth standing on, and standing on nothing is the void.
     * Nothing is dropped, because a hole in the floor that rains wool would be picked up and rebuilt.
     *
     * @param game     the round
     * @param settings what may not be taken
     * @param at       the middle of the ball
     * @param radius   how wide it is, in blocks
     */
    public static void carve(Game game, TimelineSettings settings, Location at, double radius) {
        World world = at.getWorld();
        if (world == null || radius <= 0.0d) return;
        BlockTracker tracker = game.getBlockTracker();
        int reach = (int) Math.ceil(radius);
        double squared = radius * radius;
        int centreX = at.getBlockX();
        int centreY = at.getBlockY();
        int centreZ = at.getBlockZ();

        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {
                    if (x * x + y * y + z * z > squared) continue;
                    int blockY = centreY + y;
                    if (blockY < world.getMinHeight() || blockY > world.getMaxHeight()) continue;
                    Block block = world.getBlockAt(centreX + x, blockY, centreZ + z);
                    if (block.getType().isAir() || settings.isIndestructible(block.getType())) continue;
                    // no physics update: a dragon flying over an arena would otherwise set off every
                    // sand tower and every water source it passes, on the main thread, four times a second
                    block.setType(Material.AIR, false);
                    tracker.forget(block);
                }
            }
        }
    }
}
