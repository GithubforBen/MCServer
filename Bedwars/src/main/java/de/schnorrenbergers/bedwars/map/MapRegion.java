package de.schnorrenbergers.bedwars.map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * A box of blocks in a map, written down as two corners.
 * <p>
 * There is exactly one of these per map so far - the waiting platform that hangs over the arena and has to
 * be gone once the round starts. It is a box rather than a radius because a platform is a rectangle, and a
 * sphere around its middle either leaves the corners standing or eats into the map underneath it.
 */
public record MapRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /**
     * @param section the block in the map file
     * @param path    the key underneath it
     * @return the box written there, or {@code null} when the map does not have one
     */
    public static @Nullable MapRegion read(ConfigurationSection section, String path) {
        ConfigurationSection region = section.getConfigurationSection(path);
        if (region == null) return null;
        ConfigurationSection min = region.getConfigurationSection("min");
        ConfigurationSection max = region.getConfigurationSection("max");
        if (min == null || max == null) return null;
        return new MapRegion(
                Math.min(min.getInt("x"), max.getInt("x")),
                Math.min(min.getInt("y"), max.getInt("y")),
                Math.min(min.getInt("z"), max.getInt("z")),
                Math.max(min.getInt("x"), max.getInt("x")),
                Math.max(min.getInt("y"), max.getInt("y")),
                Math.max(min.getInt("z"), max.getInt("z")));
    }

    /**
     * @param section the block in the map file
     * @param path    the key to write it under
     */
    public void write(ConfigurationSection section, String path) {
        ConfigurationSection region = section.createSection(path);
        ConfigurationSection min = region.createSection("min");
        min.set("x", minX);
        min.set("y", minY);
        min.set("z", minZ);
        ConfigurationSection max = region.createSection("max");
        max.set("x", maxX);
        max.set("y", maxY);
        max.set("z", maxZ);
    }

    /**
     * Takes every block of the box out of the world.
     * <p>
     * Without physics and without light updates: this runs in one tick on a box that can hold tens of
     * thousands of blocks, and a platform that is removed with physics on drops every torch and every
     * fence it was carrying as an item first.
     *
     * @param world where the box is
     * @return how many blocks were removed
     */
    public int clear(World world) {
        int removed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = Math.max(minY, world.getMinHeight()); y <= Math.min(maxY, world.getMaxHeight() - 1); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) continue;
                    block.setType(Material.AIR, false);
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * @param x a block
     * @param y a block
     * @param z a block
     * @return whether it is inside the box
     */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
