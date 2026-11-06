package com.benschnorr.survival.featrues.lab.brew;

import com.benschnorr.survival.featrues.lab.chain.LabChain;
import com.benschnorr.survival.featrues.lab.item.LabData;
import com.benschnorr.survival.featrues.lab.item.LabItems;
import com.benschnorr.survival.featrues.signing.SignItemCommand;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Translates between bottom slot items and the pure chain's view of them. */
final class LabSlots {

    private LabSlots() {
    }

    /** Whether the item carries a signature; signed items are never consumed or changed by a brew. */
    static boolean signed(ItemStack item) {
        return SignItemCommand.signerOf(item) != null;
    }

    /** Null for an empty slot, the lab content, or {@link LabChain#OCCUPIED} for any other or a signed item. */
    static LabData read(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (signed(item)) return LabChain.OCCUPIED;
        return LabItems.read(item).orElse(LabChain.OCCUPIED);
    }

    /** A new item for a rolled slot; null becomes air. */
    static ItemStack toItem(LabData data) {
        return data == null ? new ItemStack(Material.AIR) : LabItems.create(data);
    }
}
