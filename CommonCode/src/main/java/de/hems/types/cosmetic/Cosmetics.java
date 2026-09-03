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
    /** A thunderstorm over the winners, all noise and no damage. */
    public static final String WIN_STORM = "win-storm";
    /** A tower of light that grows out of every winner. */
    public static final String WIN_BEACON = "win-beacon";

    /** A bolt of lightning where the loser stood. */
    public static final String KILL_LIGHTNING = "kill-lightning";
    /** The soul of whoever fell, drifting up out of them. */
    public static final String KILL_SOULS = "kill-souls";
    /** A ring of flame where they went down. */
    public static final String KILL_BLAST = "kill-blast";

    /** Flames behind whoever is wearing it. */
    public static final String TRAIL_FLAME = "trail-flame";
    /** Small white sparks, the quiet one. */
    public static final String TRAIL_STARDUST = "trail-stardust";
    /** Notes, for people who want to be seen coming. */
    public static final String TRAIL_NOTES = "trail-notes";

    /** An ender pearl that is not used up, at the price of a longer cooldown. */
    public static final String GADGET_ENDLESS_PEARL = "endless-pearl";
    /** A fishing rod that pulls its owner to where the hook landed. */
    public static final String GADGET_GRAPPLE = "grappling-hook";

    /** How much longer the endless pearl's cooldown is than a normal one, in ticks. */
    public static final String SETTING_COOLDOWN_TICKS = "cooldown-ticks";
    /** How hard a gadget throws its owner, in tenths of a block per tick. */
    public static final String SETTING_POWER = "power";
    /** How long an effect keeps going, in ticks. */
    public static final String SETTING_DURATION_TICKS = "duration-ticks";

    private Cosmetics() {
    }

    /**
     * @return every cosmetic the network is delivered with
     */
    public static List<CosmeticData> shipped() {
        List<CosmeticData> shipped = new ArrayList<>();

        shipped.add(new CosmeticData(WIN_ROCKETS, CosmeticType.WIN_EFFECT,
                "Raketen", "Feuerwerk steigt über dir auf, wenn du die Runde gewinnst",
                "FIREWORK_ROCKET", 0, true));

        shipped.add(new CosmeticData(WIN_INK, CosmeticType.WIN_EFFECT,
                "Tinte", "Von der Bauhöhe regnet es Explosionen über die ganze Map - laut, aber harmlos",
                "INK_SAC", 2500, false));

        shipped.add(new CosmeticData(WIN_STORM, CosmeticType.WIN_EFFECT,
                "Gewitter", "Blitze schlagen um dich herum ein, ohne dir oder sonst wem etwas zu tun",
                "LIGHTNING_ROD", 3000, false));

        shipped.add(new CosmeticData(WIN_BEACON, CosmeticType.WIN_EFFECT,
                "Lichtsäule", "Eine Säule aus Licht wächst aus dir heraus bis über die Map",
                "BEACON", 4000, false));

        shipped.add(new CosmeticData(KILL_LIGHTNING, CosmeticType.KILL_EFFECT,
                "Blitzschlag", "Wo dein Gegner stand, schlägt ein Blitz ein",
                "LIGHTNING_ROD", 1500, false));

        shipped.add(new CosmeticData(KILL_SOULS, CosmeticType.KILL_EFFECT,
                "Seelen", "Die Seele deines Gegners steigt aus ihm auf",
                "SOUL_LANTERN", 1500, false));

        shipped.add(new CosmeticData(KILL_BLAST, CosmeticType.KILL_EFFECT,
                "Stichflamme", "Ein Ring aus Feuer da, wo dein Gegner umgefallen ist",
                "BLAZE_POWDER", 2000, false));

        shipped.add(new CosmeticData(TRAIL_FLAME, CosmeticType.TRAIL,
                "Flammenspur", "Flammen ziehen hinter dir her, solange du läufst",
                "BLAZE_POWDER", 1200, false));

        shipped.add(new CosmeticData(TRAIL_STARDUST, CosmeticType.TRAIL,
                "Sternenstaub", "Helle Funken, die langsam hinter dir absinken",
                "END_ROD", 1200, false));

        shipped.add(new CosmeticData(TRAIL_NOTES, CosmeticType.TRAIL,
                "Noten", "Bunte Noten, damit man dich kommen sieht",
                "NOTE_BLOCK", 1800, false));

        CosmeticData pearl = new CosmeticData(GADGET_ENDLESS_PEARL, CosmeticType.GADGET,
                "Endlos-Perle", "Deine Enderperle wird nicht verbraucht, hat dafür etwas mehr Cooldown",
                "ENDER_PEARL", 5000, false);
        // vanilla is 20 ticks; ten percent on top is what the gadget pays for never running out
        pearl.getSettings().put(SETTING_COOLDOWN_TICKS, "22");
        shipped.add(pearl);

        CosmeticData grapple = new CosmeticData(GADGET_GRAPPLE, CosmeticType.GADGET,
                "Enterhaken", "Eine Angel, die dich dorthin zieht, wo der Haken gelandet ist",
                "FISHING_ROD", 5000, false);
        grapple.getSettings().put(SETTING_POWER, "12");
        grapple.getSettings().put(SETTING_COOLDOWN_TICKS, "60");
        shipped.add(grapple);

        return shipped;
    }
}
