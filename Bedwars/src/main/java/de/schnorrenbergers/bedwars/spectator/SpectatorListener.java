package de.schnorrenbergers.bedwars.spectator;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * What somebody who is out may still do, which is watch.
 * <p>
 * Most of it is free: minecraft's own spectator mode already makes them invisible, lets them fly and keeps
 * them from touching anything. What is not free is the moment in between - a player who has just died and
 * is on their way back is not a spectator to minecraft, but to the round they are, and until they are
 * standing again they must not pick anything up or hit anybody.
 * <p>
 * The rules here therefore hang on the round's own idea of who is playing, not on the game mode.
 */
public class SpectatorListener implements Listener {

    public SpectatorListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (watching(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (watching(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (watching(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (watching(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (watching(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (watching(event.getPlayer())) event.setCancelled(true);
    }

    /**
     * Keeps somebody who is out of the round out of the fighting, in both directions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && watching(victim)) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Player attacker && watching(attacker)) {
            event.setCancelled(true);
        }
    }

    /**
     * @param player somebody on the server
     * @return whether the round considers them a watcher right now - out for good, on their way back, or
     *         never part of it - while leaving operators in creative alone
     */
    private static boolean watching(@Nullable Player player) {
        Game game = game();
        if (player == null || game == null || !game.isRunning()) return false;
        if (game.isSetupMode() || player.getGameMode() == GameMode.CREATIVE) return false;
        GamePlayer participant = game.get(player);
        return participant == null || !participant.isAlive();
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}
