package de.schnorrenbergers.lobby.cosmetic;

import de.hems.paper.cosmetic.Gadgets;
import de.hems.types.cosmetic.GadgetSlot;
import de.schnorrenbergers.lobby.LobbyWorld;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * The gadgets, as far as the lobby is concerned.
 * <p>
 * What a gadget <em>does</em> is not here and must not be: a double jump is the same double jump on every
 * server of this network, so it lives with the rest of the cosmetics. What is the lobby's own is exactly
 * two answers - who counts as standing in the lobby, and when they are handed what they are wearing.
 * <p>
 * The lobby world and nothing else. Somebody watching a parkour run in spectator is not standing here in
 * any sense that matters, and a player who has already been sent on to another world is on their way out.
 */
public class GadgetListener implements Listener {

    private final Plugin plugin;

    public GadgetListener(Plugin plugin) {
        this.plugin = plugin;
        Gadgets.setGuard(GadgetListener::inLobby, GadgetSlot.LOBBY);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // a reload with people online: they are already standing here and would otherwise wait for their
        // next join for something they are wearing right now
        for (Player online : plugin.getServer().getOnlinePlayers()) handOut(online, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // a tick later: joining still moves people to the spawn and sorts out their inventory, and an
        // item given before that is an item given to an inventory that is about to be cleared
        handOut(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        handOut(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        handOut(event.getPlayer(), false);
    }

    private void handOut(Player player, boolean announce) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) Gadgets.handOut(player, announce);
        });
    }

    /**
     * @param player somebody
     * @return whether they are standing in the lobby rather than watching it or on their way elsewhere
     */
    private static boolean inLobby(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        World lobby = LobbyWorld.get();
        return lobby != null && lobby.equals(player.getWorld());
    }
}
