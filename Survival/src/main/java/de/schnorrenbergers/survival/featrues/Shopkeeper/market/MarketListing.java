package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Every offer for one kind of item, which is what the marketplace shows as a single entry. The competing
 * offers behind it are what a right click reveals.
 */
public class MarketListing {

    private final ItemStack sample;
    private final List<MarketOffer> offers = new ArrayList<>();

    /**
     * @param sample one item of this kind, used for the icon and the name
     */
    public MarketListing(ItemStack sample) {
        this.sample = sample;
    }

    public void add(MarketOffer offer) {
        offers.add(offer);
    }

    public ItemStack getSample() {
        return sample.clone();
    }

    /**
     * @return every offer for this item, cheapest per item first
     */
    public List<MarketOffer> getOffers() {
        List<MarketOffer> sorted = new ArrayList<>(offers);
        sorted.sort(Comparator.comparingDouble(MarketOffer::unitPrice));
        return sorted;
    }

    /**
     * @return the offers that can actually be delivered right now, cheapest per item first
     */
    public List<MarketOffer> getAvailableOffers() {
        List<MarketOffer> available = new ArrayList<>();
        for (MarketOffer offer : offers) {
            if (offer.stock() > 0) available.add(offer);
        }
        available.sort(Comparator.comparingDouble(MarketOffer::unitPrice));
        return available;
    }

    /**
     * The offers a buyer should be served from: everything that is in stock and matches the best price per
     * item. There can be several, which is what makes taking turns between equal sellers possible.
     *
     * @return the offers tied for the best price, or an empty list if nothing is in stock
     */
    public List<MarketOffer> getBestOffers() {
        List<MarketOffer> available = getAvailableOffers();
        if (available.isEmpty()) return List.of();
        double best = available.getFirst().unitPrice();
        List<MarketOffer> tied = new ArrayList<>();
        for (MarketOffer offer : available) {
            // a hair of tolerance, so two ways of writing the same price still count as equal
            if (offer.unitPrice() - best < 1.0E-9) tied.add(offer);
        }
        return tied;
    }

    public boolean isInStock() {
        return !getAvailableOffers().isEmpty();
    }

    /**
     * @return the cheapest full price on offer, or {@link Integer#MAX_VALUE} when nothing is in stock
     */
    public int bestPrice() {
        List<MarketOffer> available = getAvailableOffers();
        return available.isEmpty() ? Integer.MAX_VALUE : available.getFirst().offer().getPrice();
    }

    /**
     * @return how often this item was bought across all shops
     */
    public int totalSold() {
        int total = 0;
        for (MarketOffer offer : offers) total += offer.offer().getSold();
        return total;
    }

    /**
     * @return how many shops offer this item
     */
    public int sellerCount() {
        return offers.size();
    }

    /**
     * @return what the item is called, for sorting and for the tooltip
     */
    public String displayName() {
        Component custom = sample.getItemMeta() == null ? null : sample.getItemMeta().displayName();
        if (custom != null) return PlainTextComponentSerializer.plainText().serialize(custom);
        String name = sample.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
