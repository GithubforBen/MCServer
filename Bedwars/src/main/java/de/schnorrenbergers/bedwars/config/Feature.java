package de.schnorrenbergers.bedwars.config;

import org.bukkit.Material;

import java.util.Locale;

/**
 * A switch an admin can flip while the server is running.
 * <p>
 * Everything in here is a yes or no about how the round plays, which is what makes one menu of them
 * possible at all. Numbers - how long a respawn takes, how much a diamond costs - stay in the config
 * files, because a menu of sliders is a worse way of editing a number than the file it lives in.
 */
public enum Feature {

    /**
     * Combat the way 1.8 played it: no attack cooldown and no sweep. Every player is given an attack
     * speed nothing can recharge, so hitting fast is worth as much as hitting at the right moment.
     */
    OLD_PVP("1.8 PvP", Material.IRON_SWORD, false,
            "How a swing of a sword works.",
            "",
            "On: no cooldown bar, no sweep. Hitting fast",
            "is worth as much as hitting at the right",
            "moment - combat the way it played before 1.9.",
            "Off: the modern one, where a swing has to",
            "recharge before it does full damage."),

    /**
     * The bar 1.21.9 puts above the hotbar showing where everybody else is. Off by default, and that is
     * the point: a round of bedwars is half about not knowing where the other seven teams are.
     */
    LOCATOR_BAR("Locator Bar", Material.RECOVERY_COMPASS, false,
            "The bar over the hotbar that shows which",
            "direction every other player is in.",
            "",
            "Off, and that is deliberate: half of bedwars",
            "is not knowing where the other seven teams",
            "are. On, it gives away every rush for free.",
            "The tracker compass is the paid version."),

    /**
     * A bought compass points at the nearest enemy while it is held. The honest version of the locator
     * bar: the same information, but somebody had to pay for it and has to hold it in their hand.
     */
    COMPASS_TRACKER("Tracker Compass", Material.COMPASS, true,
            "Whether the shop sells a working compass.",
            "",
            "It points at one team at a time, costs an",
            "emerald for every team it is pointed at, and",
            "has to be held in a hand that could be",
            "holding a sword. Off takes the item out."),

    /** Whether the sun moves. The map says what it wants; this overrules it for the round that is on. */
    DAYLIGHT_CYCLE("Day and Night", Material.DAYLIGHT_DETECTOR, false,
            "Whether the sun moves during the round.",
            "",
            "Off holds the map at the time of day it asks",
            "for in its own file. On, a long round runs",
            "into the night and half of it is played in",
            "the dark."),

    /** Whether what somebody was carrying goes to whoever killed them. */
    RESOURCES_TO_KILLER("Resources to the Killer", Material.GOLD_INGOT, true,
            "What happens to the iron, gold and diamonds",
            "somebody was carrying when they died.",
            "",
            "On: whoever killed them gets the lot. Chasing",
            "a player home from the middle is worth doing.",
            "Off: it is simply gone, and the safest thing",
            "anybody can do with a diamond is stand still."),

    /** Whether hunger goes down at all. Off, because starving is not what this game mode is about. */
    HUNGER("Hunger", Material.COOKED_BEEF, false,
            "Whether the food bar goes down.",
            "",
            "Off by default. A round is decided by beds",
            "and fights, not by remembering to eat, and",
            "nothing in the shop feeds you.",
            "Golden apples still heal either way."),

    /** Whether anything at all can be bought. For a round that is meant to be a fist fight. */
    SHOP("Shop", Material.EMERALD, true,
            "Whether the keepers sell anything at all.",
            "",
            "Off makes a round out of nothing but the",
            "starting kit: a wooden sword, leather armour",
            "and whatever you can take off somebody else.",
            "The upgrade keeper closes with it."),

    /**
     * Whether the waiting lobby starts the round by itself once it is full enough.
     * <p>
     * Off is what a round somebody is hosting wants: the lobby fills up, and whoever is running it says
     * when it begins with /bw start.
     */
    AUTO_START("Auto Start", Material.CLOCK, true,
            "Whether the round starts by itself.",
            "",
            "On: the countdown runs as soon as there are",
            "enough players, and shortens when the lobby",
            "is full.",
            "Off: nothing happens until somebody types",
            "/bw start. What a hosted round wants.");

    private final String title;
    private final Material icon;
    private final boolean byDefault;
    private final String[] description;

    Feature(String title, Material icon, boolean byDefault, String... description) {
        this.title = title;
        this.icon = icon;
        this.byDefault = byDefault;
        this.description = description;
    }

    /**
     * @return the key this switch is written under in {@code features.yml}
     */
    public String getKey() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String getTitle() {
        return title;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean isDefault() {
        return byDefault;
    }

    public String[] getDescription() {
        return description.clone();
    }

    /**
     * @param name the key as a command or a config writes it
     * @return the switch, or {@code null} when there is none by that name
     */
    public static Feature byKey(String name) {
        if (name == null) return null;
        for (Feature feature : values()) {
            if (feature.getKey().equalsIgnoreCase(name) || feature.name().equalsIgnoreCase(name)) {
                return feature;
            }
        }
        return null;
    }
}
