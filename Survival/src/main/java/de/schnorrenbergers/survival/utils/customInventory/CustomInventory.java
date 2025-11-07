package de.schnorrenbergers.survival.utils.customInventory;

import de.hems.api.ItemApi;
import de.schnorrenbergers.survival.utils.customInventory.types.Inventorys;
import de.schnorrenbergers.survival.utils.customInventory.types.ItemAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class CustomInventory {
    private Inventory inventory;
    private static final ItemStack placeholder = new ItemApi(Material.BLACK_STAINED_GLASS_PANE, " ").build();
    private static HashMap<UUID, ItemAction> actions = new HashMap<>();
    private static HashMap<Inventory, Consumer<InventoryCloseEvent>> closeActions = new HashMap<>();

    public CustomInventory(int size, String title, Consumer<InventoryCloseEvent> onClose) {
        inventory = Bukkit.createInventory(null, size, title);
        closeActions.put(inventory, onClose);
    }

    public CustomInventory(InventoryType type, String title, Consumer<InventoryCloseEvent> onClose) {
        inventory = Bukkit.createInventory(null, type, title);
        closeActions.put(inventory, onClose);
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

    public void fillPlaceHolder() {
        int inventorySize = inventory.getSize();
        for(int i = 0; i < inventorySize; i++) {
            setItem(i, placeholder, ItemAction.placeholder);
        }
    }

    public void addBackButton(int slot, UUID uuid, CustomInventory backInventory) throws MalformedURLException {
        setItem(slot, new ItemApi(new URL("http://textures.minecraft.net/texture/cdc9e4dcfa4221a1fadc1b5b2b11d8beeb57879af1c42362142bae1edd5"), ChatColor.ITALIC.toString() + ChatColor.AQUA.toString() + "Gehe zurück").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return uuid;
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                event.getWhoClicked().closeInventory();
                event.getWhoClicked().openInventory(backInventory.getInventory());
            }

            @Override
            public boolean isMovable() {
                return false;
            }

            @Override
            public boolean fireEvent() {
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() {
                return null;
            }
        });
    }


    public static HashMap<UUID, ItemAction> getActions() {
        return actions;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static HashMap<Inventory, Consumer<InventoryCloseEvent>> getCloseActions() {
        return closeActions;
    }
}
