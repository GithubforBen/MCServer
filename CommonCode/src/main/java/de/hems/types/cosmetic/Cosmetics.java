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
    /** A second jump, in mid air. */
    public static final String GADGET_DOUBLE_JUMP = "double-jump";
    /** A rocket that throws its owner into the air and sets them down again gently. */
    public static final String GADGET_ROCKET_BOOTS = "rocket-boots";
    /** Snowballs that shove people about and hurt nobody. */
    public static final String GADGET_SNOWBALL_CANNON = "snowball-cannon";
    /** A floor that lights up under its owner, for their neighbours only. */
    public static final String GADGET_DISCO_FLOOR = "disco-floor";
    /** Prints left and right where its owner walked. */
    public static final String GADGET_FOOTSTEPS = "footsteps";
    /** A pad that throws whoever steps on it, and is gone a few seconds later. */
    public static final String GADGET_JUMP_PAD = "jump-pad";
    /** A horse that comes when called. */
    public static final String GADGET_MOUNT = "lobby-mount";
    /** Harvests a ripe plant and puts a new one in its place. */
    public static final String GADGET_HARVEST_HELPER = "harvest-helper";
    /** Sitting down on stairs and slabs. */
    public static final String GADGET_SIT = "sit";
    /** A workbench that travels. */
    public static final String GADGET_WORKBENCH = "mobile-workbench";
    /** A small animal that walks after its owner. */
    public static final String GADGET_PET = "pet-companion";
    /** A balloon on a string over its owner's head. */
    public static final String GADGET_BALLOON = "balloon";
    /** A bang of colour, and nothing else. */
    public static final String GADGET_CONFETTI = "confetti-cannon";
    /** Its owner's own sky, seen by nobody else. */
    public static final String GADGET_WEATHER = "personal-weather";
    /** What its owner just said, over their head. */
    public static final String GADGET_CHAT_BUBBLE = "chat-bubble";
    /** A handful of gestures, out of a menu. */
    public static final String GADGET_EMOTES = "emote-wheel";

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

        shipped.add(gadget(GADGET_DOUBLE_JUMP, "Doppelsprung",
                "Ein zweiter Sprung in der Luft, und du landest sanft", "FEATHER", 1500,
                SETTING_POWER, "8", SETTING_DURATION_TICKS, "60"));

        shipped.add(gadget(GADGET_ROCKET_BOOTS, "Raketenstiefel",
                "Rechtsklick wirft dich hoch, runter kommst du langsam", "FIREWORK_ROCKET", 3000,
                SETTING_POWER, "14", SETTING_COOLDOWN_TICKS, "80", SETTING_DURATION_TICKS, "140"));

        shipped.add(gadget(GADGET_SNOWBALL_CANNON, "Schneeball-Kanone",
                "Schneebälle, die andere wegschubsen und niemandem wehtun", "SNOWBALL", 2000,
                SETTING_POWER, "8", SETTING_COOLDOWN_TICKS, "20"));

        shipped.add(gadget(GADGET_DISCO_FLOOR, "Disco-Boden",
                "Der Boden unter dir leuchtet - nur in den Augen der anderen", "MAGENTA_CONCRETE", 2500,
                SETTING_DURATION_TICKS, "6"));

        shipped.add(gadget(GADGET_FOOTSTEPS, "Fußspuren",
                "Du hinterlässt Abdrücke, links und rechts", "WHITE_DYE", 1200));

        shipped.add(gadget(GADGET_JUMP_PAD, "Sprungpad",
                "Leg eins hin - wer drauftritt, fliegt", "SLIME_BALL", 3000,
                SETTING_POWER, "11", SETTING_COOLDOWN_TICKS, "60", SETTING_DURATION_TICKS, "120"));

        shipped.add(gadget(GADGET_MOUNT, "Reittier",
                "Ein Pferd, das kommt, wenn du es rufst", "SADDLE", 4000, "speed", "25"));

        shipped.add(gadget(GADGET_HARVEST_HELPER, "Erntehelfer",
                "Rechtsklick erntet reife Pflanzen und pflanzt gleich neu", "IRON_HOE", 3500));

        shipped.add(gadget(GADGET_SIT, "Sitzen",
                "Setz dich auf Treppen und Stufen", "OAK_STAIRS", 800));

        shipped.add(gadget(GADGET_WORKBENCH, "Werkbank",
                "Eine Werkbank, die du überall aufklappen kannst", "CRAFTING_TABLE", 3500));

        shipped.add(gadget(GADGET_PET, "Haustier",
                "Ein kleines Tier, das dir hinterherläuft", "COD", 3000, "animal", "CAT"));

        shipped.add(gadget(GADGET_BALLOON, "Ballon",
                "Ein Ballon an einer Schnur über dir", "RED_CONCRETE", 2000, "colour", "RED_CONCRETE"));

        shipped.add(gadget(GADGET_CONFETTI, "Konfetti-Kanone",
                "Rechtsklick, und es regnet Farbe", "FIREWORK_STAR", 1500,
                SETTING_COOLDOWN_TICKS, "40"));

        shipped.add(gadget(GADGET_WEATHER, "Eigenes Wetter",
                "Dein eigener Himmel - sonst sieht ihn niemand", "CLOCK", 2000));

        shipped.add(gadget(GADGET_CHAT_BUBBLE, "Chat-Blase",
                "Was du schreibst, steht kurz über deinem Kopf", "PAPER", 2500,
                SETTING_DURATION_TICKS, "100"));

        shipped.add(gadget(GADGET_EMOTES, "Emotes",
                "Ein Menü mit Gesten, die alle in der Nähe sehen", "NAME_TAG", 1800,
                SETTING_COOLDOWN_TICKS, "40"));

        return shipped;
    }

    /**
     * One gadget, with its settings.
     *
     * @param id          what it is stored under
     * @param displayName what it is called
     * @param description the line under the name in the shop
     * @param icon        the material it is drawn as
     * @param priceBits   what it costs
     * @param settings    key and value, in pairs
     * @return the catalogue entry
     */
    private static CosmeticData gadget(String id, String displayName, String description, String icon,
                                       int priceBits, String... settings) {
        CosmeticData cosmetic = new CosmeticData(id, CosmeticType.GADGET, displayName, description,
                icon, priceBits, false);
        for (int i = 0; i + 1 < settings.length; i += 2) {
            cosmetic.getSettings().put(settings[i], settings[i + 1]);
        }
        return cosmetic;
    }
}
