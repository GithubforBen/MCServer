package de.schnorrenbergers.bedwars.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A spot in a map, without a world.
 * <p>
 * Bukkit's own {@link Location} carries the world it belongs to, and a map is written down long before the
 * world it will be played in exists - the arena is a copy made when the round starts, under a different
 * name. Storing plain numbers and binding them to a world on load is the only way that survives the copy.
 */
public record MapPoint(double x, double y, double z, float yaw, float pitch) {

    /**
     * @param location where somebody is standing
     * @return that spot, without its world
     */
    public static MapPoint of(Location location) {
        return new MapPoint(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    /**
     * @param location a block
     * @return its centre, so a bed or a generator sits in the middle rather than in a corner
     */
    public static MapPoint ofBlock(Location location) {
        return new MapPoint(location.getBlockX() + 0.5d, location.getBlockY(), location.getBlockZ() + 0.5d,
                0f, 0f);
    }

    /**
     * @param world the world of the round
     * @return this spot in that world
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * @param section the block in the map file
     * @param path    the key underneath it
     * @return the spot written there, or {@code null} when nothing is
     */
    public static MapPoint read(ConfigurationSection section, String path) {
        ConfigurationSection point = section.getConfigurationSection(path);
        if (point == null) return null;
        return new MapPoint(
                point.getDouble("x"),
                point.getDouble("y"),
                point.getDouble("z"),
                (float) point.getDouble("yaw"),
                (float) point.getDouble("pitch"));
    }

    /**
     * @param section the block in the map file
     * @param path    the key to write it under
     */
    public void write(ConfigurationSection section, String path) {
        ConfigurationSection point = section.createSection(path);
        point.set("x", round(x));
        point.set("y", round(y));
        point.set("z", round(z));
        if (yaw != 0f) point.set("yaw", round(yaw));
        if (pitch != 0f) point.set("pitch", round(pitch));
    }

    /**
     * @param value a coordinate
     * @return it with two decimals, so map files stay readable
     */
    private static double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    @Override
    public String toString() {
        return round(x) + " / " + round(y) + " / " + round(z);
    }
}
