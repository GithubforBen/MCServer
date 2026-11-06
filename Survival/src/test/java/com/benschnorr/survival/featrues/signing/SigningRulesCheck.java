package com.benschnorr.survival.featrues.signing;

import com.benschnorr.survival.featrues.signing.SigningRules.Outcome;

import java.util.UUID;

/**
 * Checks every outcome of {@code /signItem}. Plain main, no framework; exits non-zero on failure.
 * <pre>
 * javac -d /tmp/sigcheck \
 *   Survival/src/main/java/com/benschnorr/survival/featrues/signing/SigningRules.java \
 *   Survival/src/test/java/com/benschnorr/survival/featrues/signing/SigningRulesCheck.java
 * java -cp /tmp/sigcheck com.benschnorr.survival.featrues.signing.SigningRulesCheck
 * </pre>
 */
public final class SigningRulesCheck {
    static int failed = 0, passed = 0;

    static void expect(String name, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            System.out.println("FAIL " + name + ": expected " + expected + " got " + actual);
            failed++;
        } else passed++;
    }

    public static void main(String[] args) {
        UUID ben = UUID.randomUUID(), other = UUID.randomUUID();
        // spec: item-signing#empty-hand
        expect("empty hand", Outcome.EMPTY_HAND, SigningRules.decide(true, true, null, ben));
        // spec: item-signing#signing-twice
        expect("signing twice", Outcome.ALREADY_YOURS, SigningRules.decide(false, true, ben, ben));
        // spec: item-signing#someone-else-s-signature
        expect("someone else", Outcome.SIGNED_BY_OTHER, SigningRules.decide(false, true, other, ben));
        // spec: item-signing#gadget-item
        // (a gadget is "not signable"; the command maps the gadget marker to signable=false)
        expect("not signable", Outcome.NOT_SIGNABLE, SigningRules.decide(false, false, null, ben));
        expect("not signable, even if signed", Outcome.NOT_SIGNABLE, SigningRules.decide(false, false, other, ben));
        expect("unsigned", Outcome.SIGN, SigningRules.decide(false, true, null, ben));
        expect("lore text", "Signiert von Ben", SigningRules.loreLine("Ben"));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
