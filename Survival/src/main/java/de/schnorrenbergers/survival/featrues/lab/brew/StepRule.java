package de.schnorrenbergers.survival.featrues.lab.brew;

import org.bukkit.inventory.ItemStack;

import java.util.function.BiPredicate;

/**
 * The one question the hopper rule asks: is the top ingredient a valid next step for this item?
 * <p>
 * Until {@code LabPurifying} sets {@link #rule} from the chain, the answer is no. Cutting adds its own
 * steps to that rule later.
 */
public final class StepRule {

    /** (top ingredient, item in a bottom slot) to whether the ingredient still works on the item. */
    public static volatile BiPredicate<ItemStack, ItemStack> rule = (ingredient, item) -> false;

    private StepRule() {
    }

    public static boolean isValidStep(ItemStack ingredient, ItemStack item) {
        if (ingredient == null || item == null) return false;
        // a signed item is never a valid step, so a hopper below can always pull it out
        if (LabSlots.signed(item) || LabSlots.signed(ingredient)) return false;
        return rule.test(ingredient, item);
    }
}
