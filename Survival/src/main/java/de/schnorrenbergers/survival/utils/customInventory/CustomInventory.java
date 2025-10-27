package de.schnorrenbergers.survival.utils.customInventory;

import de.hems.api.ItemApi;
import de.schnorrenbergers.survival.utils.customInventory.types.ItemAction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;

public class CustomInventory {
    private Inventory inventory;
    private Consumer<InventoryCloseEvent> onClose;
    private static final ItemStack placeholder = new ItemApi(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "[]").build();
    private static HashMap<UUID, ItemAction> actions = new HashMap<>();

    public CustomInventory(int size, String title) {
        inventory = Bukkit.createInventory(null, size, title);
    }

    public CustomInventory(InventoryType type, String title) {
        inventory = Bukkit.createInventory(null, type, title);
    }

    /**
     * Sets an item in the inventory at the specified position, associates it with an action,
     * and updates its metadata with a unique identifier for the action.
     *
     * @param position the slot position in the inventory where the item stack should be placed
     * @param stack the item stack to be placed in the specified position
     * @param action the action associated with the item, providing behavior upon specific interactions
     */
    public void setItem(int position, ItemStack stack, ItemAction action) {
        ItemMeta itemMeta = stack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(new NamespacedKey("survival", "id"), PersistentDataType.STRING, action.getID().toString());
        stack.setItemMeta(itemMeta);
        actions.put(action.getID(), action);
        inventory.setItem(position, stack);
    }

    /**
     * Places a placeholder item in the specified inventory slot position. The placeholder
     * is a predefined static item associated with a dummy action that prevents interaction.
     *
     * @param position the slot position in the inventory where the placeholder item should be placed
     */
    public void setPlaceHolder(int position) {
        setItem(position, placeholder, ItemAction.placeholder);
    }

    /**
     * Sets a consumer that will be triggered when the inventory is closed.
     * The specified consumer accepts an {@link InventoryCloseEvent} instance, allowing
     * custom handling of the close event for the inventory.
     *
     * @param consumer the consumer to handle the inventory close event
     */
    public void onClose(Consumer<InventoryCloseEvent> consumer) {
        onClose = consumer;
    }

    /**
     * Executes the close action when the inventory is closed. If a consumer is set for the
     * close action via the {@code onClose} method, it is invoked with the provided
     * {@link InventoryCloseEvent}. This allows for custom handling of inventory close events.
     *
     * @param event the inventory close event containing information about the closure,
     *              such as the player who closed the inventory and the inventory details
     */
    public void performeCloseAction(InventoryCloseEvent event) {
        if (onClose != null) {
            onClose.accept(event);
        }
    }

    public static HashMap<UUID, ItemAction> getActions() {
        return actions;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
