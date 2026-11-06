package com.benschnorr.survival.featrues.lab.brew;

import com.benschnorr.survival.featrues.lab.LabSettings;
import com.benschnorr.survival.featrues.lab.cutting.LabCutting;
import com.benschnorr.survival.featrues.lab.cutting.LabCutting.Ingredient;
import com.benschnorr.survival.featrues.lab.item.LabData;
import com.benschnorr.survival.featrues.lab.item.LabItems;
import com.benschnorr.survival.featrues.lab.item.LabKind;
import com.benschnorr.survival.featrues.lab.item.LabMaterials;
import com.benschnorr.survival.featrues.lab.upgrade.LabUpgradeReader;
import com.benschnorr.survival.featrues.lab.upgrade.LabUpgrades;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.BrewingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Cutting and stretching run on the vanilla engine like purifying: one {@link BrewingRecipe} per
 * ingredient whose input matches only Kokain the ingredient is a valid step for, so vanilla starts no brew
 * for any other item. The {@link BrewEvent} handler replaces what vanilla would produce, and only for
 * Kokain slots: a vanilla potion brew with the same ingredient stays vanilla.
 * see knowledge: /capabilities/lab-cutting.md
 */
final class LabCutBrewing implements Listener {

    private final JavaPlugin plugin;
    private final BrewSettings settings;
    private final LabSettings labSettings;
    private final LabUpgradeReader upgrades;

    private LabCutBrewing(JavaPlugin plugin, BrewSettings settings, LabSettings labSettings,
                          LabUpgradeReader upgrades) {
        this.plugin = plugin;
        this.settings = settings;
        this.labSettings = labSettings;
        this.upgrades = upgrades;
    }

    /** Call after {@code LabPurifying.init}: the hopper rule keeps its purifying steps and adds these. */
    static void init(JavaPlugin plugin, BrewSettings settings, LabSettings labSettings, LabUpgradeReader upgrades) {
        LabCutBrewing cutting = new LabCutBrewing(plugin, settings, labSettings, upgrades);
        cutting.registerRecipes();
        var purifying = StepRule.rule;
        StepRule.rule = (ingredient, item) -> purifying.test(ingredient, item) || isStep(ingredient, item);
        Bukkit.getPluginManager().registerEvents(cutting, plugin);
    }

    /** Whether the vanilla item on top cuts or stretches the lab Kokain in the slot. */
    static boolean isStep(ItemStack ingredient, ItemStack item) {
        if (ingredient == null || LabSlots.signed(ingredient)) return false;
        Optional<Ingredient> cut = LabCutting.ingredient(ingredient.getType());
        return cut.isPresent() && isStep(cut.get(), item);
    }

    private static boolean isStep(Ingredient ingredient, ItemStack item) {
        if (LabSlots.signed(item)) return false; // signed Kokain is never cut
        return LabItems.read(item).map(data -> LabCutting.isStep(ingredient, data, item.getAmount(), item.getMaxStackSize())).orElse(false);
    }

    private void registerRecipes() {
        int added = 0;
        ItemStack display = LabItems.create(LabData.of(LabKind.KOKAIN, 1));
        for (Ingredient ingredient : Ingredient.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "lab_cut_" + ingredient.name().toLowerCase());
            // a predicate instead of a MaterialChoice, so a signed ingredient never starts a vanilla brew
            RecipeChoice ingredientChoice = RecipeChoice.predicateChoice(
                    item -> item.getType() == ingredient.material() && !LabSlots.signed(item),
                    new ItemStack(ingredient.material()));
            Predicate<ItemStack> accepts = item -> isStep(ingredient, item);
            // the predicate keeps vanilla from starting a brew when no Kokain can take the ingredient
            RecipeChoice input = RecipeChoice.predicateChoice(accepts, display);
            if (!add(new BrewingRecipe(key, display, input, ingredientChoice), key)) {
                plugin.getLogger().warning("Lab: " + key.getKey() + " with a predicate input failed, "
                        + "falling back to any Kokain; a stand whose Kokain cannot take it may start a brew");
                RecipeChoice broad = new RecipeChoice.MaterialChoice(LabMaterials.KOKAIN);
                if (!add(new BrewingRecipe(key, display, broad, ingredientChoice), key)) continue;
            }
            added++;
        }
        plugin.getLogger().info("Lab: " + added + " cutting recipes added");
    }

    private boolean add(BrewingRecipe recipe, NamespacedKey key) {
        try {
            Bukkit.removeRecipe(key);
            return Bukkit.addRecipe(recipe);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Lab: recipe " + key.getKey() + " failed: " + e.getMessage());
            return false;
        }
    }

    @EventHandler
    public void onStart(BrewingStartEvent event) {
        if (!hasCuttableKokain(event.getSource(), event.getBlock().getState(false) instanceof BrewingStand stand
                ? stand.getInventory() : null)) return;
        LabUpgrades used = upgrades.scan(event.getBlock());
        if (event.getBlock().getState(false) instanceof BrewingStand stand) {
            upgrades.store(stand.getPersistentDataContainer(), used);
        }
        int ticks = used.brewTicks(settings.brewTicks(), labSettings);
        if (ticks == event.getTotalBrewTime()) return;
        event.setTotalBrewTime(ticks);
        event.setBrewingTime(ticks);
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        ItemStack top = inventory.getIngredient();
        if (top == null) return;
        if (LabSlots.signed(top) && wouldCut(top, inventory)) {
            event.setCancelled(true); // a signed ingredient is not consumed; vanilla's brew is stopped
            return;
        }
        if (!hasCuttableKokain(top, inventory)) return; // anything else stays vanilla
        Ingredient ingredient = LabCutting.ingredient(top.getType()).orElseThrow();
        if (event.getBlock().getState(false) instanceof BrewingStand stand) {
            upgrades.clear(stand.getPersistentDataContainer());
        }
        List<ItemStack> results = event.getResults();
        for (int i = 0; i < 3 && i < results.size(); i++) {
            ItemStack before = inventory.getItem(i);
            if (LabSlots.signed(before)) {
                results.set(i, before.clone());
                continue;
            }
            LabData data = LabItems.read(before).orElse(null);
            if (data == null) continue; // not Kokain or paste: vanilla's own result stays
            LabCutting.Result cut = LabCutting.apply(ingredient, data, before.getAmount(), before.getMaxStackSize()).orElse(null);
            results.set(i, cut == null ? before.clone() : LabItems.create(cut.data(), cut.amount()));
        }
    }

    /** Whether a signed ingredient on top would have cut some unsigned Kokain in the stand. */
    private static boolean wouldCut(ItemStack top, BrewerInventory inventory) {
        Optional<Ingredient> ingredient = LabCutting.ingredient(top.getType());
        if (ingredient.isEmpty()) return false;
        for (int i = 0; i < 3; i++) {
            if (isStep(ingredient.get(), inventory.getItem(i))) return true;
        }
        return false;
    }

    /** Whether the ingredient is a cutting item and some bottom slot holds Kokain it works on. */
    private static boolean hasCuttableKokain(ItemStack top, BrewerInventory inventory) {
        if (top == null || inventory == null) return false;
        Optional<Ingredient> ingredient = LabCutting.ingredient(top.getType());
        if (ingredient.isEmpty()) return false;
        for (int i = 0; i < 3; i++) {
            if (isStep(ingredient.get(), inventory.getItem(i))) return true;
        }
        return false;
    }
}
