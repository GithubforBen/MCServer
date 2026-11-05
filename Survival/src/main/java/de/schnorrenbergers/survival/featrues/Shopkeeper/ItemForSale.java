package de.schnorrenbergers.survival.featrues.Shopkeeper;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * One offer of a shopkeeper: a stack, what it costs and how often it went over the counter.
 * <p>
 * The sold counter is what the marketplace sorts and filters by, so it is stored with the offer.
 */
public class ItemForSale {
    private int price;
    private ItemStack item;
    private final UUID uuid;
    /** How many stacks of this offer were sold, over the whole lifetime of the shop. */
    private int sold;
    /** The stock seen the last time the chest was reachable, so an unloaded shop can still be listed. */
    private int lastKnownStock;
    /** Whether the shop also buys this offer back; off for every new and every older offer. */
    private boolean buyback;
    /** What the shop pays per lot; null until a price was set, so 0 stays a valid price. */
    private Integer buybackPrice;

    public ItemForSale(ItemStack item, int price) {
        this(item, price, 0);
    }

    /**
     * @param item  what is on offer
     * @param price what it costs
     * @param sold  how often it was sold before
     */
    public ItemForSale(ItemStack item, int price, int sold) {
        this.item = item;
        this.price = price;
        this.sold = sold;
        this.uuid = UUID.randomUUID();
    }

    public int getPrice() {
        return price;
    }

    public ItemStack getItemClone() {
        return item == null ? null : item.clone();
    }

    /**
     * @return whether this offer actually has something to sell - an offer confirmed with an empty slot
     *         has not, and used to take the whole marketplace down with a NullPointerException
     */
    public boolean isValid() {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }

    public ItemStack getItemOrginal() {
        return item;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getSold() {
        return sold;
    }

    public void setSold(int sold) {
        this.sold = sold;
    }

    /** Counts one completed sale. */
    public void recordSale() {
        this.sold++;
        if (lastKnownStock > 0) lastKnownStock--;
    }

    public int getLastKnownStock() {
        return lastKnownStock;
    }

    public void setLastKnownStock(int lastKnownStock) {
        this.lastKnownStock = lastKnownStock;
    }

    public boolean isBuyback() {
        return buyback;
    }

    /** Switches buyback on or off; the first time it goes on, the price is preset from the sale price. */
    public void setBuyback(boolean buyback) {
        if (buyback && buybackPrice == null) buybackPrice = price;
        this.buyback = buyback;
    }

    public int getBuybackPrice() {
        return buybackPrice == null ? 0 : buybackPrice;
    }

    /** @return whether a buyback price was ever set, even if buyback is off now */
    public boolean hasBuybackPrice() {
        return buybackPrice != null;
    }

    public void setBuybackPrice(int buybackPrice) {
        this.buybackPrice = buybackPrice;
    }

    /** Restores stored buyback values as they are, without the preset. */
    void restoreBuyback(boolean buyback, Integer buybackPrice) {
        this.buyback = buyback;
        this.buybackPrice = buybackPrice;
    }
}
