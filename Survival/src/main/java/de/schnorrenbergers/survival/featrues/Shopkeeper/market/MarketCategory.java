package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import org.bukkit.Material;

import java.util.Set;

/**
 * The tabs of the marketplace. Every offer lands in exactly one of them, so a tab never hides an item
 * that no other tab shows.
 */
public enum MarketCategory {

    ALL("Alles", Material.COMPASS),
    BUILDING("Baumaterialien", Material.BRICKS),
    REDSTONE("Redstone", Material.REDSTONE),
    ENCHANTED_BOOKS("Verzauberte Bücher", Material.ENCHANTED_BOOK),
    GEAR("Rüstung & Waffen", Material.DIAMOND_CHESTPLATE),
    MISC("Sonstiges", Material.CHEST);

    /** Blocks that are redstone components rather than building material. */
    private static final Set<Material> REDSTONE_PARTS = Set.of(
            Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_TORCH, Material.REDSTONE_LAMP,
            Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR, Material.PISTON,
            Material.STICKY_PISTON, Material.OBSERVER, Material.HOPPER, Material.DROPPER,
            Material.DISPENSER, Material.LEVER, Material.TRIPWIRE_HOOK, Material.TRIPWIRE,
            Material.DAYLIGHT_DETECTOR, Material.TARGET, Material.NOTE_BLOCK, Material.SLIME_BLOCK,
            Material.HONEY_BLOCK, Material.TNT, Material.LECTERN, Material.CRAFTER);

    /** Item name endings that mark a tool, weapon or piece of armour. */
    private static final Set<String> GEAR_SUFFIXES = Set.of(
            "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "_SWORD", "_AXE", "_PICKAXE", "_SHOVEL", "_HOE");

    private static final Set<Material> GEAR_EXTRAS = Set.of(
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.ELYTRA,
            Material.TURTLE_HELMET, Material.FISHING_ROD, Material.SHEARS, Material.FLINT_AND_STEEL);

    private final String title;
    private final Material icon;

    MarketCategory(String title, Material icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * @param material the item to sort into a tab
     * @return the tab it belongs to, never {@link #ALL}
     */
    public static MarketCategory of(Material material) {
        if (material == Material.ENCHANTED_BOOK) return ENCHANTED_BOOKS;
        if (REDSTONE_PARTS.contains(material)) return REDSTONE;
        if (GEAR_EXTRAS.contains(material)) return GEAR;
        String name = material.name();
        for (String suffix : GEAR_SUFFIXES) {
            if (name.endsWith(suffix)) return GEAR;
        }
        if (material.isBlock()) return BUILDING;
        return MISC;
    }

    /**
     * @param material the item to test
     * @return whether it shows up under this tab
     */
    public boolean matches(Material material) {
        return this == ALL || of(material) == this;
    }
}
