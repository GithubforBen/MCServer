package de.schnorrenbergers.survival.featrues.Shopkeeper;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemForSale {
    private int price;
    private ItemStack item;
    private final UUID uuid;

    public ItemForSale(ItemStack item, int price) {
        this.item = item;
        this.price = price;
        this.uuid = UUID.randomUUID();
    }

    public int getPrice() {
        return price;
    }

    public ItemStack getItem() {
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
}
