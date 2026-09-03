package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The trails, and the one loop that draws them.
 * <p>
 * Trails are the only cosmetic that runs while the game is being played rather than at a moment that is
 * already over, so this loop is written to do nothing most of the time: it draws for a player who moved
 * and for nobody else, which on a server full of people standing in a lobby is no work at all.
 * <p>
 * Two people never see a trail: whoever is spectating, and whoever drank an invisibility potion. The
 * second one matters - a bought cosmetic that gives away an invisible player would be a cosmetic nobody
 * dares put on.
 */
public final class Trails {

    /** How often the trails are drawn, in ticks. Ten times a second is smooth enough to read as a line. */
    private static final int INTERVAL = 2;
    /** How far somebody has to have moved since the last step to get a particle, in blocks squared. */
    private static final double MOVED = 0.02d;

    private static final Map<String, TrailEffect> effects = new LinkedHashMap<>();
    /** Where everybody stood at the last step, so a player standing still draws nothing. */
    private static final Map<UUID, Location> lastSeen = new ConcurrentHashMap<>();
    private static boolean running;

    private Trails() {
    }

    /**
     * @param effect a trail this server can draw
     */
    public static void register(TrailEffect effect) {
        if (effect != null) effects.put(key(effect.getId()), effect);
    }

    /**
     * Starts the loop that draws them. Does nothing the second time it is called.
     *
     * @param plugin the plugin it runs on
     */
    public static synchronized void start(Plugin plugin) {
        if (running || plugin == null) return;
        running = true;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> drawAll(plugin), INTERVAL, INTERVAL);
    }

    private static void drawAll(Plugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location at = player.getLocation();
            Location previous = lastSeen.put(player.getUniqueId(), at.clone());
            if (!draws(player)) continue;
            if (previous == null || previous.getWorld() != at.getWorld()) continue;
            if (previous.distanceSquared(at) < MOVED) continue;

            CosmeticData chosen = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.TRAIL);
            if (chosen == null) continue;
            TrailEffect effect = effects.get(key(chosen.getId()));
            if (effect == null) continue;
            try {
                effect.draw(new TrailContext(plugin, player, previous, at, chosen));
            } catch (Exception e) {
                Bukkit.getLogger().warning("The trail " + chosen.getId() + " failed: " + e.getMessage());
            }
        }
        // whoever left is not walking anywhere, and a location per player who ever logged in is a leak
        lastSeen.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    /**
     * @param player somebody online
     * @return whether a trail behind them is a good idea right now
     */
    private static boolean draws(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        // an invisible player who leaves a line of flames behind them is not invisible
        return !player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    /**
     * @return the ids this server can draw
     */
    public static List<String> registered() {
        return List.copyOf(effects.keySet());
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}
