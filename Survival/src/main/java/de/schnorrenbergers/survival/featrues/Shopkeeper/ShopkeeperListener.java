package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Clicking a shop: its villager opens the shop, or its settings for an owner who sneaks. And the second
 * half of "change the chest" and "move the shop", where the owner was told to click a block.
 */
public class ShopkeeperListener implements Listener {

    /** The key a player carries while they are picking the chest of a shop. */
    public static final NamespacedKey CHEST_PICK = new NamespacedKey("shopkeeper", "chestlocation");
    /** The key a player carries while they are picking where a shop should stand. */
    public static final NamespacedKey SHOP_PICK = new NamespacedKey("shopkeeper", "shoplocation");

    private static boolean registered = false;

    ShopkeeperListener() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    /**
     * The villager of a shop. Cancelled, so the trading screen of the villager never opens behind the shop,
     * and only for the main hand - the event comes once per hand, which opened every shop twice.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        UUID id = Shopkeeper.shopIdOf(event.getRightClicked());
        if (id == null) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        Shopkeeper shopkeeper = ShopkeeperManager.getShopkeeper(id);
        if (shopkeeper == null) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            ShopUi.open(player, shopkeeper);
            return;
        }
        if (!ShopOwnership.owns(player, shopkeeper)) {
            player.sendMessage("Dieser Shop gehört nicht deinem Team.");
            return;
        }
        ShopEditorUi.open(player, shopkeeper);
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String chestPick = player.getPersistentDataContainer().get(CHEST_PICK, PersistentDataType.STRING);
        if (chestPick != null) {
            pickChest(event, chestPick);
            return;
        }
        String shopPick = player.getPersistentDataContainer().get(SHOP_PICK, PersistentDataType.STRING);
        if (shopPick != null) {
            pickShopLocation(event, shopPick);
        }
    }

    private void pickChest(PlayerInteractEvent event, String id) {
        if (!event.hasBlock() || event.getClickedBlock().getType() != Material.CHEST) return;
        Shopkeeper shopkeeper = shopOf(event.getPlayer(), id);
        if (shopkeeper == null || !mayUse(event.getPlayer(), event.getClickedBlock())) return;
        event.setCancelled(true);
        shopkeeper.setChest(event.getClickedBlock().getLocation());
        // binding the chest is a real change - it must not wait for the next autosave
        ShopkeeperManager.saveAll();
        Location chest = shopkeeper.getChest();
        event.getPlayer().sendMessage("Kiste gesetzt: (" + chest.getBlockX() + ", " + chest.getBlockY()
                + ", " + chest.getBlockZ() + ")");
        event.getPlayer().getPersistentDataContainer().remove(CHEST_PICK);
    }

    /**
     * Puts the shop on top of the block that was clicked.
     * <p>
     * On top, not in it: standing the villager inside the block would push it out again the moment the
     * chunk ticks, and the shop would drift away from where its owner put it.
     */
    private void pickShopLocation(PlayerInteractEvent event, String id) {
        if (!event.hasBlock()) return;
        Shopkeeper shopkeeper = shopOf(event.getPlayer(), id);
        Block block = event.getClickedBlock();
        if (shopkeeper == null || !mayUse(event.getPlayer(), block)) return;
        event.setCancelled(true);
        Location target = block.getLocation().add(0.5, 1, 0.5);
        // facing the way the owner is looking, so a shop can be turned towards the customers
        target.setYaw(event.getPlayer().getLocation().getYaw());
        shopkeeper.moveTo(target);
        ShopkeeperManager.saveAll();
        event.getPlayer().sendMessage("Shop verschoben: (" + target.getBlockX() + ", " + target.getBlockY()
                + ", " + target.getBlockZ() + ")");
        event.getPlayer().getPersistentDataContainer().remove(SHOP_PICK);
    }

    /**
     * @param player the player that is picking
     * @param id     the shop they are picking for
     * @return that shop, or {@code null} when it is gone or not theirs
     */
    private @Nullable Shopkeeper shopOf(Player player, String id) {
        Shopkeeper shopkeeper;
        try {
            shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            stopPicking(player);
            return null;
        }
        if (shopkeeper == null) {
            player.sendMessage("Diesen Shop gibt es nicht mehr.");
            stopPicking(player);
            return null;
        }
        if (!ShopOwnership.owns(player, shopkeeper)) {
            player.sendMessage("Dieser Shop gehört nicht deinem Team.");
            return null;
        }
        return shopkeeper;
    }

    /**
     * @return whether the block is on land the player's team has claimed; they are told when it is not
     */
    private static boolean mayUse(Player player, Block block) {
        String problem = ShopOwnership.problemWithChunk(player, block.getChunk());
        if (problem != null) player.sendMessage(problem);
        return problem == null;
    }

    /**
     * Takes the player out of "click a block now" mode, whichever block they were picking.
     *
     * @param player the player
     */
    static void stopPicking(Player player) {
        player.getPersistentDataContainer().remove(CHEST_PICK);
        player.getPersistentDataContainer().remove(SHOP_PICK);
    }
}
