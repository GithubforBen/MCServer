package com.benschnorr.survival.featrues.lab.upgrade;

import com.benschnorr.survival.featrues.lab.LabSettings;
import com.benschnorr.survival.featrues.lab.item.LabItems;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

/**
 * A smoker near a lab stand makes one fuel item give more fuel, and every brew, on the own tick and on
 * the vanilla engine alike, costs one level. Only a stand set up for the lab gets it, so a stand with
 * ordinary potions is refuelled as vanilla does.
 * see knowledge: /capabilities/lab-upgrades.md
 */
public final class LabFuel implements Listener {

    private final LabSettings settings;
    private final LabUpgradeReader reader;

    public LabFuel(LabSettings settings, LabUpgradeReader reader) {
        this.settings = settings;
        this.reader = reader;
    }

    @EventHandler
    public void onFuel(BrewingStandFuelEvent event) {
        if (!(event.getBlock().getState(false) instanceof BrewingStand stand)) return;
        if (!isLabStand(stand.getInventory())) return;
        LabUpgrades upgrades = reader.scan(event.getBlock());
        event.setFuelPower(upgrades.fuelPower(event.getFuelPower(), settings));
    }

    private static boolean isLabStand(BrewerInventory inventory) {
        if (isLab(inventory.getIngredient())) return true;
        for (int i = 0; i < 3; i++) {
            if (LabItems.read(inventory.getItem(i)).isPresent()) return true;
        }
        return false;
    }

    private static boolean isLab(ItemStack item) {
        return item != null && (item.getType() == Material.SUGAR_CANE || LabItems.read(item).isPresent());
    }
}
