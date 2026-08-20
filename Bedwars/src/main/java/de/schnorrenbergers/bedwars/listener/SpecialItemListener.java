package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.ShopSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The shop items that are more than the item they are made of.
 * <p>
 * What each of them does hangs off the material rather than off the id in {@code shop.yml}: a server that
 * renames "fireball" to "boom" or prices it differently still sells a fire charge, and a fire charge in
 * bedwars is thrown rather than used to light a furnace. The numbers - the price, how long a summoned mob
 * lives - do come out of the entry, which is why both are looked up here.
 */
public class SpecialItemListener implements Listener {

    /** How long a placed block of tnt waits before it goes off. */
    private static final int TNT_FUSE_TICKS = 40;
    /** How hard a fireball hits. */
    private static final float FIREBALL_YIELD = 2.5f;
    /** How far a summoned mob looks for somebody to attack. */
    private static final double MINION_RANGE = 12.0d;

    private final Plugin plugin;
    /** Summoned mob to the team that paid for it. */
    private final Map<UUID, GameTeam> minions = new HashMap<>();
    /** Who threw an ender pearl when, so that a pearl is an escape and not a way of travelling. */
    private final Map<UUID, Long> lastPearl = new HashMap<>();

    public SpecialItemListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ----------------------------------------------------------------------- tnt

    /**
     * Lights tnt the moment it is placed.
     * <p>
     * Runs after the build rules rather than before them: tnt that may not be placed there must not go off
     * there either, and letting the block land first is the only way to know that it was allowed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        if (event.getBlock().getType() != Material.TNT) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.TNT) return;
            game.getBlockTracker().forget(block);
            block.setType(Material.AIR);
            Location at = block.getLocation().add(0.5d, 0.0d, 0.5d);
            TNTPrimed tnt = block.getWorld().spawn(at, TNTPrimed.class);
            tnt.setFuseTicks(TNT_FUSE_TICKS);
            tnt.setSource(player);
        });
    }

    // ------------------------------------------------------------------ in hand

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Game game = game();
        ItemStack held = event.getItem();
        if (game == null || !game.isRunning() || held == null) return;
        GamePlayer user = game.get(event.getPlayer());
        if (user == null || !user.isAlive()) return;

        if (held.getType() == Material.ENDER_PEARL && !mayPearl(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (held.getType() == Material.FIRE_CHARGE) {
            event.setCancelled(true);
            throwFireball(event.getPlayer());
            consume(event.getPlayer(), held);
            return;
        }
        if (held.getType().name().endsWith("_SPAWN_EGG") && ShopItems.idOf(held) != null) {
            event.setCancelled(true);
            if (summon(user, event.getPlayer(), held, event.getClickedBlock())) {
                consume(event.getPlayer(), held);
            }
        }
    }

    /**
     * Keeps a player from chaining ender pearls.
     * <p>
     * The throw is stopped rather than the pearl: cancelling the flight would take the pearl and give
     * nothing back for it, which is a worse punishment than the cooldown itself.
     *
     * @param player who is trying to throw
     * @return whether they may
     */
    private boolean mayPearl(Player player) {
        int seconds = Bedwars.getInstance().getGameSettings().getEnderPearlCooldownSeconds();
        if (seconds <= 0) return true;
        long now = player.getWorld().getFullTime();
        Long last = lastPearl.get(player.getUniqueId());
        if (last != null && now - last < seconds * 20L) {
            Messages.send(player, "item.pearl-cooldown",
                    "seconds", String.valueOf(Math.max(1, (int) Math.ceil((seconds * 20L - (now - last)) / 20.0d))));
            return false;
        }
        lastPearl.put(player.getUniqueId(), now);
        return true;
    }

    /**
     * Throws a fireball the way hypixel's is thrown: straight, fast and without setting the map on fire.
     */
    private void throwFireball(Player player) {
        Fireball fireball = player.launchProjectile(Fireball.class,
                player.getEyeLocation().getDirection().multiply(0.6d));
        fireball.setShooter(player);
        fireball.setYield(FIREBALL_YIELD);
        fireball.setIsIncendiary(false);
    }

