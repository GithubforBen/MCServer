package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Explosions raining down over the whole map.
 * <p>
 * They start at the height the map may be built to and fall until they hit something, where each one goes
 * off with a bang and no blast: this is a picture, not a weapon, so nothing is damaged, nothing is broken
 * and nobody is thrown anywhere. The round is over when it plays, and the losers should be able to watch
 * it rather than be knocked around by it.
 * <p>
 * The number of drops in the air is capped. An effect that can be bought is an effect a full server will
 * eventually play four of at once, and a celebration that costs the round its tick rate is not a feature.
 */
public class InkWinEffect implements WinEffect {

    /** How long it rains, in ticks. */
    private static final String SETTING_DURATION = "duration-ticks";
    private static final int DEFAULT_DURATION = 20 * 10;
    /** How many new drops start each tick. */
    private static final String SETTING_DENSITY = "drops-per-tick";
    private static final int DEFAULT_DENSITY = 3;
    /** How many may be falling at once, whatever the density says. */
    private static final int MAX_ALIVE = 160;
    /** How many blocks a drop falls per tick. */
    private static final double SPEED = 1.6d;
    /** How far below the map a drop gives up, so one over the void does not fall forever. */
    private static final int GIVE_UP_BELOW = 12;

    @Override
    public String getId() {
        return Cosmetics.WIN_INK;
    }

    @Override
    public void play(WinContext context) {
        World world = context.world();
        if (world == null) return;
        int duration = Math.max(20, context.setting(SETTING_DURATION, DEFAULT_DURATION));
        int density = Math.max(1, Math.min(16, context.setting(SETTING_DENSITY, DEFAULT_DENSITY)));
        double radius = Math.max(8d, context.radius());
        Location center = context.center();
        int top = Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 1, context.topY()));
        int floor = Math.max(world.getMinHeight(), center.getBlockY() - GIVE_UP_BELOW);

        new BukkitRunnable() {
            final List<Drop> drops = new ArrayList<>();
            int elapsed;

            @Override
            public void run() {
                elapsed++;
                if (elapsed <= duration) start();
                fall();
                if (elapsed > duration && drops.isEmpty()) cancel();
            }

            /**
             * Lets new drops go, somewhere over the map.
             */
            private void start() {
                for (int i = 0; i < density && drops.size() < MAX_ALIVE; i++) {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    double angle = random.nextDouble(Math.PI * 2);
                    // the square root spreads them evenly over the circle instead of crowding the middle
                    double distance = Math.sqrt(random.nextDouble()) * radius;
                    drops.add(new Drop(
                            center.getX() + Math.cos(angle) * distance,
                            center.getZ() + Math.sin(angle) * distance,
                            top));
                }
            }

            /**
             * Moves every drop down a step and lands the ones that arrived.
             */
            private void fall() {
                Iterator<Drop> iterator = drops.iterator();
                while (iterator.hasNext()) {
                    Drop drop = iterator.next();
                    drop.y -= SPEED;
                    Location at = new Location(world, drop.x, drop.y, drop.z);
                    if (drop.y <= floor) {
                        iterator.remove();
                        continue;
                    }
                    // never touch a chunk that is not loaded: reading a block there loads it, and an
                    // effect that generates terrain to look at it is not an effect, it is a stall
                    if (!world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) continue;
                    if (!at.getBlock().getType().isAir()) {
                        land(at);
                        iterator.remove();
                        continue;
                    }
                    world.spawnParticle(Particle.EXPLOSION, at, 1, 0.1, 0.1, 0.1, 0d);
                }
            }

            /**
             * One drop arriving: a bang and a puff, and nothing else.
             */
            private void land(Location at) {
                world.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1, 0d, 0d, 0d, 0d);
                // quiet on purpose - a hundred of these at full volume is not a celebration, it is a wall
                world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 0.35f,
                        (float) ThreadLocalRandom.current().nextDouble(0.8d, 1.3d));
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    /**
     * One falling explosion: a fixed spot on the map and a height that shrinks.
     */
    private static final class Drop {
        private final double x;
        private final double z;
        private double y;

        private Drop(double x, double z, double y) {
            this.x = x;
            this.z = z;
            this.y = y;
        }
    }
}
