package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Everything a win effect needs to know about the round it is celebrating.
 * <p>
 * Deliberately not "the bedwars round": an effect that only knows a place, a height and who won can be
 * played by any game mode that later wants one, and none of them has to teach the effect what a bed is.
 *
 * @param plugin   the plugin the effect schedules its work on
 * @param winners  who won, at least one
 * @param center   the middle of the map
 * @param topY     the height the map is built up to, which is where things fall from
 * @param radius   how far the map reaches from the middle
 * @param cosmetic the cosmetic being played, for its settings
 */
public record WinContext(Plugin plugin, List<Player> winners, Location center, int topY, double radius,
                         CosmeticData cosmetic) {

    /**
     * @return the world the round was played in
     */
    public World world() {
        return center.getWorld();
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
