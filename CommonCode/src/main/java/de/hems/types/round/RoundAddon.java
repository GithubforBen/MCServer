package de.hems.types.round;

import org.bukkit.Material;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The extras a round can be played with, as the lobby knows them.
 * <p>
 * The addons themselves live on the bedwars server and the lobby cannot see that module, so the ids are
 * repeated here. They are the contract between the two: whoever starts a round picks from this list, the
 * ids travel with the round, and the bedwars server looks them up in its own registry. An id that has no
 * addon behind it is ignored there rather than breaking the round, which is what makes it safe to add one
 * on either side first.
 */
public enum RoundAddon {

    KITS("kits", "Kits", Material.IRON_CHESTPLATE,
            "Kleines Startpaket mit einem passiven Vorteil, in der Wartelobby gewählt", true),
    CUSTOM_ITEMS("custom-items", "Spezialitems", Material.FISHING_ROD,
            "Enterhaken, Rettungsplattform, Brückenei und Sprungfeld", true),
    KILLSTREAKS("killstreaks", "Killstreaks", Material.DIAMOND_SWORD,
            "Buffs für eine Killserie und ein Kopfgeld auf den, der eine hat", true),
    BED_TOKEN("bed-token", "Bett-Token", Material.RED_BED,
            "Sehr teures Item beim gegnerischen Händler, holt das eigene Bett zurück", true),
    RANDOM_EVENTS("random-events", "Zufallsereignisse", Material.FIREWORK_ROCKET,
            "Alle paar Minuten passiert etwas in der Mitte der Map, vorher angekündigt", false);

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String description;
    private final boolean defaultEnabled;

    RoundAddon(String id, String displayName, Material icon, String description, boolean defaultEnabled) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.defaultEnabled = defaultEnabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return whether it is preselected when somebody puts a round together
     */
    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    /**
     * @return the ids that are on when nobody has picked anything
     */
    public static Set<String> defaults() {
        Set<String> enabled = new LinkedHashSet<>();
        for (RoundAddon addon : values()) {
            if (addon.defaultEnabled) enabled.add(addon.id);
        }
        return enabled;
    }

    /**
     * @param id an addon id
     * @return the addon, or {@code null} when the lobby does not know it
     */
    public static RoundAddon byId(String id) {
        if (id == null) return null;
        String wanted = id.toLowerCase(Locale.ROOT);
        for (RoundAddon addon : values()) {
            if (addon.id.equals(wanted)) return addon;
        }
        return null;
    }
}
