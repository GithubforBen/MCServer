package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;

/**
 * A ring of fire where the loser went down.
 * <p>
 * Particles only. It looks like an explosion and is not one: nothing is damaged, nothing is moved and no
 * block is touched, because the moment a cosmetic can push somebody it stops being a cosmetic and starts
 * being a weapon somebody paid bits for.
 */
public class BlastKillEffect implements KillEffect {

    /** How many flames the ring is made of. */
    private static final int POINTS = 24;
    /** How wide it is, in blocks. */
    private static final double RADIUS = 1.6d;

    @Override
    public String getId() {
        return Cosmetics.KILL_BLAST;
    }

    @Override
    public void play(KillContext context) {
        World world = context.world();
        if (world == null) return;
        Location at = context.where().clone().add(0.0d, 0.2d, 0.0d);

        world.spawnParticle(Particle.EXPLOSION_EMITTER, at.clone().add(0.0d, 0.8d, 0.0d), 1,
                0.0d, 0.0d, 0.0d, 0.0d);
        for (int i = 0; i < POINTS; i++) {
            double angle = 2.0d * Math.PI * i / POINTS;
            Location point = at.clone().add(Math.cos(angle) * RADIUS, 0.0d, Math.sin(angle) * RADIUS);
            // the offsets are the direction the flame drifts, not a spread: outwards and up, so the ring
            // opens rather than sitting there
            world.spawnParticle(Particle.FLAME, point, 0,
                    Math.cos(angle) * 0.12d, 0.06d, Math.sin(angle) * 0.12d, 1.0d);
        }
        world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 0.45f, 1.4f);
    }
}
