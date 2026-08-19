package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.List;

/**
 * Ties a shop to the life of the chunk it stands in.
 * <p>
 * Spawning is the delicate part. Since 1.17 entities live in their own storage, so {@code chunk.isLoaded()}
 * can be true while {@code chunk.getEntities()} is still empty. Looking on a timer after plugin start
 * therefore finds nothing, concludes the villager is gone and spawns another one - one more shopkeeper per
 * restart, forever. Waiting for {@link EntitiesLoadEvent} removes the guesswork.
 * <p>
 * The other half is the stock. A chest can only be read while its chunk is loaded, so the last tick before
 * it unloads is when the marketplace has to write down what was in it.
 */
public class ShopkeeperChunkListener implements Listener {

    private static boolean registered = false;

    public ShopkeeperChunkListener() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        spawnIn(event.getChunk());
    }

    /**
     * Reads the chest of every shop in a chunk before it goes away.
     * <p>
     * This is the last tick the chest can be looked at for free. Afterwards the marketplace can only
     * report what was remembered here, so skipping it would leave the listing stuck on whatever the stock
     * was when somebody last stood next to the shop.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<Shopkeeper> shops = ShopkeeperManager.withChestInChunk(event.getChunk());
        if (shops.isEmpty()) return;
        for (Shopkeeper shopkeeper : shops) {
            shopkeeper.refreshStock();
        }
        // no write to disk here - chunks unload many times a second, and the autosave and the shutdown
        // both persist what was remembered
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Shopkeeper shopkeeper : ShopkeeperManager.getShopkeepers()) {
            if (shopkeeper.isInChunk(event.getChunk())) shopkeeper.releaseVillager();
        }
    }

    /**
     * Spawns or adopts the villagers of every shop standing in a chunk.
     *
     * @param chunk the chunk whose entities just became available
     */
    private static void spawnIn(Chunk chunk) {
        for (Shopkeeper shopkeeper : ShopkeeperManager.getShopkeepers()) {
            if (shopkeeper.isInChunk(chunk)) shopkeeper.spawnOrAdoptVillager();
        }
    }

    /**
     * Catches up on shops whose chunk was already loaded before the plugin started, which is what happens
     * on a reload. Chunks that are not loaded are left alone - their villager appears when a player gets
     * close enough for the chunk to come in.
     */
    public static void spawnInLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk.isEntitiesLoaded()) spawnIn(chunk);
            }
        }
    }
}
