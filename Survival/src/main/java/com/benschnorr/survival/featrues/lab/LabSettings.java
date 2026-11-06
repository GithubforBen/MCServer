package com.benschnorr.survival.featrues.lab;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * The numbers of the lab, in {@code ./configs/lab.yml}, the same file {@code BrewSettings} writes
 * {@code brew-seconds} to. Missing values are written back with their defaults and a comment; values that
 * are already there, including {@code brew-seconds}, stay as they are.
 */
public final class LabSettings {

    /**
     * Chances of one rolled item. Whatever goods and waste leave over is "nothing".
     *
     * @param goods chance of the next purity, or of Kokain
     * @param waste chance of Pflanzenabfall
     */
    public record Yield(double goods, double waste) {
        public double nothing() {
            return Math.max(0, 1 - goods - waste);
        }
    }

    private static final String[] STEPS = {"sugar-cane", "papierfilter", "kohlefilter", "seidenfilter",
            "goldfilter", "kokain"};
    private static final String[] STEP_NAMES = {"sugar cane into Rohpaste", "Papierfilter on purity 1",
            "Kohlefilter on purity 2", "Seidenfilter on purity 3", "Goldfilter on purity 4",
            "Feenstaub, paste into Kokain"};
    private static final double[] GOODS = {0.48, 0.35, 0.15, 0.06, 0.04, 0.83};
    private static final double[] WASTE = {0.32, 0.40, 0.50, 0.54, 0.56, 0.12};

    private final Yield[] yields = new Yield[STEPS.length];
    private final double waterCauldronFactor;
    private final double amethystFactor;
    private final double lightningRodTimeFactor;
    private final int fuelBrews;
    private final int smokerFuelBrews;
    private final int combatSeconds;
    private final int cooldownSeconds;

    /**
     * Reads the file, writing back what was missing so it documents itself.
     *
     * @param file   where the settings are
     * @param logger where a file that cannot be written is reported
     */
    public LabSettings(File file, Logger logger) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;

        for (int i = 0; i < STEPS.length; i++) {
            changed |= define(config, "yield." + STEPS[i] + ".goods", GOODS[i],
                    "Chance of the good result for " + STEP_NAMES[i] + ", from 0 to 1.");
            changed |= define(config, "yield." + STEPS[i] + ".waste", WASTE[i],
                    "Chance of Pflanzenabfall for " + STEP_NAMES[i] + ". The rest is nothing.");
        }
        changed |= define(config, "upgrade.water-cauldron-factor", 1.1,
                "A water cauldron within two blocks multiplies every goods chance by this.");
        changed |= define(config, "upgrade.amethyst-block-factor", 1.1,
                "An amethyst block within two blocks multiplies every goods chance by this.");
        changed |= define(config, "upgrade.lightning-rod-time-factor", 0.75,
                "A lightning rod within two blocks multiplies the brew time by this.");
        changed |= define(config, "fuel.brews-per-item", 20,
                "How many lab brews one fuel item lasts.");
        changed |= define(config, "upgrade.smoker-brews-per-item", 30,
                "How many lab brews one fuel item lasts with a smoker within two blocks.");
        changed |= define(config, "consume.combat-seconds", 15,
                "How long after a hit to or from another player Kokain is capped at the vanilla level.");
        changed |= define(config, "consume.cooldown-seconds", 60,
                "How long a player must wait between two Kokain.");

        if (changed) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                config.save(file);
            } catch (IOException e) {
                logger.warning("Could not write " + file.getPath() + ": " + e.getMessage());
            }
        }

        for (int i = 0; i < STEPS.length; i++) {
            double goods = clamp(config.getDouble("yield." + STEPS[i] + ".goods", GOODS[i]));
            double waste = clamp(config.getDouble("yield." + STEPS[i] + ".waste", WASTE[i]));
            if (goods + waste > 1) waste = 1 - goods;
            yields[i] = new Yield(goods, waste);
        }
        waterCauldronFactor = Math.max(1, config.getDouble("upgrade.water-cauldron-factor", 1.1));
        amethystFactor = Math.max(1, config.getDouble("upgrade.amethyst-block-factor", 1.1));
        lightningRodTimeFactor = Math.min(1, Math.max(0.05, config.getDouble("upgrade.lightning-rod-time-factor", 0.75)));
        fuelBrews = Math.max(1, config.getInt("fuel.brews-per-item", 20));
        smokerFuelBrews = Math.max(1, config.getInt("upgrade.smoker-brews-per-item", 30));
        combatSeconds = Math.max(0, config.getInt("consume.combat-seconds", 15));
        cooldownSeconds = Math.max(0, config.getInt("consume.cooldown-seconds", 60));
    }

    private static boolean define(YamlConfiguration config, String key, Object value, String comment) {
        if (config.contains(key)) return false;
        config.set(key, value);
        config.setComments(key, List.of(comment));
        return true;
    }

    private static double clamp(double chance) {
        return Math.min(1, Math.max(0, chance));
    }

    /** Sugar cane into Rohpaste. */
    public Yield sugarCane() {
        return yields[0];
    }

    /** The filter of this tier (1 Papier, 2 Kohle, 3 Seide, 4 Gold) on paste of the same purity. */
    public Yield filter(int tier) {
        if (tier < 1 || tier > 4) throw new IllegalArgumentException("tier " + tier);
        return yields[tier];
    }

    /** Feenstaub on paste, into Kokain. */
    public Yield kokain() {
        return yields[5];
    }

    public double waterCauldronFactor() {
        return waterCauldronFactor;
    }

    public double amethystFactor() {
        return amethystFactor;
    }

    public double lightningRodTimeFactor() {
        return lightningRodTimeFactor;
    }

    /** Brews one fuel item lasts without upgrades. */
    public int fuelBrews() {
        return fuelBrews;
    }

    /** Brews one fuel item lasts with a smoker nearby. */
    public int smokerFuelBrews() {
        return smokerFuelBrews;
    }

    public int combatSeconds() {
        return combatSeconds;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }
}
