package com.benschnorr.survival.featrues.lab.upgrade;

import com.benschnorr.survival.featrues.lab.LabSettings;
import com.benschnorr.survival.featrues.lab.LabSettings.Yield;

/**
 * Which upgrades a stand has and what they do, with no server in it. Each type counts once, however
 * many blocks of it stand within reach. see knowledge: /capabilities/lab-upgrades.md
 *
 * @param water        a cauldron holding water: raises the goods chance
 * @param amethyst     an amethyst block: raises the goods chance
 * @param lightningRod a lightning rod: shortens a brew
 * @param smoker       a smoker: one fuel item lasts more brews
 */
public record LabUpgrades(boolean water, boolean amethyst, boolean lightningRod, boolean smoker) {

    public static final LabUpgrades NONE = new LabUpgrades(false, false, false, false);

    /** The upgrades as one small number, to keep with a running brew. */
    public int bits() {
        return (water ? 1 : 0) | (amethyst ? 2 : 0) | (lightningRod ? 4 : 0) | (smoker ? 8 : 0);
    }

    public static LabUpgrades fromBits(int bits) {
        return new LabUpgrades((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
    }

    /** What every goods chance is multiplied by. */
    public double goodsFactor(LabSettings settings) {
        return (water ? settings.waterCauldronFactor() : 1) * (amethyst ? settings.amethystFactor() : 1);
    }

    /** Brews one fuel item lasts in vanilla, the reference of the configured numbers. */
    private static final int VANILLA_BREWS = 20;

    /** A brew of {@code base} ticks with these upgrades. */
    public int brewTicks(int base, LabSettings settings) {
        return Math.max(1, (int) Math.round(base * (lightningRod ? settings.lightningRodTimeFactor() : 1)));
    }

    /**
     * The fuel a fuel item gives. Every brew costs one level, so more levels mean more brews per item.
     *
     * @param base what the item gives in vanilla (20 for blaze powder); the configured brews per item
     *             scale it, 20 brews being vanilla
     */
    public int fuelPower(int base, LabSettings settings) {
        int brews = smoker ? settings.smokerFuelBrews() : settings.fuelBrews();
        return Math.max(1, (int) Math.round(base * brews / (double) VANILLA_BREWS));
    }

    /**
     * Multiplies the goods chance, never past 100 percent. The extra comes out of waste and nothing
     * evenly; when one of them cannot give its half, the other gives the rest. Only waste is kept in the
     * result, because "nothing" is whatever goods and waste leave over.
     */
    public static Yield scale(Yield yield, double factor) {
        if (factor <= 1) return yield;
        double goods = Math.min(1, yield.goods() * factor);
        double extra = goods - yield.goods();
        double fromNothing = Math.min(yield.nothing(), extra / 2);
        double fromWaste = Math.min(yield.waste(), extra - fromNothing);
        return new Yield(goods, Math.max(0, yield.waste() - fromWaste));
    }
}
