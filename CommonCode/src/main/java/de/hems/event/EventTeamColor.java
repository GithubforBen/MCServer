package de.hems.event;

/**
 * The colours a team can have. Kept free of bukkit types so the model also works on the launcher and on the
 * proxy - the UI turns {@link #getMaterialName()} into an item.
 */
public enum EventTeamColor {
    RED("Rot", 'c', "RED_WOOL"),
    BLUE("Blau", '9', "BLUE_WOOL"),
    GREEN("Grün", 'a', "LIME_WOOL"),
    YELLOW("Gelb", 'e', "YELLOW_WOOL"),
    AQUA("Türkis", 'b', "LIGHT_BLUE_WOOL"),
    PURPLE("Lila", 'd', "MAGENTA_WOOL"),
    ORANGE("Orange", '6', "ORANGE_WOOL"),
    WHITE("Weiß", 'f', "WHITE_WOOL"),
    BLACK("Schwarz", '8', "BLACK_WOOL");

    private final String displayName;
    private final char colorCode;
    private final String materialName;

    EventTeamColor(String displayName, char colorCode, String materialName) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.materialName = materialName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the legacy colour code of this colour, e.g. {@code §c}
     */
    public String getColorCode() {
        return "§" + colorCode;
    }

    /**
     * @return the name of the bukkit material that represents this colour
     */
    public String getMaterialName() {
        return materialName;
    }

    /**
     * @param index the position of a team
     * @return the colour that team gets by default
     */
    public static EventTeamColor byIndex(int index) {
        EventTeamColor[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
