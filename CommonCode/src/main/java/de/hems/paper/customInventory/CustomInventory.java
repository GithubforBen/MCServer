package de.hems.paper.customInventory;

import de.hems.api.ItemApi;
import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.types.ItemAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
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
    /** What the inventory was created with. Bukkit cannot change it afterwards, so a menu whose title
     * differs has to be opened rather than updated. */
    private final String title;
    private static final ItemStack placeholder = new ItemApi(Material.BLACK_STAINED_GLASS_PANE, " ").build();
    private static final HashMap<UUID, ItemAction> actions = new HashMap<>();
    private static final Map<Inventory, Consumer<InventoryCloseEvent>> closeActions = Collections.synchronizedMap(new WeakHashMap<>());
    /**
     * The actions of every inventory, kept per inventory instead of globally so that two inventories built
     * from the same code - for two players, or for two different servers - do not overwrite each others
     * behaviour. Weak, so the actions disappear together with the inventory they belong to.
     */
    private static final Map<Inventory, Map<UUID, ItemAction>> inventoryActions = Collections.synchronizedMap(new WeakHashMap<>());
    /** The menu behind an open inventory, so a click can update the screen the player is already looking at. */
    private static final Map<Inventory, CustomInventory> openMenus = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<UUID, ItemAction> localActions;

    public CustomInventory(int size, String title, Consumer<InventoryCloseEvent> onClose) {
        this(Bukkit.createInventory(null, size, title), title, onClose);
    }

    public CustomInventory(InventoryType type, String title, Consumer<InventoryCloseEvent> onClose) {
        this(Bukkit.createInventory(null, type, title), title, onClose);
    }

    private CustomInventory(Inventory inventory, String title, Consumer<InventoryCloseEvent> onClose) {
        this.inventory = inventory;
        this.title = title;
        this.localActions = new HashMap<>();
        closeActions.put(inventory, onClose);
        inventoryActions.put(inventory, localActions);
        openMenus.put(inventory, this);
    }

    /**
     * @return the title this menu was built with
     */
    public String getTitle() {
        return title;
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
                show(event.getWhoClicked(), backInventory);
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

    /* ------------------------------------------------------------------ showing menus */

    /**
     * Shows a freshly built menu to a viewer, updating the one they already have open where possible.
     * <p>
     * Closing and reopening for every click is what made menus flicker, and it is not just ugly: closing
     * runs the close handler of a menu that is not actually being left, drops whatever the player was
     * holding on their cursor back into the world, and leaves a frame in which they are standing in front
     * of no inventory at all. Writing the new contents into the inventory they are already looking at has
     * none of those problems. Only a menu of a different size or title has to be opened for real, because
     * bukkit cannot change either after the fact.
     *
     * @param viewer who is looking
     * @param fresh  the menu as it should look now
     */
    public static void show(HumanEntity viewer, CustomInventory fresh) {
        CustomInventory open = openOf(viewer);
        if (open != null && open != fresh && open.canBeReplacedBy(fresh)) {
            open.replaceWith(fresh);
            return;
        }
        if (open == fresh) {
            // the caller rebuilt into the very inventory that is on screen, so it is already up to date
            return;
        }
        viewer.openInventory(fresh.getInventory());
    }

    /**
     * @param viewer who is looking
     * @return the custom menu they have open, or {@code null} if it is not one of ours
     */
    public static CustomInventory openOf(HumanEntity viewer) {
        return openMenus.get(viewer.getOpenInventory().getTopInventory());
    }

    /**
     * @param fresh the menu that should be shown
     * @return whether it can be drawn into this one instead of being opened
     */
    private boolean canBeReplacedBy(CustomInventory fresh) {
        return inventory.getSize() == fresh.inventory.getSize()
                && Objects.equals(title, fresh.title);
    }

    /**
     * Takes over the contents, the actions and the close handler of a freshly built menu, so the open
     * screen shows it without being reopened.
     *
     * @param fresh the menu to copy in
     */
    private void replaceWith(CustomInventory fresh) {
        inventory.setContents(fresh.inventory.getContents());
        localActions.clear();
        localActions.putAll(fresh.localActions);
        closeActions.put(inventory, closeActions.get(fresh.inventory));
        resync();
        // and again next tick: a redraw usually happens inside a click that is cancelled, and bukkit
        // applies that cancellation after the handler has run by sending the client the items it had
        // before. Without this second pass the screen would show the old numbers until the next click.
        if (PaperContext.hasPlugin()) {
            Bukkit.getScheduler().runTask(PaperContext.getPlugin(), this::resync);
        }
    }

    /**
     * Pushes the current contents to everyone looking at this inventory.
     */
    private void resync() {
        for (HumanEntity viewer : List.copyOf(inventory.getViewers())) {
            if (viewer instanceof Player player) player.updateInventory();
        }
    }

    public static Map<Inventory, Consumer<InventoryCloseEvent>> getCloseActions() {
        return closeActions;
    }

    public void removeItem(int i) {
        inventory.setItem(i, null);
    }
}
