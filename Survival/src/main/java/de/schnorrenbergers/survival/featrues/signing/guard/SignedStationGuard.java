package de.schnorrenbergers.survival.featrues.signing.guard;

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent;
import de.schnorrenbergers.survival.featrues.signing.SignItemCommand;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.StonecutterInventory;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Signed items are no ingredients at the stonecutter, the grindstone and in vanilla potion brewing.
 * The stonecutter takes no recipe and gives no result (a click or shift-click on the result slot is cancelled),
 * the grindstone shows no result, and a brewing stand with a signed top ingredient does not brew; signed
 * bottles below stay as they are. Lab brews keep their own rules in {@code lab/brew}.
 * see knowledge: /capabilities/item-signing.md
 */
public final class SignedStationGuard implements Listener {

    /** The result slot of a stonecutter view. */
    private static final int STONECUTTER_RESULT_SLOT = 1;

    private SignedStationGuard() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new SignedStationGuard(), plugin);
    }

    private static boolean signed(ItemStack item) {
        return SignItemCommand.signerOf(item) != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStonecutterSelect(PlayerStonecutterRecipeSelectEvent event) {
        if (signed(event.getStonecutterInventory().getInputItem())) event.setCancelled(true);
    }

    /** Covers click, shift-click and hotkeys on the result slot, whatever recipe was chosen before. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStonecutterTake(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof StonecutterInventory cutter)) return;
        if (event.getRawSlot() != STONECUTTER_RESULT_SLOT || !signed(cutter.getInputItem())) return;
        event.setCancelled(true);
        cutter.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrindstone(PrepareGrindstoneEvent event) {
        if (signed(event.getInventory().getUpperItem()) || signed(event.getInventory().getLowerItem())) {
            event.setResult(null);
        }
    }

    /**
     * A signed top ingredient brews nothing. With an unsigned ingredient, a signed bottle keeps its original
     * (the lab does the same for its own brews, so both agree).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        if (signed(inventory.getIngredient())) {
            event.setCancelled(true);
            return;
        }
        List<ItemStack> results = event.getResults();
        for (int i = 0; i < 3 && i < results.size(); i++) {
            ItemStack before = inventory.getItem(i);
            if (signed(before)) results.set(i, before.clone());
        }
    }
}
