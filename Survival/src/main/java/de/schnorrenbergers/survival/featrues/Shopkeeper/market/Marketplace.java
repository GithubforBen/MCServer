package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import de.schnorrenbergers.survival.featrues.Shopkeeper.ItemForSale;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The global view on every shopkeeper offer on this server.
 * <p>
 * Shopkeepers stay the selling side - the stock lives in their chest and the money goes to their team. This
 * only collects what they offer, so a buyer does not have to walk to every shop to compare prices.
 */
public final class Marketplace {

    /**
     * Whose turn it is among sellers that ask exactly the same price, kept per item.
     * <p>
     * Without this the first seller in the list would take every single sale, and undercutting by nothing
     * would be pointless. With it, equal offers share the customers.
     */
    private static final Map<ItemStack, Integer> ROTATION = new ConcurrentHashMap<>();

    private Marketplace() {
    }

    /**
     * Collects every offer of every loaded shopkeeper and groups it by item.
     *
     * @return the listings, one per kind of item
     */
    public static List<MarketListing> index() {
        Map<ItemStack, MarketListing> byItem = new LinkedHashMap<>();
        for (Shopkeeper shopkeeper : ShopkeeperManager.getShopkeepers()) {
            for (ItemForSale offer : shopkeeper.getItems()) {
                if (!offer.isValid()) continue;
                ItemStack item = offer.getItemClone();
                // the amount is what differs between competing offers, so it must not split the grouping
                ItemStack key = item.asOne();
                byItem.computeIfAbsent(key, MarketListing::new).add(new MarketOffer(shopkeeper, offer));
            }
        }
        return new ArrayList<>(byItem.values());
    }

    /**
     * The listings a player wants to see.
     *
     * @param category       the tab that is open
     * @param sort           how to order them
     * @param showOutOfStock whether items nobody can deliver are included
     * @param onlySold       whether to keep only items that were bought at least once
     * @return the listings to draw
     */
    public static List<MarketListing> view(MarketCategory category, MarketSort sort,
                                           boolean showOutOfStock, boolean onlySold) {
        List<MarketListing> listings = new ArrayList<>();
        for (MarketListing listing : index()) {
            if (!category.matches(listing.getSample().getType())) continue;
            if (!showOutOfStock && !listing.isInStock()) continue;
            if (onlySold && listing.totalSold() <= 0) continue;
            listings.add(listing);
        }
        listings.sort(sort.comparator());
        return listings;
    }

    /**
     * Buys one item from the best offer.
     * <p>
     * The cheapest seller is served first and keeps being served until their chest runs dry, which is what
     * makes undercutting worth something. Sellers asking the same price take turns.
     *
     * @param player  who is buying
     * @param listing the item being bought
     */
    public static void buy(Player player, MarketListing listing) {
        List<MarketOffer> best = listing.getBestOffers();
        if (best.isEmpty()) {
            player.sendMessage("Dieses Angebot ist gerade ausverkauft.");
            return;
        }
        ItemStack key = listing.getSample().asOne();
        int turn = ROTATION.merge(key, 1, Integer::sum) - 1;
        MarketOffer chosen = best.get(Math.floorMod(turn, best.size()));
        chosen.shopkeeper().buyItem(player, chosen.offer());
    }
}
