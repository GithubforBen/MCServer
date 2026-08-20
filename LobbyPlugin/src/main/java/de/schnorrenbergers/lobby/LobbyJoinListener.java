package de.schnorrenbergers.lobby;

import de.hems.paper.event.AwardService;
import de.hems.paper.event.EventAnnouncer;
import de.hems.paper.event.RunQueue;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Greets a player in the lobby with what the events are doing. The lobby is where most people land first,
 * so it is the place the calendar has to be one click away.
 */
public class LobbyJoinListener implements Listener {

    private static boolean registered = false;

    public LobbyJoinListener() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, LobbyPlugin.getInstance());
        registered = true;
    }

    /**
     * A queue only makes sense while everyone in it is online, so somebody logging off leaves it.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RunQueue.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // one lobby world, one spawn - anybody standing anywhere else got there by accident
        LobbyWorld.place(event.getPlayer());
        // a tick later, so the message does not get lost above the server's own greeting
        Bukkit.getScheduler().runTaskLater(LobbyPlugin.getInstance(), () -> {
            EventAnnouncer.sendJoinMessage(event.getPlayer());
            // no economy in the lobby, so a prize hands over its items here and its money on survival
            AwardService.deliverAsync(event.getPlayer());
        }, 20L);
    }
}
