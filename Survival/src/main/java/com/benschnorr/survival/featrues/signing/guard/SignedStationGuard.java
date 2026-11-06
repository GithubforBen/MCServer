package com.benschnorr.survival.featrues.signing.guard;

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent;
import com.benschnorr.survival.featrues.signing.SignItemCommand;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.StonecutterInventory;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Signed items are no ingredients at the stonecutter, the grindstone and in vanilla potion brewing.
 * The stonecutter takes no recipe and gives no result (a click or shift-click on the result slot is cancelled),
 * the grindstone shows no result, and a signed item cannot get into the top ingredient slot of a brewing stand
 * (click, number key, offhand, shift-click, drag, hopper from above); the brew cancel stays as a backstop.
 * Signed bottles below stay as they are. Lab brews keep their own rules in {@code lab/brew}.
 * see knowledge: /capabilities/item-signing.md
 */
public final class SignedStationGuard implements Listener {

    /** The result slot of a stonecutter view. */
    private static final int STONECUTTER_RESULT_SLOT = 1;

    /** The ingredient (top) slot of a brewing stand view. */
    private static final int BREWING_INGREDIENT_SLOT = 3;

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

    /**
     * No signed item into the ingredient slot: placing, number key, offhand swap and shift-click. A shift-click
     * is only refused for items that could be ingredients (not potions or blaze powder), so fuel and bottles
     * still move.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof BrewerInventory)) return;
        if (event.getRawSlot() == BREWING_INGREDIENT_SLOT) {
            ItemStack incoming = switch (event.getClick()) {
                case NUMBER_KEY -> event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                case SWAP_OFFHAND -> event.getWhoClicked().getInventory().getItemInOffHand();
                default -> event.getCursor();
            };
            if (signed(incoming)) event.setCancelled(true);
            return;
        }
        if (event.getClick().isShiftClick() && event.getClickedInventory() != event.getView().getTopInventory()) {
            ItemStack item = event.getCurrentItem();
            if (signed(item) && !isFuelOrBottle(item.getType())) event.setCancelled(true);
        }
    }

    private static boolean isFuelOrBottle(Material type) {
        return type == Material.BLAZE_POWDER || type == Material.POTION || type == Material.SPLASH_POTION
                || type == Material.LINGERING_POTION || type == Material.GLASS_BOTTLE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrewingStandDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory() instanceof BrewerInventory)) return;
        if (event.getRawSlots().contains(BREWING_INGREDIENT_SLOT) && signed(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /** A hopper above a brewing stand feeds the ingredient slot; side hoppers feed fuel and bottles. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrewingStandHopper(InventoryMoveItemEvent event) {
        if (!(event.getDestination() instanceof BrewerInventory target) || !signed(event.getItem())) return;
        Location stand = target.getLocation();
        Location source = event.getSource().getLocation();
        if (stand == null || source == null || source.getBlockY() > stand.getBlockY()) event.setCancelled(true);
    }
}
