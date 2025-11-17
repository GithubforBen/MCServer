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
        if (value instanceof ItemForSale) {
            ItemForSale itemForSale = (ItemForSale) value;
            set(path + ".item", itemForSale.getItem());
            set(path + ".price", itemForSale.getPrice());
        }
        super.set(path, value);
    }


}
