package de.hems.paper.customInventory;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.types.ItemAction;
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
    private final Inventory inventory;
    private static final ItemStack placeholder = new ItemApi(Material.BLACK_STAINED_GLASS_PANE, " ").build();
    private static final HashMap<UUID, ItemAction> actions = new HashMap<>();
    private static final Map<Inventory, Consumer<InventoryCloseEvent>> closeActions = Collections.synchronizedMap(new WeakHashMap<>());
    /**
     * The actions of every inventory, kept per inventory instead of globally so that two inventories built
     * from the same code - for two players, or for two different servers - do not overwrite each others
     * behaviour. Weak, so the actions disappear together with the inventory they belong to.
     */
    private static final Map<Inventory, Map<UUID, ItemAction>> inventoryActions = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<UUID, ItemAction> localActions;

    public CustomInventory(int size, String title, Consumer<InventoryCloseEvent> onClose) {
        inventory = Bukkit.createInventory(null, size, title);
        localActions = new HashMap<>();
        closeActions.put(inventory, onClose);
        inventoryActions.put(inventory, localActions);
    }

    public CustomInventory(InventoryType type, String title, Consumer<InventoryCloseEvent> onClose) {
        inventory = Bukkit.createInventory(null, type, title);
        localActions = new HashMap<>();
        closeActions.put(inventory, onClose);
        inventoryActions.put(inventory, localActions);
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
        localActions.put(action.getID(), action);
        inventory.setItem(position, stack);
    }

    /**
     * Places a placeholder item in the specified inventory slot position. The placeholder
     * is a predefined static item associated with a dummy action that prevents interaction.
     *
     * @param position the slot position in the inventory where the placeholder item should be placed
     */
    public void setPlaceHolder(int position) {
        setItem(position, placeholder, ItemAction.NOTMOVABLE);
    }

    public void fillPlaceHolder() {
        int inventorySize = inventory.getSize();
        for(int i = 0; i < inventorySize; i++) {
            setItem(i, placeholder, ItemAction.NOTMOVABLE);
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


    /**
     * @return the globally registered actions
     * @deprecated actions live on the inventory they were put into, use {@link #findAction(Inventory, UUID)}
     */
    @Deprecated
    public static HashMap<UUID, ItemAction> getActions() {
        return actions;
    }

    /**
     * Resolves the action of a clicked item.
     *
     * @param inventory the inventory the item was clicked in
     * @param id the id stored on the item
     * @return the action to run, or {@code null} if the item does not belong to a custom inventory
     */
    public static ItemAction findAction(Inventory inventory, UUID id) {
        Map<UUID, ItemAction> local = inventoryActions.get(inventory);
        if (local != null) {
            ItemAction action = local.get(id);
            if (action != null) return action;
        }
        return actions.get(id);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static Map<Inventory, Consumer<InventoryCloseEvent>> getCloseActions() {
        return closeActions;
    }

    public void removeItem(int i) {
        inventory.setItem(i, null);
    }
}
