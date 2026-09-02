package de.hems.types.cosmetic;

/**
 * The kinds of cosmetic there are.
 * <p>
 * The kind decides two things: which tab it shows up under, and that a player has exactly one of it
 * selected at a time. Nobody wants two win effects going off at once.
 */
public enum CosmeticType {

    /** Goes off when its owner wins a round. */
    WIN_EFFECT("Sieges-Effekt", "Was passiert, wenn du gewinnst"),
    /** Something you carry into a round. */
    GADGET("Gadget", "Kleine Extras, die du im Spiel dabei hast");

    private final String displayName;
    private final String description;

    CosmeticType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
