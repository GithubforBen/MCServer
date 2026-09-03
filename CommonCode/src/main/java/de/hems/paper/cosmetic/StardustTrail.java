package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Small white sparks that sink slowly behind their owner.
 * <p>
 * The quiet one. End rods hang in the air for a moment instead of vanishing at once, which is what makes
 * this read as a line somebody walked rather than as a puff at their feet.
 */
public class StardustTrail implements TrailEffect {

    @Override
    public String getId() {
        return Cosmetics.TRAIL_STARDUST;
    }

    @Override
    public void draw(TrailContext context) {
        World world = context.world();
        if (world == null) return;
        world.spawnParticle(Particle.END_ROD, context.feet().add(0.0d, 0.4d, 0.0d), 2,
                0.12d, 0.12d, 0.12d, 0.0d);
    }
}
