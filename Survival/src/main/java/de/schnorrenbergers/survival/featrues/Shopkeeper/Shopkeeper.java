package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    public Shopkeeper(UUID uuid) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        String path = "shopkeepers." + uuid.toString();
        this.uuid = uuid;
        this.shop = config.getLocation(path + ".location.shop");
        this.chest = config.getLocation(path + ".location.chest");
        this.ownerTeam = config.getString(path + ".ownerTeam");
        this.items = getItemList(path + ".items");
    }

    public List<ItemForSale> getItemList(@NotNull String path) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        List<ItemForSale> result = new ArrayList<>();
        if (!config.contains(path + ".size")) return result;
        if (config.getInt(path + ".size") == 0) {
            return result;
        }
        for (int i = 0; i < config.getInt(path + ".size"); i++) {
            result.add(getItemForSale(path + ".[" + i + "]"));
        }
        return result;
    }

    public ItemForSale getItemForSale(String path) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        return new ItemForSale(config.getItemStack(path + ".item"), config.getInt(path + ".price"));
    }

    public void save() {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        String path = "shopkeepers." + uuid.toString();
        config.set(path + ".id", uuid);
        config.set(path + ".location.shop", getShop());
        config.set(path + ".location.chest", getChest());
        config.set(path + ".ownerTeam", getOwnerTeam());
        for (int i = 0; i < items.size(); i++) {
            ItemForSale itemForSale = items.get(i);
            config.set(path + "[" + "].item", itemForSale.getItem());
            config.set(path + ".price", itemForSale.getPrice());
        }
        config.set(path + ".size", getItems().size());
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
