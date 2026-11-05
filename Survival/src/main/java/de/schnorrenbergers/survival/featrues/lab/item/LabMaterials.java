package de.schnorrenbergers.survival.featrues.lab.item;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * The one place that says which base material each lab item kind uses, and which look it borrows.
 * <p>
 * Pottery sherds have no vanilla crafting, brewing or smelting use besides decorated pots, cannot be
 * placed, eaten or thrown, and stack to 64. Each kind has its own type, so no lab item can be mistaken
 * for another kind or for a plain item. {@link #bottomInputs()} are the types group 5 must register as
 * brewing inputs, {@link #topReagents()} the ones for the top slot.
 */
public final class LabMaterials {

    public static final Material PASTE = Material.SHEAF_POTTERY_SHERD;
    public static final Material KOKAIN = Material.SNORT_POTTERY_SHERD;
    public static final Material PAPIERFILTER = Material.ANGLER_POTTERY_SHERD;
    public static final Material KOHLEFILTER = Material.ARCHER_POTTERY_SHERD;
    public static final Material SEIDENFILTER = Material.ARMS_UP_POTTERY_SHERD;
    public static final Material GOLDFILTER = Material.BLADE_POTTERY_SHERD;
    public static final Material FEENSTAUB = Material.BREWER_POTTERY_SHERD;
    public static final Material PFLANZENABFALL = Material.FRIEND_POTTERY_SHERD;

    private static final Map<LabKind, Material> BASE = new EnumMap<>(LabKind.class);
    private static final Map<LabKind, String> MODEL = new EnumMap<>(LabKind.class);

    static {
        put(LabKind.PASTE, PASTE, "minecraft:clay_ball");
        put(LabKind.KOKAIN, KOKAIN, "minecraft:sugar");
        put(LabKind.PAPIERFILTER, PAPIERFILTER, "minecraft:paper");
        put(LabKind.KOHLEFILTER, KOHLEFILTER, "minecraft:charcoal");
        put(LabKind.SEIDENFILTER, SEIDENFILTER, "minecraft:string");
        put(LabKind.GOLDFILTER, GOLDFILTER, "minecraft:gold_nugget");
        put(LabKind.FEENSTAUB, FEENSTAUB, "minecraft:glowstone_dust");
        put(LabKind.PFLANZENABFALL, PFLANZENABFALL, "minecraft:dead_bush");
    }

    private LabMaterials() {
    }

    private static void put(LabKind kind, Material base, String model) {
        BASE.put(kind, base);
        MODEL.put(kind, model);
    }

    /** The base material items of this kind are made of. */
    public static Material of(LabKind kind) {
        return BASE.get(kind);
    }

    /** The vanilla item model this kind looks like. */
    public static String modelOf(LabKind kind) {
        return MODEL.get(kind);
    }

    /** The types that go into the bottom slots as brewing input: paste and Kokain. */
    public static Material[] bottomInputs() {
        return new Material[]{PASTE, KOKAIN};
    }

    /** The types that go into the top slot: filters and Feenstaub. */
    public static Material[] topReagents() {
        return new Material[]{PAPIERFILTER, KOHLEFILTER, SEIDENFILTER, GOLDFILTER, FEENSTAUB};
    }
}
