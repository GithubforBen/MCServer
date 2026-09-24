package de.schnorrenbergers.hungergames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The supply packages: a chest that falls out of the sky somewhere inside the border, with the best loot
 * of the game in it.
 * <p>
 * Where it comes down is announced with coordinates and marked with a column of light, because the point of
 * a supply drop is that people have to leave cover to get it - and that the others know where they went.
 * <p>
 * It lands inside the border as it is at that moment, and never in the outer fifth of it, so a drop is
 * never a trap that the border takes away before anybody gets there.
 */
public final class SupplyDrops implements Listener {

    /** How high above the ground the package starts falling. */
    private static final int FALL_HEIGHT = 40;
    /** How long a package may fall before it is simply put down. */
    private static final long FALLBACK_TICKS = 20L * 20L;
    /** How long the column of light stays. */
    private static final int BEACON_SECONDS = 60;

    private final HungerGamesPlugin plugin;
    private final Game game;
    private final ArenaMap map;
    /** Falling packages, and where they were aimed. */
    private final Map<UUID, Location> falling = new HashMap<>();

    public SupplyDrops(HungerGamesPlugin plugin, Game game, ArenaMap map) {
        this.plugin = plugin;
        this.game = game;
        this.map = map;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Drops one package.
     */
    public void drop() {
        World world = map.getWorld();
        Location center = map.getWorld().getWorldBorder().getCenter();
        double radius = world.getWorldBorder().getSize() / 2.0 * 0.8;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = Math.sqrt(random.nextDouble()) * radius;
        int x = (int) Math.floor(center.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * distance);
        Location ground = ArenaMap.surface(world, x, z);

        Location start = ground.clone().add(0, FALL_HEIGHT, 0);
        FallingBlock block = world.spawn(start, FallingBlock.class, spawned -> {
            spawned.setBlockData(Material.CHEST.createBlockData());
            spawned.setDropItem(false);
            spawned.setHurtEntities(false);
            spawned.setGlowing(true);
        });
        falling.put(block.getUniqueId(), ground);

        Bukkit.getServer().sendMessage(Component.text("📦 Supply Drop bei X " + ground.getBlockX() + " / Z "
                + ground.getBlockZ() + "!", NamedTextColor.AQUA));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.6f);
        }

        // a package that lands on a torch or a slab breaks instead of turning into a chest, and one that
        // falls into an unloaded chunk may never arrive at all - so after a while it is simply put down
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location aimed = falling.remove(block.getUniqueId());
            if (aimed == null) return;
            block.remove();
            land(aimed.getBlock());
        }, FALLBACK_TICKS);
    }

    @EventHandler
    public void onLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        if (falling.remove(event.getEntity().getUniqueId()) == null) return;
        Block block = event.getBlock();
        // the falling block becomes the chest a moment from now; filling it has to wait until it is one
        Bukkit.getScheduler().runTask(plugin, () -> land(block));
    }

    @EventHandler
    public void onBreak(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        Location aimed = falling.remove(event.getEntity().getUniqueId());
        if (aimed == null) return;
        event.setCancelled(true);
        land(event.getEntity().getLocation().getBlock());
    }

    private void land(Block block) {
        if (block.getType() != Material.CHEST) block.setType(Material.CHEST);
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            Loot.fill(chest.getBlockInventory(), Loot.Tier.SUPPLY);
            game.loot(block.getLocation());
        }
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
        beacon(block.getLocation().add(0.5, 1, 0.5));
    }

    private void beacon(Location top) {
        BukkitTask[] task = new BukkitTask[1];
        int[] left = {BEACON_SECONDS};
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (--left[0] < 0 || top.getBlock().getRelative(0, -1, 0).getType() != Material.CHEST) {
                task[0].cancel();
                return;
            }
            for (int y = 0; y < 30; y += 2) {
                top.getWorld().spawnParticle(Particle.END_ROD, top.clone().add(0, y, 0), 2, 0.1, 0.5, 0.1, 0);
            }
        }, 0L, 20L);
    }
}
