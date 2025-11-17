package de.schnorrenbergers.survival.utils.configs;

import de.schnorrenbergers.survival.featrues.Shopkeeper.ItemForSale;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomConfig extends YamlConfiguration {
    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        if (value == null) {
            super.set(path, value);
        }
        if (value instanceof Shopkeeper) {
            Shopkeeper shopkeeper = (Shopkeeper) value;
            set(path + ".id", shopkeeper.getUuid());
            set(path + ".location.shop", shopkeeper.getShop());
            set(path + ".location.chest", shopkeeper.getChest());
            set(path + ".ownerTeam", shopkeeper.getOwnerTeam());
            for (int i = 0; i < shopkeeper.getItems().size(); i++) {
                set(path + "items.[" + i + "]", shopkeeper.getItems().get(i));
            }
            set(path + ".size", shopkeeper.getItems().size());
            return;
        }
        if (value instanceof UUID) {
            super.set(path, value.toString());
            return;
        }
        if (value instanceof ItemForSale) {
            ItemForSale itemForSale = (ItemForSale) value;
            set(path + ".item", itemForSale.getItem());
            set(path + ".price", itemForSale.getPrice());
        }
        super.set(path, value);
    }

    public Shopkeeper getShopkeeper(String path) {
        return new Shopkeeper(getUUID(path + ".id"),
                getLocation(path + ".location.shop"),
                getLocation(path + ".location.chest"),
                getString(path + ".ownerTeam"),
                getItemList(path+ ".items"));
    }

    public List<ItemForSale> getItemList(@NotNull String path) {
        List<ItemForSale> result = new ArrayList<>();
        if (!contains(path + ".size")) return result;
        if (getInt(path + ".size") == 0) {
            return result;
        }
        for (int i = 0; i < getInt(path + ".size"); i++) {
            result.add(getItemForSale(path + ".[" + i + "]"));
        }
        return result;
    }

    public ItemForSale getItemForSale(String path) {
        return new ItemForSale(getItemStack(path+ ".item"), getInt(path + ".price"));
    }

    public UUID getUUID(String path) {
        return UUID.fromString(getString(path));
    }

    public static CustomConfig loadConfiguration(File file) {
        return (CustomConfig) YamlConfiguration.loadConfiguration(file);
    }
}
