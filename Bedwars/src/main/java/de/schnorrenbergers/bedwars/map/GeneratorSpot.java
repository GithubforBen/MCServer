package de.schnorrenbergers.bedwars.map;

import java.util.Locale;

/**
 * A generator that belongs to nobody - the diamonds and emeralds out in the middle.
 * <p>
 * The type is a plain string on purpose. Which types exist and how fast they run is decided by
 * {@code generators.yml} in phase 3, and a map that names a type this server does not know should be
 * reported as such rather than fail to load.
 */
public record GeneratorSpot(String type, MapPoint point) {

    public GeneratorSpot(String type, MapPoint point) {
        this.type = type == null ? "" : type.toUpperCase(Locale.ROOT);
        this.point = point;
    }
}
