package de.schnorrenbergers.survival.featrues.lab.craft;

import org.bukkit.plugin.Plugin;

/** Entry point of lab crafting. */
public final class LabCrafting {

    private LabCrafting() {
    }

    public static void init(Plugin plugin) {
        LabRecipes.init(plugin);
    }
}
