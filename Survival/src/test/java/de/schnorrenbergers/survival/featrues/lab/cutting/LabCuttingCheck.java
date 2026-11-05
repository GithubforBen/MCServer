package de.schnorrenbergers.survival.featrues.lab.cutting;

import de.schnorrenbergers.survival.featrues.lab.cutting.LabCutting.Ingredient;
import de.schnorrenbergers.survival.featrues.lab.cutting.LabCutting.Result;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabEffect;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;

import java.util.Optional;
import java.util.Set;

/**
 * Checks cutting and stretching on plain data. Needs paper-api on the classpath (for Material). Exits
 * non-zero when something is wrong.
 */
public final class LabCuttingCheck {

    private static int failed;

    public static void main(String[] args) {
        LabData k3 = LabData.of(LabKind.KOKAIN, 3);

        // spec: lab-cutting#adding-an-effect
        Result carrot = LabCutting.apply(Ingredient.GOLDEN_CARROT, k3).orElseThrow();
        check("carrot purity", carrot.data().purity(), 3);
        check("carrot effects", carrot.data().effects(), Set.of(LabEffect.NIGHT_VISION));
        check("carrot amount", carrot.amount(), 1);
        check("carrot not stretched", carrot.data().stretched(), false);
        check("membrane", LabCutting.apply(Ingredient.PHANTOM_MEMBRANE, k3).orElseThrow().data().effects(),
                Set.of(LabEffect.JUMP_BOOST));
        check("fire", LabCutting.apply(Ingredient.FIRE_CHARGE, k3).orElseThrow().data().effects(),
                Set.of(LabEffect.FIRE_RESISTANCE));
        check("honey", LabCutting.apply(Ingredient.HONEY_BOTTLE, k3).orElseThrow().data().effects(),
                Set.of(LabEffect.REGENERATION));
        check("magma", LabCutting.apply(Ingredient.MAGMA_CREAM, k3).orElseThrow().data().effects(),
                Set.of(LabEffect.STRENGTH));

        // spec: lab-cutting#fourth-ingredient
        LabData three = k3.withEffects(Set.of(LabEffect.NIGHT_VISION, LabEffect.JUMP_BOOST, LabEffect.STRENGTH));
        check("fourth ingredient", LabCutting.apply(Ingredient.HONEY_BOTTLE, three), Optional.empty());
        check("fourth is no step", LabCutting.isStep(Ingredient.FIRE_CHARGE, three), false);

        // spec: lab-cutting#same-effect-twice
        check("same effect twice", LabCutting.apply(Ingredient.GOLDEN_CARROT, carrot.data()), Optional.empty());

        // spec: lab-cutting#order-does-not-matter
        LabData ab = cut(cut(k3, Ingredient.GOLDEN_CARROT), Ingredient.MAGMA_CREAM);
        LabData ba = cut(cut(k3, Ingredient.MAGMA_CREAM), Ingredient.GOLDEN_CARROT);
        check("order", ab, ba);
        check("order hash", ab.hashCode(), ba.hashCode());
        check("three effects fit", cut(ab, Ingredient.FIRE_CHARGE).effects().size(), 3);

        // spec: lab-cutting#stretching
        Result stretch = LabCutting.apply(Ingredient.SUGAR, LabData.of(LabKind.KOKAIN, 4)).orElseThrow();
        check("stretch amount", stretch.amount(), 3);
        check("stretch purity", stretch.data().purity(), 3);
        check("stretch marked", stretch.data().stretched(), true);
        Result keeps = LabCutting.apply(Ingredient.SUGAR, carrot.data().withPurity(5)).orElseThrow();
        check("stretch keeps effects", keeps.data().effects(), Set.of(LabEffect.NIGHT_VISION));
        check("stretched differs from plain", stretch.data().equals(LabData.of(LabKind.KOKAIN, 3)), false);

        // spec: lab-cutting#no-second-stretch
        check("second stretch", LabCutting.apply(Ingredient.SUGAR, stretch.data()), Optional.empty());

        // spec: lab-cutting#lowest-purity
        check("lowest purity", LabCutting.apply(Ingredient.SUGAR, LabData.of(LabKind.KOKAIN, 1)), Optional.empty());

        // only Kokain is cut
        check("paste not cut", LabCutting.isStep(Ingredient.GOLDEN_CARROT, LabData.of(LabKind.PASTE, 3)), false);
        check("nothing not cut", LabCutting.isStep(Ingredient.SUGAR, null), false);

        if (failed > 0) {
            System.out.println(failed + " checks failed");
            System.exit(1);
        }
        System.out.println("LabCuttingCheck ok");
    }

    private static LabData cut(LabData data, Ingredient ingredient) {
        return LabCutting.apply(ingredient, data).orElseThrow().data();
    }

    private static void check(String what, Object actual, Object expected) {
        if (!actual.equals(expected)) {
            System.out.println("FAIL " + what + ": " + actual + " != " + expected);
            failed++;
        }
    }
}
