package de.schnorrenbergers.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The rules of the arena that are enforced by saying no.
 * <p>
 * Before the start nobody can be hurt, get hungry, build or open a chest. During the grace period players
 * cannot hurt each other. The nether and the end are switched off on the server already; the portal events
 * are refused here too, so an arena that was set up by hand without that still has no way out.
 */
public final class GameListener implements Listener {

    private final HungerGamesPlugin plugin;
    private final Game game;
    /** Where somebody fell, so the spectator starts looking from there. */
    private final Map<UUID, Location> deathSpots = new HashMap<>();

    public GameListener(HungerGamesPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    private boolean before() {
        return game.getState() == Game.State.WAITING || game.getState() == Game.State.COUNTDOWN;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        game.join(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        game.quit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathSpots.put(player.getUniqueId(), player.getLocation());
        event.deathMessage(null);
        game.eliminate(player, player.getKiller(), null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location spot = deathSpots.remove(player.getUniqueId());
        if (spot != null) event.setRespawnLocation(spot.clone().add(0, 2, 0));
        if (game.isAlive(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (before() || game.getState() == Game.State.ENDED) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player player) attacker = player;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || attacker.equals(event.getEntity())) return;
        if (!game.isPvp()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (before()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!game.isFrozen() || !game.isAlive(event.getPlayer().getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        // looking around is fine, walking off the start place is not
        if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
            Location back = from.clone();
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            event.setTo(back);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (before() && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (before() && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (before()) event.setCancelled(true);
    }

    /**
     * Fills a chest the first time it is opened in the game, if it is empty. A chest the map builder
     * filled by hand keeps what they put in it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest) && !(holder instanceof DoubleChest)) return;
        if (before()) {
            if (!event.getPlayer().isOp()) event.setCancelled(true);
            return;
        }
        if (game.getState() != Game.State.RUNNING) return;
        Location location = event.getInventory().getLocation();
        if (location == null) return;
        location = location.getBlock().getLocation();
        if (!game.loot(location)) return;
        if (!event.getInventory().isEmpty()) return;
        Loot.Tier tier = game.getMap().isCornucopia(location) ? Loot.Tier.CORNUCOPIA : Loot.Tier.NORMAL;
        Loot.fill(event.getInventory(), tier);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        event.setCancelled(true);
    }
}
