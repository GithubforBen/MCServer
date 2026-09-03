package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Notes in whatever colour, for people who want to be seen coming.
 * <p>
 * A note particle takes its colour from the x offset and only when the count is zero - that is the whole
 * api for it, and with a count above zero it is always the same green. So: one note at a time, with a
 * random colour, which is also why this one is drawn less often than the others.
 */
public class NotesTrail implements TrailEffect {

    @Override
    public String getId() {
        return Cosmetics.TRAIL_NOTES;
    }

    @Override
    public void draw(TrailContext context) {
        World world = context.world();
        if (world == null) return;
        // every other step: a note is bigger than a spark and ten a second is a wall of them
        if (ThreadLocalRandom.current().nextBoolean()) return;
        world.spawnParticle(Particle.NOTE, context.feet().add(0.0d, 1.0d, 0.0d), 0,
                ThreadLocalRandom.current().nextDouble(), 0.0d, 0.0d, 1.0d);
    }
}
