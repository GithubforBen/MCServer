package de.schnorrenbergers.survival.featrues.lab.brew;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.chain.LabChain;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabItems;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;
import de.schnorrenbergers.survival.featrues.lab.item.LabMaterials;
import de.schnorrenbergers.survival.featrues.lab.upgrade.LabUpgradeReader;
import de.schnorrenbergers.survival.featrues.lab.upgrade.LabUpgrades;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.BrewingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Purifying and Kokain run on the vanilla engine: a registered {@link BrewingRecipe} per valid step lets
 * vanilla start the brew, and the {@link BrewEvent} handler replaces what vanilla would produce by
 * rolling. Inputs match lab items only, so vanilla never brews from a plain pottery sherd.
 * see knowledge: /decisions/gadget-choices.md
 */
final class LabPurifying implements Listener {

    private static final LabKind[] TOPS = {LabKind.PAPIERFILTER, LabKind.KOHLEFILTER, LabKind.SEIDENFILTER,
            LabKind.GOLDFILTER, LabKind.FEENSTAUB};

    private final JavaPlugin plugin;
    private final LabChain chain;
    private final BrewSettings settings;
    private final LabSettings labSettings;
    private final LabUpgradeReader upgrades;

    private LabPurifying(JavaPlugin plugin, LabChain chain, BrewSettings settings, LabSettings labSettings,
                         LabUpgradeReader upgrades) {
        this.plugin = plugin;
        this.chain = chain;
        this.settings = settings;
        this.labSettings = labSettings;
        this.upgrades = upgrades;
    }

    static void init(JavaPlugin plugin, LabChain chain, BrewSettings settings, LabSettings labSettings,
                    LabUpgradeReader upgrades) {
        LabPurifying purifying = new LabPurifying(plugin, chain, settings, labSettings, upgrades);
        purifying.registerRecipes();
        // the hopper rule: a bottom item stays while the top ingredient is a valid step for it
        StepRule.rule = (ingredient, item) -> {
            LabData top = LabItems.read(ingredient).orElse(null);
            LabData bottom = LabItems.read(item).orElse(null);
            return chain.isStep(top, bottom);
        };
        Bukkit.getPluginManager().registerEvents(purifying, plugin);
    }

    /** One recipe per valid step: the filter of a tier on paste of that purity, Feenstaub on any paste. */
    private void registerRecipes() {
        int added = 0;
        for (LabKind topKind : TOPS) {
            LabData top = LabData.of(topKind);
            for (int purity = 1; purity <= 5; purity++) {
                LabData bottom = LabData.of(LabKind.PASTE, purity);
                LabData goods = chain.goods(top, bottom).orElse(null);
                if (goods == null) continue;
                NamespacedKey key = new NamespacedKey(plugin, "lab_" + topKind.name().toLowerCase() + "_" + purity);
                BrewingRecipe recipe = new BrewingRecipe(key, LabItems.create(goods),
                        new RecipeChoice.ExactChoice(LabItems.create(bottom)),
                        new RecipeChoice.ExactChoice(LabItems.create(top)));
                try {
                    Bukkit.removeRecipe(key);
                    if (Bukkit.addRecipe(recipe)) added++;
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Lab: recipe " + key.getKey() + " failed: " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Lab: " + added + " purifying recipes added");
        // for task 10.1: does the item-level input set follow a runtime recipe for each type?
        for (Material type : LabMaterials.bottomInputs()) {
            int reported = 0;
            for (Recipe recipe : Bukkit.getRecipesFor(LabItems.create(LabData.of(LabKind.PASTE, 1)))) {
                if (recipe instanceof BrewingRecipe) reported++;
            }
            boolean used = type == LabMaterials.of(LabKind.PASTE);
            plugin.getLogger().info("Lab: brewing input " + type + (used ? " registered" : " not used yet")
                    + ", Bukkit reports " + (used ? reported : 0) + " brewing recipes for it");
        }
    }

    @EventHandler
    public void onStart(BrewingStartEvent event) {
        LabData top = LabItems.read(event.getSource()).orElse(null);
        if (top == null) return;
        // read once here and kept in the stand until this brew is done
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
        LabData top = LabItems.read(inventory.getIngredient()).orElse(null);
        if (top == null || !isReagent(top.kind())) return; // anything without lab items stays vanilla
        LabData[] slots = new LabData[3];
        for (int i = 0; i < 3; i++) slots[i] = LabSlots.read(inventory.getItem(i));
        LabUpgrades used = LabUpgrades.NONE;
        if (event.getBlock().getState(false) instanceof BrewingStand stand) {
            used = upgrades.stored(stand.getPersistentDataContainer());
            upgrades.clear(stand.getPersistentDataContainer());
        }
        LabChain.Brew brew = chain.brew(top, 1, slots, ThreadLocalRandom.current(), used.goodsFactor(labSettings));
        List<ItemStack> results = event.getResults();
        for (int i = 0; i < 3 && i < results.size(); i++) {
            ItemStack before = inventory.getItem(i);
            results.set(i, brew.rolled()[i] ? LabSlots.toItem(brew.slots()[i])
                    : before == null ? new ItemStack(Material.AIR) : before.clone());
        }
    }

    private static boolean isReagent(LabKind kind) {
        return kind.filterTier() > 0 || kind == LabKind.FEENSTAUB;
    }
}
