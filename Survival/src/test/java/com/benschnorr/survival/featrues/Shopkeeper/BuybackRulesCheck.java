package com.benschnorr.survival.featrues.Shopkeeper;

import com.benschnorr.survival.featrues.Shopkeeper.BuybackRules.Refusal;

/**
 * Checks every refusal of a buyback sale. Plain main, no framework; exits non-zero on failure.
 * <pre>
 * javac -d /tmp/bbcheck \
 *   Survival/src/main/java/com/benschnorr/survival/featrues/Shopkeeper/BuybackRules.java \
 *   Survival/src/test/java/com/benschnorr/survival/featrues/Shopkeeper/BuybackRulesCheck.java
 * java -cp /tmp/bbcheck com.benschnorr.survival.featrues.Shopkeeper.BuybackRulesCheck
 * </pre>
 */
public final class BuybackRulesCheck {
    static int failed = 0, passed = 0;

    static void expect(String name, Refusal expected, Refusal actual) {
        if (expected != actual) {
            System.out.println("FAIL " + name + ": expected " + expected + " got " + actual);
            failed++;
        } else passed++;
    }

    public static void main(String[] args) {
        // spec: shop-buyback#selling-a-diamond-block
        // spec: shop-buyback#member-of-the-owning-team-sells
        // (only the start of the sale; the rules do not look at who sells, so a team member passes too)
        expect("diamond block", null, BuybackRules.check(true, true, true, true, 1, 1, 64, 5000, 900));
        expect("exact balance", null, BuybackRules.check(true, true, true, true, 1, 1, 64, 900, 900));
        // spec: shop-buyback#offer-without-buyback
        expect("buyback off", Refusal.BUYBACK_OFF, BuybackRules.check(true, false, true, true, 1, 1, 64, 5000, 900));
        // spec: shop-buyback#team-cannot-pay
        expect("team short", Refusal.TEAM_CANNOT_PAY, BuybackRules.check(true, true, true, true, 1, 1, 64, 899, 900));
        // spec: shop-buyback#chest-is-full
        expect("chest full", Refusal.CHEST_FULL, BuybackRules.check(true, true, true, true, 4, 4, 3, 5000, 900));
        // spec: shop-buyback#item-only-looks-the-same
        // the caller counts only identical items, so a renamed one leaves heldMatching at 0
        expect("not identical", Refusal.NOT_ENOUGH_GOODS, BuybackRules.check(true, true, true, true, 1, 0, 64, 5000, 900));
        expect("too few for lot", Refusal.NOT_ENOUGH_GOODS, BuybackRules.check(true, true, true, true, 4, 3, 64, 5000, 900));
        expect("broken offer", Refusal.BROKEN_OFFER, BuybackRules.check(false, true, true, true, 1, 1, 64, 5000, 900));
        expect("no chest", Refusal.NO_CHEST, BuybackRules.check(true, true, false, true, 1, 1, 64, 5000, 900));
        expect("no team", Refusal.NO_TEAM, BuybackRules.check(true, true, true, false, 1, 1, 64, 5000, 900));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
