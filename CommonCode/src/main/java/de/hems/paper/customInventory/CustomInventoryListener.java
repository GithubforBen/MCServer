package de.hems.paper.customInventory;

import de.hems.paper.customInventory.types.ItemAction;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Consumer;

public class CustomInventoryListener implements org.bukkit.event.Listener {

    private static boolean registered = false;

    public CustomInventoryListener(Plugin plugin) {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    /**
     * Handles the InventoryClickEvent for custom inventory interactions. This method ensures
     * that interactions with specific items in the inventory trigger associated actions, while
     * also managing item movement and inventory transitions based on the item's defined behavior.
     *
     * @param event the InventoryClickEvent triggered when a player interacts with an inventory
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != null) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getItemMeta() == null) {
            return;
        }
        String id = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey("survival", "id"), PersistentDataType.STRING);
        try {
            UUID uuid = UUID.fromString(id);
            ItemAction itemAction = CustomInventory.getActions().get(uuid);
            if (itemAction.fireEvent()) {
                itemAction.onClick(event);
            }
            if (!itemAction.isMovable()) {
                event.setCancelled(true);
            }
            if (itemAction.loadInventoryOnClick() != null) {
                event.getWhoClicked().closeInventory();
                event.getWhoClicked().openInventory(itemAction.loadInventoryOnClick().getInventory());
            }
        } catch (Exception ignored) {
            System.out.println(ignored.toString());
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != null) {
            return;
        }
        Consumer<InventoryCloseEvent> inventoryCloseEventConsumer = CustomInventory.getCloseActions().get(event.getInventory());
        if (inventoryCloseEventConsumer == null) return;
        inventoryCloseEventConsumer.accept(event);
    }

    public static boolean hasBeenRegistered() {
        return registered;
    }
}
