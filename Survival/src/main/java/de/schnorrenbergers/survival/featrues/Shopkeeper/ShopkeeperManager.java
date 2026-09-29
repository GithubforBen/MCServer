package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.paper.team.TeamService;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every shop on this server: where they are found, how they are made, and when they are written out.
 */
public final class ShopkeeperManager {

    /** How often everything is written out, so a crash costs at most this much. */
    private static final long AUTOSAVE_TICKS = 20L * 60L * 5L;

    private static final Map<UUID, Shopkeeper> shopkeepers = new LinkedHashMap<>();
    private static ShopkeeperStore store;
    private static ShopSettings settings;

    /**
     * Shops by the chunk their chest sits in.
     * <p>
     * Chunks unload constantly, so the unload handler must not walk the whole shop list every time. The
     * index is thrown away whenever a chest moves or a shop is added.
     */
    private static Map<Long, List<Shopkeeper>> chestIndex;

    private ShopkeeperManager() {
    }

    /**
     * Loads the shops and starts looking after them.
     *
     * @param shopStore    where the shops are kept
     * @param shopSettings what the server decides about shops
     */
    public static void init(ShopkeeperStore shopStore, ShopSettings shopSettings) {
        if (store != null) return;
        store = shopStore;
        settings = shopSettings;
        for (Shopkeeper shop : store.loadAll()) shopkeepers.put(shop.getUuid(), shop);
        new ShopkeeperChunkListener();
        new ShopkeeperListener();
        new ShopChestListener();
        // shops whose chunk is already in get their villager now, the rest when their chunk loads
        ShopkeeperChunkListener.spawnInLoadedChunks();
        Bukkit.getScheduler().runTaskTimer(Survival.getInstance(),
                ShopkeeperManager::saveAll, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        // a shop is kept under its team's name, so it has to follow the team when that changes
        TeamService.onRename(ShopkeeperManager::onTeamRenamed);
    }

    /**
     * Writes every shopkeeper to disk.
     */
    public static void saveAll() {
        if (store == null) return;
        store.saveAll(shopkeepers.values());
    }

    /**
     * Stores everything and removes the villagers, so the next start finds exactly one villager per shop.
     */
    public static void shutdown() {
        if (store == null) return;
        saveAll();
        shopkeepers.values().forEach(Shopkeeper::despawn);
    }

    /**
     * Moves every shop of a team over to its new name.
     * <p>
     * Without this a rename orphaned every shop the team had: the shop still pointed at the old name, told
     * every customer the team had been disbanded, and its owners could no longer open its settings.
     *
     * @param oldName what the team was called
     * @param newName what it is called now
     */
    static void onTeamRenamed(String oldName, String newName) {
        boolean changed = false;
        for (Shopkeeper shop : shopkeepers.values()) {
            if (oldName.equalsIgnoreCase(shop.getOwnerTeam())) {
                shop.setOwnerTeam(newName);
                changed = true;
            }
        }
        if (changed) saveAll();
    }

    // ------------------------------------------------------------------ finding shops

    /**
     * @return every shopkeeper that is currently loaded, for the marketplace to collect offers from
     */
    public static List<Shopkeeper> getShopkeepers() {
        return List.copyOf(shopkeepers.values());
    }

    /**
     * @param uuid the id of a shop
     * @return that shop, or {@code null}
     */
    public static @Nullable Shopkeeper getShopkeeper(UUID uuid) {
        return uuid == null ? null : shopkeepers.get(uuid);
    }

    /** Drops the lookup, so it is rebuilt the next time a chunk unloads. */
    public static void invalidateChestIndex() {
        chestIndex = null;
    }

    private static Map<Long, List<Shopkeeper>> chestIndex() {
        Map<Long, List<Shopkeeper>> index = chestIndex;
        if (index != null) return index;
        index = new HashMap<>();
        for (Shopkeeper shopkeeper : shopkeepers.values()) {
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

    /**
     * @param location a block
     * @return the shop whose stock chest stands there, or {@code null} when no shop does
     */
    public static @Nullable Shopkeeper withChestAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        List<Shopkeeper> candidates = chestIndex().get(Chunk.getChunkKey(location));
        if (candidates == null) return null;
        for (Shopkeeper shopkeeper : candidates) {
            Location chest = shopkeeper.getChest();
            if (chest == null || chest.getWorld() == null) continue;
            if (!chest.getWorld().equals(location.getWorld())) continue;
            if (chest.getBlockX() == location.getBlockX()
                    && chest.getBlockY() == location.getBlockY()
                    && chest.getBlockZ() == location.getBlockZ()) {
                return shopkeeper;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ making shops

    /**
     * Puts a shop down: the villager where the player stands, the chest they stand on as its stock. The
     * player pays {@link ShopSettings#getCreateCost()} for it.
     *
     * @param player who is opening the shop
     * @param name   what it should be called
     * @return the new shop, or {@code null} when it could not be made - the player has been told why
     */
    public static @Nullable Shopkeeper createShopkeeper(Player player, String name) {
        int cost = settings.getCreateCost();
        Location chest = player.getLocation().getBlock().getLocation();
        if (chest.getBlock().getType() != Material.CHEST) {
            player.sendMessage("Stell dich dafür auf die Kiste, die das Lager des Shops sein soll.");
            return null;
        }
        String problem = ShopOwnership.problemWithChunk(player, player.getChunk());
        if (problem != null) {
            player.sendMessage(problem);
            return null;
        }
        // paid last, once nothing else can stand in the way, so nobody pays for a shop that is then refused
        if (cost > 0 && !MoneyHandler.removeMoney(cost, player.getUniqueId())) {
            player.sendMessage("Ein Shop kostet " + cost + " Bits - so viele hast du nicht.");
            return null;
        }
        if (cost > 0) player.sendMessage("Für den Shop wurden dir " + cost + " Bits abgezogen.");
        Shopkeeper shopkeeper = new Shopkeeper(UUID.randomUUID(), name, player.getLocation(), chest,
                ShopOwnership.teamOf(player), new ArrayList<>());
        shopkeepers.put(shopkeeper.getUuid(), shopkeeper);
        shopkeeper.spawnOrAdoptVillager();
        invalidateChestIndex();
        // a new shop has to reach the disk right away, otherwise it is gone after the next crash
        saveAll();
        return shopkeeper;
    }
}
