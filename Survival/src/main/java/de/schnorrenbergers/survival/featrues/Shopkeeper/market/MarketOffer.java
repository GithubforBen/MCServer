package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import de.schnorrenbergers.survival.featrues.Shopkeeper.ItemForSale;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;

/**
 * One shopkeeper's offer for one item, as the marketplace sees it.
 *
 * @param shopkeeper who sells it
 * @param offer      what is sold and for how much
 */
public record MarketOffer(Shopkeeper shopkeeper, ItemForSale offer) {

    /**
     * @return how many times this offer can still be bought
     */
    public int stock() {
        return shopkeeper.getStock(offer);
    }

    /**
     * What one single item costs here.
     * <p>
     * Offers are compared per item, not per stack: 64 cobblestone for one diamond is the better deal than
     * 32 for one diamond, even though both cost the same.
     *
     * @return the price of a single item
     */
    public double unitPrice() {
        int amount = offer.getItemClone().getAmount();
        return amount <= 0 ? Double.MAX_VALUE : (double) offer.getPrice() / amount;
    }

    /**
     * @return the name of the shop this offer belongs to
     */
    public String sellerName() {
        return shopkeeper.getName() == null ? "?" : shopkeeper.getName();
    }
}
