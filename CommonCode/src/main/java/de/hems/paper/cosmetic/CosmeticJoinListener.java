package de.hems.paper.cosmetic;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Fetches somebody's cosmetics when they walk in, and lets go of them again after they leave.
 * <p>
 * This is what replaced every server holding the ownership of every player who ever bought anything. The
 * request is one round trip in the background at the moment somebody joins, which is minutes before
 * anything they own can matter - the first thing a cosmetic is asked about is a kill or the end of a
 * round, and neither happens in the tick after a login.
 */
public class CosmeticJoinListener implements Listener {

    public CosmeticJoinListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        CosmeticService.loadPlayerAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        CosmeticService.forgetLater(event.getPlayer().getUniqueId());
    }
}
