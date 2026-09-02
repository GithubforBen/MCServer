package de.hems.types.cosmetic;

import java.util.ArrayList;
import java.util.List;

/**
 * The cosmetics that ship with the network.
 * <p>
 * This is the list a fresh launcher writes into {@code cosmetics.yml} the first time it starts. After that
 * the file is the truth - an admin who changed a price or switched something off should not have that
 * quietly overwritten on the next update - and this only fills in what is new.
 * <p>
 * Adding one is two steps: an entry here, and the code that plays it on the server that plays it. Until the
 * second step exists it simply does nothing, which is a safe state to ship in.
 */
public final class Cosmetics {

    /** Rockets that go off over whoever won. The one everybody starts with. */
    public static final String WIN_ROCKETS = "win-rockets";
    /** Explosions raining down over the map from the build limit. */
    public static final String WIN_INK = "win-ink";
    /** An ender pearl that is not used up, at the price of a longer cooldown. */
    public static final String GADGET_ENDLESS_PEARL = "endless-pearl";

    /** How much longer the endless pearl's cooldown is than a normal one, in ticks. */
    public static final String SETTING_COOLDOWN_TICKS = "cooldown-ticks";

    private Cosmetics() {
    }

    /**
     * @return every cosmetic the network is delivered with
     */
    public static List<CosmeticData> shipped() {
        List<CosmeticData> shipped = new ArrayList<>();

        CosmeticData rockets = new CosmeticData(WIN_ROCKETS, CosmeticType.WIN_EFFECT,
                "Raketen", "Feuerwerk steigt über dir auf, wenn du die Runde gewinnst",
                "FIREWORK_ROCKET", 0, true);
        shipped.add(rockets);

        CosmeticData ink = new CosmeticData(WIN_INK, CosmeticType.WIN_EFFECT,
                "Tinte", "Von der Bauhöhe regnet es Explosionen über die ganze Map - laut, aber harmlos",
                "INK_SAC", 2500, false);
        shipped.add(ink);

        CosmeticData pearl = new CosmeticData(GADGET_ENDLESS_PEARL, CosmeticType.GADGET,
                "Endlos-Perle", "Deine Enderperle wird nicht verbraucht, hat dafür etwas mehr Cooldown",
                "ENDER_PEARL", 5000, false);
        // vanilla is 20 ticks; ten percent on top is what the gadget pays for never running out
        pearl.getSettings().put(SETTING_COOLDOWN_TICKS, "22");
        shipped.add(pearl);

        return shipped;
    }
}
