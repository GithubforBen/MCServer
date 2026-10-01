package de.schnorrenbergers.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nothing hurts anybody in the lobby.
 * <p>
 * The lobby is a waiting room with a parkour in it, not a place anything is at stake. Falling off the
 * course, standing in the rain or forgetting to eat should cost nothing at all - and up to now every one
 * of them cost health, because nothing here ever said otherwise.
 * <p>
 * A player in creative is left alone: that is somebody building the map, and taking their fall damage
 * away is not what they are asking for either way.
 * <p>
 * Nor does anybody change the map, unless they are in creative: no block broken or placed, no bucket, no
 * item frame, painting or armor stand touched, no farmland trampled, no flower pot or note block turned.
 * Nothing changes it by itself either - explosions, fire, decaying leaves and melting ice are off. The
 * vanilla spawn protection does not do this: it leaves operators alone, and it ends 16 blocks from spawn.
 * <p>
 * A nether portal in the lobby leads to survival.
 */
public class LobbyProtectionListener implements Listener {

    /** How far under the world a player is caught and put back, in blocks below the lowest one. */
    private static final int VOID_MARGIN = 16;
    /** Where a nether portal in the lobby leads. */
    private static final String PORTAL_TARGET = "SURVIVAL";
    /** A player stands in the portal for a while; one warp per this many milliseconds is enough. */
    private static final long PORTAL_COOLDOWN_MILLIS = 5000L;

    /** Blocks a right click changes - turned, filled, eaten or tuned - which is a change to the map. */
    private static final Set<Material> CHANGED_BY_CLICK = EnumSet.of(
            Material.NOTE_BLOCK, Material.REPEATER, Material.COMPARATOR, Material.DAYLIGHT_DETECTOR,
            Material.COMPOSTER, Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON,
            Material.POWDER_SNOW_CAULDRON, Material.JUKEBOX, Material.LECTERN, Material.CHISELED_BOOKSHELF,
            Material.RESPAWN_ANCHOR, Material.BEEHIVE, Material.BEE_NEST, Material.SWEET_BERRY_BUSH,
            Material.CAVE_VINES, Material.CAVE_VINES_PLANT, Material.DECORATED_POT, Material.VAULT);

    private final Map<UUID, Long> lastPortal = new ConcurrentHashMap<>();

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

    /* ------------------------------------------------------------------ the map stays as it is */

    private static boolean builds(Player player) {
        return player != null && player.getGameMode() == GameMode.CREATIVE;
    }

    private static boolean builds(Entity entity) {
        return entity instanceof Player player && builds(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    /** Farmland under feet, and the blocks a right click turns into something else. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (builds(event.getPlayer())) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (event.getAction() == Action.PHYSICAL) {
            if (type == Material.FARMLAND || type == Material.TURTLE_EGG || type == Material.SNIFFER_EGG) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (CHANGED_BY_CLICK.contains(type) || Tag.FLOWER_POTS.isTagged(type) || Tag.CANDLES.isTagged(type)
                || Tag.CANDLE_CAKES.isTagged(type) || type == Material.CAKE || Tag.CAMPFIRES.isTagged(type)
                || Tag.ALL_SIGNS.isTagged(type)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
        // a spawn egg, flint and steel or bone meal would change the map from the item side
        if (event.getItem() != null) {
            Material item = event.getItem().getType();
            if (item == Material.FLINT_AND_STEEL || item == Material.FIRE_CHARGE || item == Material.BONE_MEAL
                    || item.name().endsWith("_SPAWN_EGG") || item == Material.ARMOR_STAND
                    || item == Material.END_CRYSTAL || item.name().endsWith("_BOAT")
                    || item.name().endsWith("_RAFT") || item.name().endsWith("MINECART")) {
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            }
        }
    }

    /** Item frames turned or emptied, armor stands and paintings - the entities that are part of the map. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (builds(event.getPlayer())) return;
        if (event.getRightClicked() instanceof org.bukkit.entity.Hanging) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) return;
        Entity damager = event.getDamager();
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        if (!builds(damager)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event instanceof HangingBreakByEntityEvent byEntity) {
            Entity remover = byEntity.getRemover();
            if (remover instanceof org.bukkit.entity.Projectile projectile
                    && projectile.getShooter() instanceof Entity shooter) {
                remover = shooter;
            }
            if (builds(remover)) return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    /** Endermen, falling blocks onto farmland, a sheep eating grass, and somebody trampling crops. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (builds(event.getEntity())) return;
        // sand and gravel that fall land again as the same block - cancelling that would leave them hanging
        if (event.getEntity() instanceof org.bukkit.entity.FallingBlock) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    /** Fire, grass, mushrooms and vines that creep - the map is built the way it is meant to look. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    /** Ice and snow melting, coral dying, fire going out on its own is fine. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.FIRE || event.getBlock().getType() == Material.SOUL_FIRE) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    /* ------------------------------------------------------------------ the portal to survival */

    /**
     * A nether portal in the lobby is the way to survival - the lobby has no nether, and a portal that
     * leads somewhere is a nicer door than an NPC.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = lastPortal.get(player.getUniqueId());
        if (last != null && now - last < PORTAL_COOLDOWN_MILLIS) return;
        lastPortal.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text("Auf nach Survival …", NamedTextColor.GREEN));
        // the same way the NPCs and /warp go: it knows about servers that are still starting
        player.performCommand("warp " + PORTAL_TARGET);
    }

    /** Items and mobs going through stay here; there is nowhere for them to arrive. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        event.setCancelled(true);
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