    /**
     * Drinking magic milk is what makes traps ignore somebody for a while.
     * <p>
     * The vanilla drink is cancelled on purpose: milk clears every potion effect, and a player who paid for
     * trap immunity should not lose their speed potion for it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        if (event.getItem().getType() != Material.MILK_BUCKET) return;
        GamePlayer drinker = game.get(event.getPlayer());
        if (drinker == null || !drinker.isAlive()) return;

        event.setCancelled(true);
        consume(event.getPlayer(), event.getItem());
        int seconds = Bedwars.getInstance().getUpgradeSettings().getTrapImmunitySeconds();
        long now = game.getLoop() == null ? 0L : game.getLoop().getTicks();
        drinker.getLoadout().setTrapImmuneUntil(now + seconds * 20L);
        Messages.send(event.getPlayer(), "item.magic-milk", "seconds", String.valueOf(seconds));
    }

    // ------------------------------------------------------------------- minions

    /**
     * Summons what a spawn egg stands for, on the team of whoever used it.
     *
     * @param user    who used it
     * @param player  the same player, for where it lands when they clicked no block
     * @param egg     the spawn egg
     * @param clicked the block it was used on, or {@code null} when it was used in the air
     * @return whether something was summoned
     */
    private boolean summon(GamePlayer user, Player player, ItemStack egg, @Nullable Block clicked) {
        EntityType type = typeOf(egg.getType());
        Location at = clicked == null
                ? player.getLocation()
                : clicked.getRelative(BlockFace.UP).getLocation().add(0.5d, 0.0d, 0.5d);
        if (type == null || at.getWorld() == null) return false;

        Entity summoned = at.getWorld().spawnEntity(at, type);
        if (!(summoned instanceof Mob mob)) {
            summoned.remove();
            return false;
        }
        GameTeam team = user.getTeam();
        if (team != null) {
            mob.customName(Messages.get("item.minion-name",
                    "team", team.getColor().getDisplayName(),
                    "player", user.getName()));
            mob.setCustomNameVisible(true);
            minions.put(mob.getUniqueId(), team);
        }
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(false);
        watch(mob, team, lifetimeOf(egg));
        return true;
    }

    /**
     * Keeps a summoned mob pointed at the enemy and takes it away when its time is up.
     *
     * @param mob      what was summoned
     * @param team     whose side it is on
     * @param lifetime how many seconds it stays, 0 for as long as it survives
     */
    private void watch(Mob mob, @Nullable GameTeam team, int lifetime) {
        new BukkitRunnable() {
            private int lived;

            @Override
            public void run() {
                if (!mob.isValid()) {
                    minions.remove(mob.getUniqueId());
                    cancel();
                    return;
                }
                if (lifetime > 0 && ++lived >= lifetime) {
                    minions.remove(mob.getUniqueId());
                    mob.remove();
                    cancel();
                    return;
                }
                Player enemy = nearestEnemy(mob, team);
                if (enemy != null) mob.setTarget(enemy);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * @return the closest player a summoned mob may attack, or {@code null} when there is none nearby
     */
    private @Nullable Player nearestEnemy(Mob mob, @Nullable GameTeam team) {
        Game game = game();
        if (game == null) return null;
        Player closest = null;
        double distance = MINION_RANGE * MINION_RANGE;
        for (GamePlayer participant : game.getOnlinePlayers()) {
            if (!participant.isAlive() || (team != null && team.contains(participant))) continue;
            Player player = participant.getPlayer();
            if (player == null || !player.getWorld().equals(mob.getWorld())) continue;
            double squared = player.getLocation().distanceSquared(mob.getLocation());
            if (squared > distance) continue;
            closest = player;
            distance = squared;
        }
        return closest;
    }

    /**
     * Keeps a summoned mob from turning on the team that paid for it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        GameTeam team = minions.get(event.getEntity().getUniqueId());
        if (team == null || !(event.getTarget() instanceof Player player)) return;
        Game game = game();
        GamePlayer targeted = game == null ? null : game.get(player);
        if (targeted != null && team.contains(targeted)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMinionHit(EntityDamageByEntityEvent event) {
        GameTeam team = minions.get(event.getDamager().getUniqueId());
        if (team == null || !(event.getEntity() instanceof Player player)) return;
        Game game = game();
        GamePlayer victim = game == null ? null : game.get(player);
        if (victim != null && team.contains(victim)) event.setCancelled(true);
    }

    // ------------------------------------------------------------------- helpers

    /**
     * @param egg the spawn egg that was used
     * @return how long what it summons stays, out of the shop entry it was bought as
     */
    private int lifetimeOf(ItemStack egg) {
        ShopSettings settings = Bedwars.getInstance().getShopSettings();
        ShopItem item = settings.get(ShopItems.idOf(egg));
        return item == null ? 0 : item.lifetime();
    }

    /**
     * @param egg a spawn egg
     * @return what it summons, or {@code null} when this server does not know that entity
     */
    private static @Nullable EntityType typeOf(Material egg) {
        String name = egg.name().replace("_SPAWN_EGG", "");
        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Takes one off the stack in the player's hand.
     */
    private static void consume(Player player, ItemStack held) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        held.setAmount(held.getAmount() - 1);
    }

    private static @Nullable Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }

    /**
     * @param entity anything
     * @return whether it was summoned by a player, so that other listeners can leave it alone
     */
    public boolean isMinion(@Nullable Entity entity) {
        return entity instanceof LivingEntity && minions.containsKey(entity.getUniqueId());
    }
}
