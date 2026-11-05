package de.schnorrenbergers.survival.featrues.lab.item;

/** What a lab item is. Pure on purpose: the base materials live in {@link LabMaterials}. */
public enum LabKind {
    PASTE("Rohpaste", true),
    KOKAIN("Kokain", true),
    PAPIERFILTER("Papierfilter", false),
    KOHLEFILTER("Kohlefilter", false),
    SEIDENFILTER("Seidenfilter", false),
    GOLDFILTER("Goldfilter", false),
    FEENSTAUB("Feenstaub", false),
    PFLANZENABFALL("Pflanzenabfall", false);

    private final String label;
    private final boolean hasPurity;

    LabKind(String label, boolean hasPurity) {
        this.label = label;
        this.hasPurity = hasPurity;
    }

    /** The German name players see. */
    public String label() {
        return label;
    }

    /** Whether items of this kind carry a purity from 1 to 5. */
    public boolean hasPurity() {
        return hasPurity;
    }

    /** The purity a filter works on, or 0 for everything that is not a filter. */
    public int filterTier() {
        return switch (this) {
            case PAPIERFILTER -> 1;
            case KOHLEFILTER -> 2;
            case SEIDENFILTER -> 3;
            case GOLDFILTER -> 4;
            default -> 0;
        };
    }
}
