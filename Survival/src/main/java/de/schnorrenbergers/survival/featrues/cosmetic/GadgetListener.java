package de.schnorrenbergers.survival.featrues.cosmetic;

import de.hems.paper.cosmetic.Gadgets;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * The gadgets, as far as survival is concerned.
 * <p>
 * What a gadget <em>does</em> lives with the rest of the cosmetics; what survival answers is who may use
 * one and when they get it. Everybody who is actually playing, in every world of this server - the end
 * and the nether are survival too, and a harvest helper that stops working at the nether portal would be
 * a bug nobody could explain.
 * <p>
 * Which gadgets those are is not decided here either: a gadget says itself whether it belongs on survival,
 * and the endless pearl says no. That is the line this server has to hold - a cosmetic may not become a
 * shortcut in a world people build in.
 */
public class GadgetListener implements Listener {

    private final Plugin plugin;

    public GadgetListener(Plugin plugin) {
        this.plugin = plugin;
        Gadgets.setGuard(GadgetListener::isPlaying, GadgetSlot.SURVIVAL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player online : plugin.getServer().getOnlinePlayers()) handOut(online, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        handOut(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // a gadget that is gone the first time its owner dies is a gadget they bought once
        handOut(event.getPlayer(), false);
    }

    private void handOut(Player player, boolean announce) {
        // a tick later, so the item is not put into an inventory the respawn is still sorting out
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) Gadgets.handOut(player, announce);
        });
    }

    /**
     * @param player somebody
     * @return whether they are playing rather than watching
     */
    private static boolean isPlaying(Player player) {
        return player.getGameMode() != GameMode.SPECTATOR;
    }
}
