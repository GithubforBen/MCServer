package de.schnorrenbergers.survival.featrues.animations;

import org.bukkit.Location;
import org.bukkit.Particle;

public class ParticleLine {
    private Location start;
    private Location end;
    private Particle particle;
    private double stepsDistance;

    public ParticleLine(Location start, Location end, Particle particle, double steps) {
        this.start = start;
        this.end = end;
        this.particle = particle;
        this.stepsDistance = steps;
    }

    public void drawParticleLine() {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        // Compute total distance
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Normalize direction vector, calculate steps
        int steps = (int) Math.ceil(distance / stepsDistance);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        // Spawn particles along the line
        for (int i = 0; i <= steps; i++) {
            double x = start.getX() + stepX * i;
            double y = start.getY() + stepY * i;
            double z = start.getZ() + stepZ * i;
            start.getWorld().spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}
