package de.schnorrenbergers.survival.featrues.signing.guard;

import de.schnorrenbergers.survival.featrues.signing.SignItemCommand;
import org.bukkit.Bukkit;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Signed items are no ingredients: the crafting table and the crafter preview show no result, the crafter
 * does not craft (and keeps the items), and the smithing table gives no result.
 */
public final class SignedCraftGuard implements Listener {

    private SignedCraftGuard() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new SignedCraftGuard(), plugin);
    }

    /** Whether any of these stacks is signed. */
    static boolean anySigned(ItemStack[] items) {
        if (items == null) return false;
        for (ItemStack item : items) {
            if (SignItemCommand.signerOf(item) != null) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (anySigned(event.getInventory().getMatrix())) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (event.getBlock().getState(false) instanceof Crafter crafter
                && anySigned(crafter.getInventory().getContents())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithing(PrepareSmithingEvent event) {
        if (anySigned(event.getInventory().getContents())) event.setResult(null);
    }
}
