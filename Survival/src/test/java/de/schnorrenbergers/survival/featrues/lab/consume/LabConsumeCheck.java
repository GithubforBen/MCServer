package de.schnorrenbergers.survival.featrues.lab.consume;

import de.schnorrenbergers.survival.featrues.lab.consume.LabConsumeRules.CombatClock;
import de.schnorrenbergers.survival.featrues.lab.consume.LabConsumeRules.Crash;
import de.schnorrenbergers.survival.featrues.lab.consume.LabConsumeRules.Grant;

/** Checks the consumption tables, the combat cap and the cooldown. No Bukkit needed. Exits non-zero on failure. */
public final class LabConsumeCheck {

    private static int failed;

    public static void main(String[] args) {
        // spec: lab-consumption#purity-3
        check("purity 3", LabConsumeRules.grant(3, false), new Grant(2, 1, 60));
        check("purity 1", LabConsumeRules.grant(1, false), new Grant(1, 0, 30));
        check("purity 2", LabConsumeRules.grant(2, false), new Grant(1, 1, 45));
        check("purity 4", LabConsumeRules.grant(4, false), new Grant(2, 2, 90));
        check("purity 5", LabConsumeRules.grant(5, false), new Grant(3, 3, 120));

        // spec: lab-consumption#cut-in-effect
        // cut-in effects last as long as the grant: purity 1 is 30 seconds
        check("cut-in duration", LabConsumeRules.grant(1, false).seconds(), 30);

        // spec: lab-consumption#crash-after-the-effects
        check("crash 5", LabConsumeRules.crash(5), new Crash(true, 2, 20));
        check("crash 1", LabConsumeRules.crash(1), new Crash(true, 0, 5));
        check("crash 2", LabConsumeRules.crash(2), new Crash(true, 0, 8));
        check("crash 3", LabConsumeRules.crash(3), new Crash(true, 1, 10));
        check("crash 4", LabConsumeRules.crash(4), new Crash(true, 1, 15));

        // spec: lab-consumption#too-soon
        check("cooldown at 30 s", LabConsumeRules.cooldownLeft(1000, 31000, 60), 30);
        check("cooldown rounds up", LabConsumeRules.cooldownLeft(1000, 31500, 60), 30);
        check("cooldown over", LabConsumeRules.cooldownLeft(1000, 61000, 60), 0);
        check("cooldown never", LabConsumeRules.cooldownLeft(LabConsumeRules.NEVER, 5, 60), 0);

        // spec: lab-consumption#consuming-in-combat
        CombatClock<String> clock = new CombatClock<>(15);
        clock.hit("a", 100_000);
        check("in combat after 5 s", clock.inCombat("a", 105_000), true);
        check("combat grant", LabConsumeRules.grant(5, clock.inCombat("a", 105_000)), new Grant(2, 2, 120));
        check("combat ends after 15 s", clock.inCombat("a", 115_000), false);
        check("other player not in combat", clock.inCombat("b", 105_000), false);

        // spec: lab-consumption#hit-during-purity-5
        check("cap III to II", LabConsumeRules.cap(3, true), 2);
        check("cap leaves I", LabConsumeRules.cap(1, true), 1);
        check("no cap out of combat", LabConsumeRules.cap(3, false), 3);

        // spec: lab-consumption#mob-hit-does-not-count
        check("zombie hit", LabConsumeRules.countsAsCombat(false, true, false), false);
        check("player hit", LabConsumeRules.countsAsCombat(true, true, false), true);
        check("self hit", LabConsumeRules.countsAsCombat(true, true, true), false);

        // telling Kokain's own effect from a potion or beacon
        check("own effect", LabConsumeRules.isOurs(2, 100_500, 2, 100_000, 2000), true);
        check("potion with other end", LabConsumeRules.isOurs(2, 400_000, 2, 100_000, 2000), false);
        check("beacon level", LabConsumeRules.isOurs(1, 100_000, 2, 100_000, 2000), false);
        check("crash due", LabConsumeRules.crashDue(10, 10), true);
        check("crash not due", LabConsumeRules.crashDue(11, 10), false);

        if (failed > 0) {
            System.err.println(failed + " check(s) failed");
            System.exit(1);
        }
        System.out.println("LabConsumeCheck ok");
    }

    private static void check(String name, Object actual, Object expected) {
        if (!actual.equals(expected)) {
            failed++;
            System.err.println("FAIL " + name + ": expected " + expected + " but was " + actual);
        }
    }
}
