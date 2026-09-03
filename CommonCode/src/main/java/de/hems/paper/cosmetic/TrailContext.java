package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * One step of a trail.
 * <p>
 * A trail is drawn where its owner has been, not where they are: {@link #from()} is where they stood at
 * the last step and {@link #at()} where they stand now, so an effect that wants a line between the two
 * has both without having to remember anything itself.
 *
 * @param plugin   the plugin it runs on
 * @param player   whoever is wearing it
 * @param from     where they were a moment ago
 * @param at       where they are now
 * @param cosmetic the cosmetic being drawn, for its settings
 */
public record TrailContext(Plugin plugin, Player player, Location from, Location at,
                           CosmeticData cosmetic) {

    /**
     * @return the world they are walking through
     */
    public World world() {
        return at.getWorld();
    }

    /**
     * @return where the particles belong: at their feet, which is where a footprint would be
     */
    public Location feet() {
        return at.clone().add(0.0d, 0.1d, 0.0d);
    }

    /**
     * @param key      a setting of the cosmetic
     * @param fallback what it is when nobody set it
     * @return the number behind it
     */
    public int setting(String key, int fallback) {
        return cosmetic == null ? fallback : cosmetic.getNumber(key, fallback);
    }
}
