package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Flames behind whoever is wearing it.
 * <p>
 * {@code SMALL_FLAME} rather than {@code FLAME}: the big one at ten steps a second is a wall of fire that
 * nobody can see through, and a trail that hides the person wearing it is a trail they take off again.
 */
public class FlameTrail implements TrailEffect {

    @Override
    public String getId() {
        return Cosmetics.TRAIL_FLAME;
    }

    @Override
    public void draw(TrailContext context) {
        World world = context.world();
        if (world == null) return;
        world.spawnParticle(Particle.SMALL_FLAME, context.feet(), 3, 0.15d, 0.05d, 0.15d, 0.0d);
    }
}
