package de.hems.paper.cosmetic;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Keeps a gadget's item with its owner.
 * <p>
 * A gadget hands its item out again whenever its owner does not carry one - after a respawn, on the next
 * join. So an item that can be put down somewhere is an item that can be farmed: into a chest, die, and
 * there is a second one. For a fishing rod that is untidy; for the endless pearl on survival it would be a
 * pearl machine. So the item cannot leave: it is not dropped, not put into any inventory but its owner's,
 * and not left behind on death.
 */
public class GadgetItemGuard implements Listener {

    public GadgetItemGuard(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (GadgetItems.isAny(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (ownInventory(top)) return;
        boolean inTop = event.getClickedInventory() == top;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        // shift-click from their own inventory up into the chest
        if (!inTop && event.isShiftClick() && GadgetItems.isAny(current)) {
            event.setCancelled(true);
            return;
        }
        if (!inTop) return;
        // putting the carried item down into the chest
        if (GadgetItems.isAny(cursor)) {
            event.setCancelled(true);
            return;
        }
        // the number keys and the off-hand key swap a hotbar item straight into the chest slot
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getClick().isKeyboardClick()) {
            int button = event.getHotbarButton();
            ItemStack hotbar = button >= 0
                    ? event.getWhoClicked().getInventory().getItem(button)
                    : event.getWhoClicked().getInventory().getItemInOffHand();
            if (GadgetItems.isAny(hotbar)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (ownInventory(top) || !GadgetItems.isAny(event.getOldCursor())) return;
        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        // it comes back with the respawn; left on the ground it would be a second one for whoever finds it
        event.getDrops().removeIf(GadgetItems::isAny);
    }

    /**
     * @param top the upper half of an open inventory view
     * @return whether it is the player's own crafting grid, which is where nothing leaves them
     */
    private static boolean ownInventory(Inventory top) {
        InventoryType type = top.getType();
        return type == InventoryType.CRAFTING || type == InventoryType.PLAYER || type == InventoryType.CREATIVE;
    }
}
