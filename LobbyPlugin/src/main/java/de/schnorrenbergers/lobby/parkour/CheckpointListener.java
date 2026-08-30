package de.schnorrenbergers.lobby.parkour;

import de.schnorrenbergers.lobby.LobbyPlugin;
import de.schnorrenbergers.lobby.LobbyWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Watches where people walk, which is all a parkour needs.
 * <p>
 * Only whole blocks are looked at: a move event fires for every turn of the head, and running the course
 * check on all of them would be a few hundred pointless calls a second per player.
 */
public class CheckpointListener implements Listener {

    /** How far under the lowest block of the world a fall counts as off the course. */
    private static final int FALL_MARGIN = 8;

    private final ParkourService parkour;

    public CheckpointListener(LobbyPlugin plugin, ParkourService parkour) {
        this.parkour = parkour;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        if (LobbyWorld.get() != null && !to.getWorld().equals(LobbyWorld.get())) return;

        // a runner who has fallen out of the world goes back to their checkpoint rather than to spawn,
        // which is the difference between a parkour and a long walk back
        if (to.getY() < to.getWorld().getMinHeight() + FALL_MARGIN
                && parkour.returnToCheckpoint(event.getPlayer())) {
            return;
        }
        parkour.onMove(event.getPlayer(), to);
    }

    /**
     * A teleport out of the lobby ends a run. Warping to another server in the middle of one and coming
     * back to a clock that has been going ever since is not a time anybody ran.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        if (LobbyWorld.get() == null || event.getTo().getWorld().equals(LobbyWorld.get())) return;
        parkour.quit(event.getPlayer(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        parkour.quit(event.getPlayer(), false);
    }
}
