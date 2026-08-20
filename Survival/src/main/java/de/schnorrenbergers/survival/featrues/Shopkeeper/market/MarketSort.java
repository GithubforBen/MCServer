package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import org.bukkit.Material;

import java.util.Comparator;

/**
 * How the marketplace orders what it shows. Cycled through by clicking the sort button.
 */
public enum MarketSort {

    NAME("Name (A-Z)", Material.NAME_TAG),
    PRICE("Preis (günstigste zuerst)", Material.GOLD_INGOT),
    SALES("Verkäufe (meistgekauft zuerst)", Material.EMERALD);

    private final String title;
    private final Material icon;

    MarketSort(String title, Material icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public Material getIcon() {
        return icon;
    }

    public MarketSort next() {
        MarketSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * @return how to order the listings of the marketplace under this setting
     */
    public Comparator<MarketListing> comparator() {
        return switch (this) {
            case NAME -> Comparator.comparing(MarketListing::displayName);
            // an item nobody stocks has no meaningful price, so it sinks to the bottom either way
            case PRICE -> Comparator.comparingInt(MarketListing::bestPrice)
                    .thenComparing(MarketListing::displayName);
            case SALES -> Comparator.comparingInt(MarketListing::totalSold).reversed()
                    .thenComparing(MarketListing::displayName);
        };
    }
}
