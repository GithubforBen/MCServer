package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * A column of light that grows out of every winner up past the map.
 * <p>
 * The one that is visible from the other side of the arena, which is the point: everybody who lost is
 * standing somewhere else, and this is the effect that tells them where the winners are.
 */
public class BeaconWinEffect implements WinEffect {

    /** How long the column keeps standing, in ticks. */
    private static final int DEFAULT_DURATION = 20 * 10;
    /** How often it is redrawn, in ticks. */
    private static final int INTERVAL = 4;
    /** How far above the build height it reaches, in blocks. */
    private static final int OVERSHOOT = 20;
    /** How far apart the points of the column are, in blocks. */
    private static final double STEP = 0.5d;

    @Override
    public String getId() {
        return Cosmetics.WIN_BEACON;
    }

    @Override
    public void play(WinContext context) {
        World world = context.world();
        if (world == null) return;
        List<Player> winners = context.winners();
        if (winners.isEmpty()) return;
        int duration = Math.max(INTERVAL, context.setting(Cosmetics.SETTING_DURATION_TICKS, DEFAULT_DURATION));

        for (Player winner : winners) {
            if (winner.isOnline() && winner.getWorld() == world) {
                world.playSound(winner.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
                        SoundCategory.AMBIENT, 0.8f, 1.2f);
            }
        }
        new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                if (elapsed >= duration) {
                    cancel();
                    return;
                }
                elapsed += INTERVAL;
                for (Player winner : winners) {
                    if (!winner.isOnline() || winner.getWorld() != world) continue;
                    column(world, winner.getLocation(), context.topY() + OVERSHOOT);
                }
            }
        }.runTaskTimer(context.plugin(), 0L, INTERVAL);
    }

    /**
     * Draws one column, from somebody's feet up to a height.
     *
     * @param world where
     * @param from  the bottom of it
     * @param topY  where it ends
     */
    private static void column(World world, Location from, int topY) {
        // from the winner upwards, and never downwards: a winner standing on a tower would otherwise get
        // a column that starts under the map and is invisible for most of its length
        double height = Math.max(4.0d, topY - from.getY());
        for (double y = 0.0d; y < height; y += STEP) {
            Location at = from.clone().add(0.0d, y, 0.0d);
            world.spawnParticle(Particle.END_ROD, at, 1, 0.12d, 0.0d, 0.12d, 0.0d);
            if (y % 2.0d < STEP) {
                world.spawnParticle(Particle.FIREWORK, at, 1, 0.25d, 0.0d, 0.25d, 0.0d);
            }
        }
    }
}
