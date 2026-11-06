package com.benschnorr.survival.featrues.lab.craft;

import com.benschnorr.survival.featrues.lab.item.LabData;
import com.benschnorr.survival.featrues.lab.item.LabItems;
import com.benschnorr.survival.featrues.lab.item.LabKind;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers the filter and Feenstaub recipes. They are only added to the server, never discovered or
 * granted, so nobody gets a recipe notification. Lab ingredients are matched exactly, so plain paper never
 * passes as a Papierfilter.
 * <p>
 * Papierfilter is shaped (a row of three paper); the others are shapeless, which a crafter fills in any
 * order. All of them work in a crafting table and in a crafter.
 */
public final class LabRecipes {

    private static final Set<NamespacedKey> KEYS = new HashSet<>();

    private LabRecipes() {
    }

    /** Registers all lab recipes; call once from plugin startup. */
    public static void init(Plugin plugin) {
        KEYS.clear();

        ShapedRecipe paper = new ShapedRecipe(key(plugin, "papierfilter"),
                LabItems.create(LabData.of(LabKind.PAPIERFILTER), 8));
        paper.shape("PPP");
        paper.setIngredient('P', Material.PAPER);
        add(paper);

        ShapelessRecipe coal = base(plugin, "kohlefilter", LabKind.KOHLEFILTER);
        coal.addIngredient(exact(LabKind.PAPIERFILTER));
        coal.addIngredient(Material.CHARCOAL);
        add(coal);

        ShapelessRecipe silk = base(plugin, "seidenfilter", LabKind.SEIDENFILTER);
        silk.addIngredient(exact(LabKind.KOHLEFILTER));
        for (int i = 0; i < 4; i++) silk.addIngredient(Material.STRING);
        add(silk);

        ShapelessRecipe gold = base(plugin, "goldfilter", LabKind.GOLDFILTER);
        gold.addIngredient(exact(LabKind.SEIDENFILTER));
        for (int i = 0; i < 8; i++) gold.addIngredient(Material.GOLD_NUGGET);
        add(gold);

        ShapelessRecipe dust = base(plugin, "feenstaub", LabKind.FEENSTAUB);
        dust.addIngredient(Material.GLOWSTONE_DUST);
        dust.addIngredient(Material.SUGAR);
        add(dust);
    }

    /** Whether this recipe is one of the lab recipes. */
    public static boolean isLabRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed && KEYS.contains(keyed.getKey());
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        NamespacedKey key = new NamespacedKey(plugin, "lab_" + name);
        KEYS.add(key);
        return key;
    }

    private static RecipeChoice exact(LabKind kind) {
        return new RecipeChoice.ExactChoice(LabItems.create(LabData.of(kind)));
    }

    private static ShapelessRecipe base(Plugin plugin, String name, LabKind kind) {
        return new ShapelessRecipe(key(plugin, name), LabItems.create(LabData.of(kind)));
    }

    /** Added without discovering it for anyone. */
    private static void add(Recipe recipe) {
        Bukkit.addRecipe(recipe, false);
    }
}
