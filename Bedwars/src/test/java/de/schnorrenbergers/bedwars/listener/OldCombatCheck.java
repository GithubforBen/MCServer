package de.schnorrenbergers.bedwars.listener;

import org.bukkit.Material;

/**
 * Checks the numbers of {@link OldCombatListener} against what 1.8 dealt.
 * <p>
 * A class with a main rather than a test framework, like the checks of the launcher. From the repository
 * root, after {@code ./mvnw -q -pl Bedwars -am install -DskipTests}:
 * <pre>
 * ./mvnw -q -pl Bedwars dependency:build-classpath -Dmdep.outputFile=cp.txt
 * javac -cp "$(cat Bedwars/cp.txt):Bedwars/target/classes" -d /tmp/occ \
 *       Bedwars/src/test/java/de/schnorrenbergers/bedwars/listener/OldCombatCheck.java
 * java -cp "$(cat Bedwars/cp.txt):Bedwars/target/classes:/tmp/occ" \
 *      de.schnorrenbergers.bedwars.listener.OldCombatCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class OldCombatCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        // what a weapon deals today, and what it dealt in 1.8
        weapon(Material.WOODEN_SWORD, 4, 5);
        weapon(Material.STONE_SWORD, 5, 6);
        weapon(Material.IRON_SWORD, 6, 7);
        weapon(Material.DIAMOND_SWORD, 7, 8);
        weapon(Material.WOODEN_AXE, 7, 4);
        weapon(Material.STONE_AXE, 9, 5);
        weapon(Material.IRON_AXE, 9, 6);
        weapon(Material.DIAMOND_AXE, 9, 7);
        weapon(Material.WOODEN_PICKAXE, 2, 3);
        weapon(Material.DIAMOND_PICKAXE, 5, 6);
        weapon(Material.IRON_SHOVEL, 4.5, 4);
        weapon(Material.AIR, 1, 1);
        weapon(Material.SHEARS, 1, 1);

        near(OldCombatListener.damage(7, Material.DIAMOND_SWORD, 0, true), 12, "a diamond sword crits for 12");
        near(OldCombatListener.damage(6, Material.IRON_SWORD, 1, false), 8.25, "sharpness I adds 1.25");
        near(OldCombatListener.damage(6, Material.IRON_SWORD, 2, true), 13, "sharpness is not part of the crit");

        near(OldCombatListener.armorOff(8, 7), 2.24, "leather takes 28 percent off");
        near(OldCombatListener.armorOff(8, 20), 6.4, "diamond takes 80 percent off");
        near(OldCombatListener.armorOff(8, 30), 6.4, "more than 20 points count as 20");
        near(OldCombatListener.armorOff(8, 0), 0, "no armour takes nothing off");

        near(OldCombatListener.hitLift(-0.0784), 0.3608, "a hit on the ground lifts by 0.3608");
        near(OldCombatListener.hitLift(0.0), 0.4, "a hit in the air lifts as well");
        near(OldCombatListener.hitLift(0.42), 0.4, "and never by more than 0.4");
        near(OldCombatListener.hitLift(-1.0), -0.1, "somebody falling fast keeps falling, slower");

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void weapon(Material material, double now, double then) {
        near(OldCombatListener.damage(now, material, 0, false), then, material + " deals " + then);
    }

    private static void near(double value, double expected, String what) {
        if (Math.abs(value - expected) < 1.0e-9d) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + what + ", got " + value);
        }
    }
}
