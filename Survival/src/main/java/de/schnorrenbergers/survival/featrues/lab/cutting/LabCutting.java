package de.schnorrenbergers.survival.featrues.lab.cutting;

import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabEffect;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Cutting and stretching of Kokain, with no server in it. An effect ingredient adds its effect, sugar
 * turns one item into three of one purity lower. Both always succeed; what cannot be done is no step.
 * see knowledge: /capabilities/lab-cutting.md
 */
public final class LabCutting {

    /** The vanilla items that work on Kokain from the top slot. */
    public enum Ingredient {
        GOLDEN_CARROT(Material.GOLDEN_CARROT, LabEffect.NIGHT_VISION),
        PHANTOM_MEMBRANE(Material.PHANTOM_MEMBRANE, LabEffect.JUMP_BOOST),
        FIRE_CHARGE(Material.FIRE_CHARGE, LabEffect.FIRE_RESISTANCE),
        HONEY_BOTTLE(Material.HONEY_BOTTLE, LabEffect.REGENERATION),
        MAGMA_CREAM(Material.MAGMA_CREAM, LabEffect.STRENGTH),
        /** No effect: stretching. */
        SUGAR(Material.SUGAR, null);

        private final Material material;
        private final LabEffect effect;

        Ingredient(Material material, LabEffect effect) {
            this.material = material;
            this.effect = effect;
        }

        public Material material() {
            return material;
        }

        /** The effect it cuts in, or null for sugar. */
        public LabEffect effect() {
            return effect;
        }
    }

    /**
     * What one Kokain became.
     *
     * @param data   the new content
     * @param amount how many items the slot holds afterwards
     */
    public record Result(LabData data, int amount) {
    }

    private LabCutting() {
    }

    public static Optional<Ingredient> ingredient(Material material) {
        for (Ingredient ingredient : Ingredient.values()) {
            if (ingredient.material == material) return Optional.of(ingredient);
        }
        return Optional.empty();
    }

    /** The result of {@code ingredient} on {@code bottom}, or empty when it is no valid step. */
    public static Optional<Result> apply(Ingredient ingredient, LabData bottom) {
        if (ingredient == null || bottom == null || bottom.kind() != LabKind.KOKAIN) return Optional.empty();
        if (ingredient.effect == null) {
            if (bottom.stretched() || bottom.purity() < 2) return Optional.empty();
            return Optional.of(new Result(bottom.withPurity(bottom.purity() - 1).asStretched(), 3));
        }
        if (bottom.effects().contains(ingredient.effect) || bottom.effects().size() >= LabData.MAX_EFFECTS) {
            return Optional.empty();
        }
        Set<LabEffect> effects = EnumSet.of(ingredient.effect);
        effects.addAll(bottom.effects());
        return Optional.of(new Result(bottom.withEffects(effects), 1));
    }

    public static boolean isStep(Ingredient ingredient, LabData bottom) {
        return apply(ingredient, bottom).isPresent();
    }
}
