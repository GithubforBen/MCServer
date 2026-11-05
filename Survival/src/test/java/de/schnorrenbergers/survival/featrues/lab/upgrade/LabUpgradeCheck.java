package de.schnorrenbergers.survival.featrues.lab.upgrade;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.LabSettings.Yield;
import de.schnorrenbergers.survival.featrues.lab.chain.LabChain;
import de.schnorrenbergers.survival.featrues.lab.chain.LabChainCheck;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Checks the upgrade arithmetic and the fully upgraded average, using the seeded simulation of
 * {@link LabChainCheck}. Needs paper-api on the classpath (for the config reader). Whether the blocks
 * around a real stand are read right is proven on the server run, not here. Exits non-zero when
 * something is wrong.
 */
public final class LabUpgradeCheck {

    private static int failed;

    public static void main(String[] args) throws Exception {
        File file = new File(Files.createTempDirectory("lab-upgrade").toFile(), "configs/lab.yml");
        LabSettings settings = new LabSettings(file, Logger.getLogger("check"));
        LabChain chain = LabChain.of(settings);
        Yield cane = chain.caneYield();

        LabUpgrades water = new LabUpgrades(true, false, false, false);
        check("no upgrades, factor", LabUpgrades.NONE.goodsFactor(settings), 1.0);
        check("water cauldron, cane goods", Math.round(LabUpgrades.scale(cane, water.goodsFactor(settings)).goods() * 1000), 528L);

        // spec: lab-upgrades#two-cauldrons-count-once
        // two cauldrons are still one flag: the factor is the one cauldron's
        check("one cauldron factor", water.goodsFactor(settings), settings.waterCauldronFactor());
        check("both goods upgrades", Math.round(new LabUpgrades(true, true, false, false)
                .goodsFactor(settings) * 1000), 1210L);

        // spec: lab-upgrades#lightning-rod-shortens-a-brew
        check("brew with a rod", new LabUpgrades(false, false, true, false).brewTicks(400, settings), 300);
        check("brew without a rod", LabUpgrades.NONE.brewTicks(400, settings), 400);
        check("smoker fuel", new LabUpgrades(false, false, false, true).fuelPower(20, settings), 30);
        check("no smoker fuel", LabUpgrades.NONE.fuelPower(20, settings), 20);

        // the extra comes out of waste and nothing evenly, the sum stays 1
        Yield scaled = LabUpgrades.scale(cane, 1.21);
        near("scaled goods", scaled.goods(), 0.5808);
        near("scaled waste", scaled.waste(), 0.32 - 0.0504);
        near("scaled nothing", scaled.nothing(), 0.20 - 0.0504);
        // waste runs out: nothing gives the rest
        Yield thin = LabUpgrades.scale(new Yield(0.5, 0.02), 1.4);
        near("thin goods", thin.goods(), 0.7);
        near("thin waste", thin.waste(), 0);
        near("thin nothing", thin.nothing(), 0.3);
        // the chance never passes 100 percent
        Yield capped = LabUpgrades.scale(new Yield(0.83, 0.12), 1.21);
        check("capped goods", capped.goods(), 1.0);
        near("capped waste", capped.waste(), 0);
        near("capped nothing", capped.nothing(), 0);

        for (int bits = 0; bits < 16; bits++) check("bits " + bits, LabUpgrades.fromBits(bits).bits(), bits);

        fullyUpgraded(chain);

        if (failed > 0) {
            System.out.println(failed + " checks failed");
            System.exit(1);
        }
        System.out.println("LabUpgradeCheck ok");
    }

    // spec: lab-upgrades#fully-upgraded
    private static void fullyUpgraded(LabChain chain) {
        // both goods upgrades with the default 1.1 each; the lightning rod and the smoker leave the chance alone
        double perKokain = LabChainCheck.canePerKokain(chain, 1.1 * 1.1, 50_000_000L, 20261106L);
        System.out.println("fully upgraded: cane per kokain=" + perKokain);
        check("fully upgraded about 6400 (" + perKokain + ")", perKokain > 6_000 && perKokain < 6_800, true);
    }

    private static void near(String what, double actual, double expected) {
        check(what + " (" + actual + " vs " + expected + ")", Math.abs(actual - expected) < 1e-9, true);
    }

    private static void check(String what, Object actual, Object expected) {
        if (!expected.equals(actual)) {
            System.out.println("FAIL " + what + ": expected " + expected + " but was " + actual);
            failed++;
        }
    }
}
