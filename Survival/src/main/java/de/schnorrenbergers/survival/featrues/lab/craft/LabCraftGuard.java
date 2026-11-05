package de.schnorrenbergers.survival.featrues.lab.craft;

import de.schnorrenbergers.survival.featrues.lab.item.LabItems;
import org.bukkit.Bukkit;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Lab items are pottery sherds, which a decorated pot would take. In a crafting table or crafter they only
 * count in the lab recipes; in any other recipe the result is cleared (table) or the craft is cancelled
 * (crafter). Sherds are used nowhere else in vanilla.
 */
public final class LabCraftGuard implements Listener {

    private LabCraftGuard() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new LabCraftGuard(), plugin);
    }

    /** Whether any of these stacks is a lab item. */
    static boolean containsLabItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (LabItems.read(item).isPresent()) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (LabRecipes.isLabRecipe(event.getRecipe())) return;
        if (containsLabItem(event.getInventory().getMatrix())) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (LabRecipes.isLabRecipe(event.getRecipe())) return;
        if (event.getBlock().getState() instanceof Crafter crafter
                && containsLabItem(crafter.getInventory().getContents())) {
            event.setCancelled(true);
        }
    }
}
