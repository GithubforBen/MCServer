package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.utils.Inventorys;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopkeeperManager {
    private static List<Shopkeeper> shopkeepers;

    /** How often everything is written out, so a crash costs at most this much. */
    private static final long AUTOSAVE_TICKS = 20L * 60L * 5L;

    public ShopkeeperManager() {
        if (shopkeepers == null) {
            shopkeepers = new ArrayList<>();
            load();
            new ShopkeeperChunkListener();
            // shops whose chunk is already in get their villager now, the rest when their chunk loads
            ShopkeeperChunkListener.spawnInLoadedChunks();
            Bukkit.getScheduler().runTaskTimer(Survival.getInstance(),
                    ShopkeeperManager::saveAll, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        }
    }

    /**
     * Writes every shopkeeper into the config and flushes it to disk.
     * <p>
     * Writing the config object alone is not enough - nothing survived a crash before, because the file was
     * only ever written in {@code onDisable}.
     */
    public static void saveAll() {
        if (shopkeepers == null) return;
        shopkeepers.forEach(Shopkeeper::save);
        Survival.getInstance().getShopConfig().save();
    }

    /** Kept for the shutdown path, which also has to take the villagers back out of the world. */
    public static void save() {
        saveAll();
    }

    /**
     * Stores everything and removes the villagers, so the next start finds exactly one villager per shop.
     */
    public static void shutdown() {
        if (shopkeepers == null) return;
        saveAll();
        shopkeepers.forEach(Shopkeeper::despawn);
    }

    /**
     * @return every shopkeeper that is currently loaded, for the marketplace to collect offers from
     */
    public static List<Shopkeeper> getShopkeepers() {
        return shopkeepers == null ? List.of() : List.copyOf(shopkeepers);
    }

    /**
     * Shops by the chunk their chest sits in.
     * <p>
     * Chunks unload constantly, so the unload handler must not walk the whole shop list every time. The
     * index is thrown away whenever a chest moves or a shop is added.
     */
    private static Map<Long, List<Shopkeeper>> chestIndex;

    /** Drops the lookup, so it is rebuilt the next time a chunk unloads. */
    public static void invalidateChestIndex() {
        chestIndex = null;
    }

    private static Map<Long, List<Shopkeeper>> chestIndex() {
        Map<Long, List<Shopkeeper>> index = chestIndex;
        if (index != null) return index;
        index = new HashMap<>();
        for (Shopkeeper shopkeeper : getShopkeepers()) {
            Location chest = shopkeeper.getChest();
            if (chest == null || chest.getWorld() == null) continue;
            index.computeIfAbsent(Chunk.getChunkKey(chest), key -> new ArrayList<>()).add(shopkeeper);
        }
        chestIndex = index;
        return index;
    }

    /**
     * @param chunk the chunk being asked about
     * @return the shops whose chest stands in it
     */
    public static List<Shopkeeper> withChestInChunk(Chunk chunk) {
        List<Shopkeeper> candidates = chestIndex().get(chunk.getChunkKey());
        if (candidates == null) return List.of();
        List<Shopkeeper> result = new ArrayList<>();
        // the key ignores the world, so two worlds can share it - the world is checked here
        for (Shopkeeper shopkeeper : candidates) {
            if (shopkeeper.isChestInChunk(chunk)) result.add(shopkeeper);
        }
        return result;
    }

    public static Shopkeeper createShopkeeper(Player player, String name) {
        int money = MoneyHandler.getMoney(player.getUniqueId());
        if (money < 20 * 100) {
            player.sendMessage("You dont have enough money! You need 2000!");
            return null;
        }
        Location chest = new Location(player.getWorld(), player.getX(), player.getY() , player.getZ());
        if (chest.getBlock().getType() != Material.CHEST) {
            player.sendMessage("You need to be standing on a chest!");
            return null;
        }
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        if (playerTeam == null) {
            player.sendMessage("You dont have a team!");
            return null;
        }
        if (ClaimManager.getTeamOfChunk(player.getChunk()) == null) {
            player.sendMessage("You need to claim this chunk first!");
            return null;
        }
        if (!ClaimManager.getTeamOfChunk(player.getChunk()).equals(playerTeam.getName())) {
            player.sendMessage("You dont own this chunk!");
            return null;
        }
        Shopkeeper shopkeeper = new Shopkeeper(UUID.randomUUID(),
                name,
                player.getLocation(),
                chest,
                playerTeam.getName(),
                new ArrayList<>());
        shopkeepers.add(shopkeeper);
        invalidateChestIndex();
        // a new shop has to reach the disk right away, otherwise it is gone after the next crash
        saveAll();
        return shopkeeper;
    }

    public static void openManagerInventory(Player player, UUID uuid) {
        Shopkeeper shopkeeper = getShopkeeper(uuid);
        if (shopkeeper == null) {
            player.sendMessage("Shopkeeper not found! Report this to the server owner!");
            return;
        }
        player.openInventory(Inventorys.ADMIN_SHOPKEEPER_INVENTORY(
                shopkeeper
        ).getInventory());
    }

    public static Shopkeeper getShopkeeper(UUID uuid) {
        for (Shopkeeper shopkeeper : shopkeepers) {
            if (shopkeeper.getUuid().equals(uuid)) {
                return shopkeeper;
            }
        }
        return null;
    }

    public static void openShopInventory(Player player, UUID uuid) {
        Shopkeeper shopkeeper = getShopkeeper(uuid);
        if (shopkeeper == null) return;
        player.openInventory(shopkeeper.getInventory(1).getInventory());
    }

    private void load() {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        if (config.contains("shopkeepers.ids")) {
            config.getStringList("shopkeepers.ids").forEach(id -> {
                if (config.contains("shopkeepers." + id)) {
                    shopkeepers.add(new Shopkeeper(UUID.fromString(id)));
                }
            });
        }
    }
}
