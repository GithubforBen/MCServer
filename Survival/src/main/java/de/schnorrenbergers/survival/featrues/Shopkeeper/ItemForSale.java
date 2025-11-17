package de.schnorrenbergers.survival.featrues.Shopkeeper;

import org.bukkit.inventory.ItemStack;

public class ItemForSale {
    private int price;
    private ItemStack item;

    public ItemForSale(ItemStack item, int price) {
        this.item = item;
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    public ItemStack getItem() {
        return item;
    }
}
