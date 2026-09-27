package de.hems.paper.cosmetic;

import de.hems.paper.PaperContext;
import org.bukkit.entity.Player;
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
 * <p>
 * The one thing that does happen right after a login is the gadget being handed out on the lobby and on
 * survival. That waits for the answer here: handed out a tick after the join, it went to somebody whose
 * cosmetics had not arrived yet, and they walked in with nothing.
 */
public class CosmeticJoinListener implements Listener {

    public CosmeticJoinListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PaperContext.async(() -> {
            if (CosmeticService.loadPlayerBlocking(player.getUniqueId()) == null) return;
            // on a bedwars server the guard says no until the round starts, and the round hands out itself
            PaperContext.sync(() -> {
                if (player.isOnline()) Gadgets.handOut(player, true);
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        CosmeticService.forgetLater(event.getPlayer().getUniqueId());
    }
}
