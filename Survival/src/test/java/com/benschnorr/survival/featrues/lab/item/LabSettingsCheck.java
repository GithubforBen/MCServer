package com.benschnorr.survival.featrues.lab.item;

import com.benschnorr.survival.featrues.lab.LabSettings;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Checks that a missing {@code lab.yml} is created with every default and that an existing
 * {@code brew-seconds} survives. Needs paper-api and its libraries on the classpath, for example from
 * {@code ./mvnw -pl Survival dependency:build-classpath}. Exits non-zero when something is wrong.
 */
public final class LabSettingsCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("lab-settings").toFile();
        File file = new File(dir, "configs/lab.yml");
        Logger logger = Logger.getLogger("check");

        LabSettings fresh = new LabSettings(file, logger);
        check("file created", file.exists(), true);
        YamlConfiguration written = YamlConfiguration.loadConfiguration(file);
        for (String key : new String[]{"yield.sugar-cane.goods", "yield.sugar-cane.waste", "yield.papierfilter.goods",
                "yield.kohlefilter.goods", "yield.seidenfilter.goods", "yield.goldfilter.goods",
                "yield.goldfilter.waste", "yield.kokain.goods", "yield.kokain.waste",
                "upgrade.water-cauldron-factor", "upgrade.amethyst-block-factor",
                "upgrade.lightning-rod-time-factor", "upgrade.smoker-brews-per-item", "fuel.brews-per-item",
                "consume.combat-seconds", "consume.cooldown-seconds"}) {
            check("written " + key, written.contains(key), true);
        }
        check("sugar cane goods", fresh.sugarCane().goods(), 0.48);
        check("sugar cane nothing", Math.round(fresh.sugarCane().nothing() * 100), 20L);
        check("papierfilter", fresh.filter(1).goods(), 0.35);
        check("goldfilter waste", fresh.filter(4).waste(), 0.56);
        check("kokain goods", fresh.kokain().goods(), 0.83);
        check("combat window", fresh.combatSeconds(), 15);
        check("cooldown", fresh.cooldownSeconds(), 60);
        check("fuel brews", fresh.fuelBrews(), 20);
        check("smoker brews", fresh.smokerFuelBrews(), 30);

        // a file that has brew-seconds and a changed value keeps both, and gains the rest
        File other = new File(dir, "other/lab.yml");
        other.getParentFile().mkdirs();
        YamlConfiguration seeded = new YamlConfiguration();
        seeded.set("brew-seconds", 33);
        seeded.set("consume.cooldown-seconds", 5);
        seeded.save(other);
        LabSettings kept = new LabSettings(other, logger);
        YamlConfiguration after = YamlConfiguration.loadConfiguration(other);
        check("brew-seconds kept", after.getInt("brew-seconds"), 33);
        check("changed value kept", kept.cooldownSeconds(), 5);
        check("rest written", after.contains("yield.kokain.goods"), true);

        // the file is stable: reading again changes nothing
        String first = Files.readString(file.toPath());
        new LabSettings(file, logger);
        check("second read leaves the file alone", Files.readString(file.toPath()), first);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
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
