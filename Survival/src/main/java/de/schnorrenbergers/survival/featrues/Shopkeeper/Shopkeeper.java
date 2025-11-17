package de.schnorrenbergers.survival.featrues.Shopkeeper;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class Shopkeeper {
    private UUID uuid;
    private Location shop;
    private Location chest;
    private String ownerTeam;
    private List<ItemForSale> items;

    public Shopkeeper(UUID uuid, Location shop, Location chest, String ownerTeam, List<ItemForSale> items) {
        this.uuid = uuid;
        this.shop = shop;
        this.chest = chest;
        this.ownerTeam = ownerTeam;
        this.items = items;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getShop() {
        return shop;
    }

    public Location getChest() {
        return chest;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public List<ItemForSale> getItems() {
        return items;
    }
}
