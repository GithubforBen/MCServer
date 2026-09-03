package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The soul of whoever fell, drifting up out of them.
 * <p>
 * The slow one of the three: it takes two seconds and makes almost no noise, which is what somebody who
 * does not want an explosion over every kill is buying.
 */
public class SoulsKillEffect implements KillEffect {

    /** How long the soul takes to rise, in ticks. */
    private static final int DEFAULT_DURATION = 40;
    /** How often a puff is drawn on the way up, in ticks. */
    private static final int INTERVAL = 2;

    @Override
    public String getId() {
        return Cosmetics.KILL_SOULS;
    }

    @Override
    public void play(KillContext context) {
        World world = context.world();
        if (world == null) return;
        Location from = context.where().clone().add(0.0d, 0.5d, 0.0d);
        int duration = Math.max(INTERVAL, context.setting(Cosmetics.SETTING_DURATION_TICKS, DEFAULT_DURATION));

        world.playSound(from, Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.AMBIENT, 0.8f, 0.8f);
        new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                if (elapsed >= duration) {
                    cancel();
                    return;
                }
                elapsed += INTERVAL;
                // a hand's width per step, so the soul is still over the body when the fight moves on
                Location at = from.clone().add(0.0d, elapsed * 0.06d, 0.0d);
                world.spawnParticle(Particle.SOUL, at, 4, 0.15d, 0.1d, 0.15d, 0.01d);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 1, 0.1d, 0.1d, 0.1d, 0.0d);
            }
        }.runTaskTimer(context.plugin(), 0L, INTERVAL);
    }
}
