package de.schnorrenbergers.survival.featrues.lab.brew;

import de.schnorrenbergers.survival.featrues.lab.chain.LabChain;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Translates between bottom slot items and the pure chain's view of them. */
final class LabSlots {

    private LabSlots() {
    }

    /** Null for an empty slot, the lab content, or {@link LabChain#OCCUPIED} for any other item. */
    static LabData read(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return LabItems.read(item).orElse(LabChain.OCCUPIED);
    }

    /** A new item for a rolled slot; null becomes air. */
    static ItemStack toItem(LabData data) {
        return data == null ? new ItemStack(Material.AIR) : LabItems.create(data);
    }
}
