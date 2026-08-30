package de.schnorrenbergers.lobby.parkour;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * A spot on a course, with the ball of air around it that counts as "reached".
 * <p>
 * A radius rather than a block, because a checkpoint that only counts when a player stands in one exact
 * block is a checkpoint that is missed at a run. It is also what keeps a course independent of the blocks
 * it is built from: a plate somebody accidentally breaks does not take the course with it.
 *
 * @param x      where it is
 * @param y      where it is
 * @param z      where it is
 * @param yaw    which way a player is turned when they are put here
 * @param pitch  and how far up or down they look
 * @param radius how close counts as reached, in blocks
 */
public record ParkourPoint(double x, double y, double z, float yaw, float pitch, double radius) {

    /** How close a player has to be when a course does not say. */
    public static final double DEFAULT_RADIUS = 1.5d;

    /**
     * @param location where somebody is standing
     * @param radius   how close counts
     * @return that spot, without its world
     */
    public static ParkourPoint of(Location location, double radius) {
        return new ParkourPoint(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), radius);
    }

    /**
     * @param world the lobby world
     * @return this spot in it
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * @param at where a player is
     * @return whether they are close enough for this to count
     */
    public boolean reachedFrom(Location at) {
        double dx = at.getX() - x;
        double dy = at.getY() - y;
        double dz = at.getZ() - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * @param section the block in the file
     * @param path    the key underneath it
     * @return the spot written there, or {@code null} when nothing is
     */
    public static @Nullable ParkourPoint read(ConfigurationSection section, String path) {
        ConfigurationSection point = section.getConfigurationSection(path);
        if (point == null) return null;
        return new ParkourPoint(
                point.getDouble("x"),
                point.getDouble("y"),
                point.getDouble("z"),
                (float) point.getDouble("yaw"),
                (float) point.getDouble("pitch"),
                Math.max(0.5d, point.getDouble("radius", DEFAULT_RADIUS)));
    }

    /**
     * @param section the block in the file
     * @param path    the key to write it under
     */
    public void write(ConfigurationSection section, String path) {
        ConfigurationSection point = section.createSection(path);
        point.set("x", round(x));
        point.set("y", round(y));
        point.set("z", round(z));
        if (yaw != 0f) point.set("yaw", round(yaw));
        if (pitch != 0f) point.set("pitch", round(pitch));
        if (radius != DEFAULT_RADIUS) point.set("radius", round(radius));
    }

    private static double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    @Override
    public String toString() {
        return round(x) + " / " + round(y) + " / " + round(z);
    }
}
