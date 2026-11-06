package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.featrues.Shopkeeper.SkinName.Kind;

/**
 * Checks how a typed skin answer is read. Plain main, no framework; exits non-zero on failure.
 * <pre>
 * javac -d /tmp/skincheck \
 *   Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/SkinName.java \
 *   Survival/src/test/java/de/schnorrenbergers/survival/featrues/Shopkeeper/SkinNameCheck.java
 * java -cp /tmp/skincheck de.schnorrenbergers.survival.featrues.Shopkeeper.SkinNameCheck
 * </pre>
 */
public final class SkinNameCheck {
    static int failed = 0, passed = 0;

    static void expect(String name, String input, Kind kind, String playerName) {
        SkinName.Result r = SkinName.parse(input);
        boolean ok = r.kind() == kind && java.util.Objects.equals(r.name(), playerName);
        if (!ok) {
            System.out.println("FAIL " + name + ": expected " + kind + "/" + playerName + " got " + r);
            failed++;
        } else passed++;
    }

    public static void main(String[] args) {
        // spec: shop-skin#setting-a-skin
        // (the reading of the name only; the figure swap needs the game)
        expect("Notch", "Notch", Kind.NAME, "Notch");
        expect("padded", "  Notch ", Kind.NAME, "Notch");
        expect("underscore and numbers", "A_b1", Kind.NAME, "A_b1");
        expect("three chars", "abc", Kind.NAME, "abc");
        expect("sixteen chars", "abcdefghijklmnop", Kind.NAME, "abcdefghijklmnop");
        // spec: shop-skin#invalid-name
        expect("spaces and bang", "not a name!", Kind.INVALID, null);
        expect("too short", "ab", Kind.INVALID, null);
        expect("too long", "abcdefghijklmnopq", Kind.INVALID, null);
        expect("empty", "", Kind.INVALID, null);
        expect("blank", "   ", Kind.INVALID, null);
        expect("null", null, Kind.INVALID, null);
        expect("dash", "no-pe", Kind.INVALID, null);
        // spec: shop-skin#removing-the-skin
        // (the reading of the word only; the villager coming back needs the game)
        expect("aus", "aus", Kind.REMOVE, null);
        expect("AUS padded", " AUS ", Kind.REMOVE, null);
        // spec: shop-skin#cancelling
        expect("abbrechen", "abbrechen", Kind.CANCEL, null);
        expect("Abbrechen padded", " Abbrechen ", Kind.CANCEL, null);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
