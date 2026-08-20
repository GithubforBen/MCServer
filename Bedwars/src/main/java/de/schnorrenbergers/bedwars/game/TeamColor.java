package de.schnorrenbergers.bedwars.game;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.Locale;

/**
 * The eight teams a round can have, in the order they are handed out.
 * <p>
 * Everything a team is made of - its wool, its bed, its glass, the dye of its leather armour - follows from
 * one {@link DyeColor}, so a team is one entry here and not six lists that have to be kept in step.
 */
public enum TeamColor {

    RED("Red", NamedTextColor.RED, DyeColor.RED),
    BLUE("Blue", NamedTextColor.BLUE, DyeColor.BLUE),
    GREEN("Green", NamedTextColor.GREEN, DyeColor.LIME),
    YELLOW("Yellow", NamedTextColor.YELLOW, DyeColor.YELLOW),
    AQUA("Aqua", NamedTextColor.AQUA, DyeColor.LIGHT_BLUE),
    WHITE("White", NamedTextColor.WHITE, DyeColor.WHITE),
    PINK("Pink", NamedTextColor.LIGHT_PURPLE, DyeColor.PINK),
    GRAY("Gray", NamedTextColor.DARK_GRAY, DyeColor.GRAY);

    private final String displayName;
    private final NamedTextColor textColor;
    private final DyeColor dye;
    private final Material wool;
    private final Material bed;
    private final Material glass;
    private final Material terracotta;
    private final Material concrete;

    TeamColor(String displayName, NamedTextColor textColor, DyeColor dye) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.dye = dye;
        this.wool = material("_WOOL");
        this.bed = material("_BED");
        this.glass = material("_STAINED_GLASS");
        this.terracotta = material("_TERRACOTTA");
        this.concrete = material("_CONCRETE");
    }

    /**
     * @param suffix the block family, e.g. {@code _WOOL}
     * @return the block of this colour in that family
     */
    private Material material(String suffix) {
        return Material.valueOf(dye.name() + suffix);
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getTextColor() {
        return textColor;
    }

    public DyeColor getDye() {
        return dye;
    }

    public Material getWool() {
        return wool;
    }

    public Material getBed() {
        return bed;
    }

    public Material getGlass() {
        return glass;
    }

    public Material getTerracotta() {
        return terracotta;
    }

    public Material getConcrete() {
        return concrete;
    }

    /**
     * @return the colour leather armour of this team is dyed in
     */
    public Color getArmorColor() {
        return dye.getColor();
    }

    /**
     * @return the single letter the sidebar and the tab list use
     */
    public String getInitial() {
        return displayName.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /**
     * @param name a colour name as it arrived from a config or a command
     * @return the colour, or {@code null} when there is none by that name
     */
    public static TeamColor byName(String name) {
        if (name == null) return null;
        for (TeamColor color : values()) {
            if (color.name().equalsIgnoreCase(name)) return color;
        }
        return null;
    }
}
