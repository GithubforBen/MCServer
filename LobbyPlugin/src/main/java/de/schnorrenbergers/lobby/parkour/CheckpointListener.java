package de.schnorrenbergers.lobby.parkour;

import de.schnorrenbergers.lobby.LobbyPlugin;
import de.schnorrenbergers.lobby.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

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
        parkour.abandon(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        parkour.abandon(event.getPlayer());
    }

    /**
     * The three items a runner carries. Cancelled whatever they are: the barrier and the clock are a block
     * and a usable item, and a right click that is not caught here places or uses one of them.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        ParkourItems.Kind kind = ParkourItems.kindOf(event.getItem());
        if (kind == null) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        // right click only, though a left click is still swallowed above: a runner swinging at the air
        // should not find themselves back at their last checkpoint for it
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        switch (kind) {
            case CHECKPOINT -> {
                if (!parkour.returnToCheckpoint(player)) {
                    player.sendActionBar(Component.text("Du läufst gerade keine Strecke.",
                            NamedTextColor.GRAY));
                }
            }
            case RESTART -> parkour.restart(player);
            case QUIT -> parkour.quit(player);
        }
    }

    /**
     * They cannot be dropped: an item lying in the lobby is one somebody else picks up, and a runner who
     * threw their own way out of the course away is stuck in it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (ParkourItems.kindOf(event.getItemDrop().getItemStack()) == null) return;
        event.setCancelled(true);
    }
}
