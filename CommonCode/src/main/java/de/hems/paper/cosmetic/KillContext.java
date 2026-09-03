package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Everything a kill effect needs to know about the kill it is celebrating.
 * <p>
 * Deliberately not "the bedwars kill": an effect that knows who did it, who it happened to and where can
 * be played by any game mode, and none of them has to teach the effect what a final kill is.
 *
 * @param plugin   the plugin the effect schedules its work on
 * @param killer   whoever did it
 * @param victim   whoever it happened to, still where they fell
 * @param where    where they fell
 * @param cosmetic the cosmetic being played, for its settings
 */
public record KillContext(Plugin plugin, Player killer, Player victim, Location where,
                          CosmeticData cosmetic) {

    /**
     * @return the world it happened in
     */
    public World world() {
        return where.getWorld();
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
