package de.schnorrenbergers.lobby;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Nothing hurts anybody in the lobby.
 * <p>
 * The lobby is a waiting room with a parkour in it, not a place anything is at stake. Falling off the
 * course, standing in the rain or forgetting to eat should cost nothing at all - and up to now every one
 * of them cost health, because nothing here ever said otherwise.
 * <p>
 * A player in creative is left alone: that is somebody building the map, and taking their fall damage
 * away is not what they are asking for either way.
 */
public class LobbyProtectionListener implements Listener {

    /** How far under the world a player is caught and put back, in blocks below the lowest one. */
    private static final int VOID_MARGIN = 16;

    public LobbyProtectionListener(LobbyPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Runs last and ignores what is already cancelled, so it has the final say without overruling a
     * plugin that had a reason of its own.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        event.setCancelled(true);
        // the fall is over as far as the player is concerned, so the counter has to agree - otherwise the
        // next landing adds this one on top of it
        player.setFallDistance(0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        event.setCancelled(true);
    }

    /**
     * Catches anybody who has fallen off the map and puts them back at the spawn.
     * <p>
     * Cancelling the damage alone is not enough: without this they keep falling for ever, out of reach of
     * the parkour and of everybody else, with nothing to do but log off.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to.getY() > to.getWorld().getMinHeight() - VOID_MARGIN) return;
        Location spawn = LobbyWorld.spawn();
        if (spawn == null) return;
        event.getPlayer().setFallDistance(0f);
        event.getPlayer().teleport(spawn);
    }
}
