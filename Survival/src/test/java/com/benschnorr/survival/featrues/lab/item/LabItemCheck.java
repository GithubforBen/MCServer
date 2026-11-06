package com.benschnorr.survival.featrues.lab.item;

import java.util.List;
import java.util.Set;

/**
 * Checks the words on lab items and that equal content gives equal data. From the repository root:
 * <pre>
 * javac -d /tmp/lic Survival/src/main/java/com/benschnorr/survival/featrues/lab/item/{LabKind,LabEffect,LabData,LabText}.java \
 *       Survival/src/test/java/com/benschnorr/survival/featrues/lab/item/LabItemCheck.java
 * java -cp /tmp/lic com.benschnorr.survival.featrues.lab.item.LabItemCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class LabItemCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        nextStep();
        stacking();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void nextStep() {
        // spec: lab-brewing#item-names-the-next-step
        List<String> lore = LabText.lore(LabData.of(LabKind.PASTE, 1));
        check("purity 1 paste names the Papierfilter", lore.stream().anyMatch(l -> l.contains("Papierfilter")), true);
        check("purity 1 paste names its purity", lore.get(0).contains("Reinheit: 1"), true);
        check("purity 2 paste names the Kohlefilter",
                LabText.nextStep(LabData.of(LabKind.PASTE, 2)).contains("Kohlefilter"), true);
        check("purity 3 paste names the Seidenfilter",
                LabText.nextStep(LabData.of(LabKind.PASTE, 3)).contains("Seidenfilter"), true);
        check("purity 4 paste names the Goldfilter",
                LabText.nextStep(LabData.of(LabKind.PASTE, 4)).contains("Goldfilter"), true);
        check("purity 5 paste names Feenstaub",
                LabText.nextStep(LabData.of(LabKind.PASTE, 5)).contains("Feenstaub"), true);
        check("title", LabText.title(LabData.of(LabKind.PASTE, 1)), "Rohpaste (Dreckig)");
        check("top purity name", LabText.purityName(5), "Makellos");
        check("Kokain names consuming",
                LabText.nextStep(LabData.of(LabKind.KOKAIN, 3)).contains("Rechtsklick"), true);
        check("stretched Kokain does not offer stretching",
                LabText.nextStep(LabData.of(LabKind.KOKAIN, 3).asStretched()).contains("Zucker"), false);
        check("every kind has a next step", everyKindHasOne(), true);
    }

    private static boolean everyKindHasOne() {
        for (LabKind kind : LabKind.values()) {
            LabData data = kind.hasPurity() ? LabData.of(kind, 1) : LabData.of(kind);
            if (LabText.nextStep(data).isBlank()) return false;
        }
        return true;
    }

    private static void stacking() {
        LabData a = LabData.of(LabKind.KOKAIN, 3)
                .withEffects(List.of(LabEffect.NIGHT_VISION, LabEffect.STRENGTH));
        LabData b = LabData.of(LabKind.KOKAIN, 3)
                .withEffects(List.of(LabEffect.STRENGTH, LabEffect.NIGHT_VISION));
        check("effect order does not matter", a.equals(b), true);
        check("same lore", LabText.lore(a).equals(LabText.lore(b)), true);
        check("different purity differs", a.equals(a.withPurity(4)), false);
        check("stretched differs", a.equals(a.asStretched()), false);
        check("waste has no purity", LabData.of(LabKind.PFLANZENABFALL).purity(), 0);
        boolean refused = false;
        try {
            LabData.of(LabKind.KOKAIN, 1).withEffects(Set.of(LabEffect.values()));
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        check("more than three effects refused", refused, true);
    }

    private static void check(String name, Object actual, Object expected) {
        if (actual.equals(expected)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + ": expected " + expected + " but was " + actual);
        }
    }
}
