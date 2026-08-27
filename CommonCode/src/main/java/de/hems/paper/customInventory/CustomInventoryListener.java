package de.hems.paper.customInventory;

import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.types.ItemAction;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Consumer;

public class CustomInventoryListener implements org.bukkit.event.Listener {

    private static boolean registered = false;

    public CustomInventoryListener(Plugin plugin) {
        PaperContext.setPlugin(plugin);
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
        if (id == null) {
            return;
        }
        ItemAction itemAction;
        try {
            itemAction = CustomInventory.findAction(event.getInventory(), UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (itemAction == null) {
            return;
        }
        if (!itemAction.isMovable()) {
            event.setCancelled(true);
        }
        try {
            if (itemAction.fireEvent()) {
                itemAction.onClick(event);
            }
            CustomInventory next = itemAction.loadInventoryOnClick();
            // updates the screen in place where it can, so a button that only changes a number does not
            // shut the menu and open it again on every single click
            if (next != null) CustomInventory.show(event.getWhoClicked(), next);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops a drag from painting items over the buttons of a custom inventory.
     * <p>
     * Only clicks were ever checked, and a drag is a separate event - so items dragged onto a button landed
     * inside a menu that is thrown away when it closes, and were simply gone. Slots without a button are
     * left alone on purpose: the menu for adding a shop offer uses one as a real input slot.
     *
     * @param event the drag being performed
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != null) {
            return;
        }
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            // raw slots past the top inventory belong to the player and are none of our business
            if (slot >= topSize) {
                continue;
            }
            ItemStack current = event.getInventory().getItem(slot);
            if (current == null || current.getItemMeta() == null) {
                continue;
            }
            if (current.getItemMeta().getPersistentDataContainer()
                    .has(new NamespacedKey("survival", "id"), PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
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
