package de.schnorrenbergers.bedwars.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Generates nothing at all.
 * <p>
 * A bedwars map is a handful of islands in the void, and the world it is copied into only holds the chunks
 * somebody actually built. Without this, everything around them would be filled in with ordinary terrain
 * the moment a player walked or was knocked far enough - islands with a landscape behind them.
 * <p>
 * Saying "do not generate" rather than "generate emptiness" is the cheaper half of the deal: the server
 * skips the work instead of doing it and throwing the result away.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    /**
     * @return a spot high in the air, so a server that falls back to the world spawn does not drop anybody
     *         into the void at y=0
     */
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5d, 100.0d, 0.5d);
    }
}
