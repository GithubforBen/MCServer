package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.utils.configs.ShopConfig;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads the shops from {@code configs/shop-config.yml} and writes them back.
 * <p>
 * The layout of the file is the one it always had, so existing shops load as they are:
 * <pre>
 * shopkeepers:
 *   ids: [&lt;uuid&gt;, ...]
 *   &lt;uuid&gt;:
 *     name, ownerTeam, location.shop, location.chest
 *     items: { size: n, "[0]": { item, price, sold, stock }, ... }
 * </pre>
 */
public final class ShopkeeperStore {

    private final ShopConfig file;
    private final Logger logger;

    public ShopkeeperStore(ShopConfig file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /**
     * @return every shop in the file that has somewhere to stand
     */
    public List<Shopkeeper> loadAll() {
        YamlConfiguration config = file.getConfig();
        List<Shopkeeper> shops = new ArrayList<>();
        for (String id : config.getStringList("shopkeepers.ids")) {
            ConfigurationSection section = config.getConfigurationSection("shopkeepers." + id);
            if (section == null) continue;
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                logger.warning("Shopkeeper '" + id + "' has no valid id and stays unloaded.");
                continue;
            }
            Location shop = section.getLocation("location.shop");
            if (shop == null || shop.getWorld() == null) {
                logger.warning("Shopkeeper " + id + " has no usable location and stays unloaded.");
                continue;
            }
            shops.add(new Shopkeeper(uuid, section.getString("name"), shop, section.getLocation("location.chest"),
                    section.getString("ownerTeam"), readOffers(section.getConfigurationSection("items"))));
        }
        return shops;
    }

    private static List<ItemForSale> readOffers(ConfigurationSection items) {
        List<ItemForSale> offers = new ArrayList<>();
        if (items == null) return offers;
        int size = items.getInt("size", 0);
        for (int i = 0; i < size; i++) {
            ConfigurationSection entry = items.getConfigurationSection("[" + i + "]");
            if (entry == null) continue;
            ItemForSale offer = new ItemForSale(entry.getItemStack("item"), entry.getInt("price"),
                    entry.getInt("sold", 0));
            offer.setLastKnownStock(entry.getInt("stock", 0));
            // an offer without an item cannot be drawn or bought, and would break every view it appears in
            if (offer.isValid()) offers.add(offer);
        }
        return offers;
    }

    /**
     * Writes every shop and flushes the file to disk.
     * <p>
     * Entries that are not in the list are left alone on purpose: a shop whose world was not loaded at
     * start never made it into the list, and writing the list as the whole truth would delete it.
     *
     * @param shops every shop that is loaded
     */
    public void saveAll(Collection<Shopkeeper> shops) {
        YamlConfiguration config = file.getConfig();
        List<String> ids = new ArrayList<>(config.getStringList("shopkeepers.ids"));
        for (Shopkeeper shop : shops) {
            if (!ids.contains(shop.getUuid().toString())) ids.add(shop.getUuid().toString());
            write(config, shop);
        }
        config.set("shopkeepers.ids", ids);
        file.save();
    }

    private static void write(YamlConfiguration config, Shopkeeper shop) {
        String path = "shopkeepers." + shop.getUuid() + ".";
        config.set(path + "id", shop.getUuid().toString());
        config.set(path + "location.shop", shop.getShop());
        config.set(path + "location.chest", shop.getChest());
        config.set(path + "ownerTeam", shop.getOwnerTeam());
        config.set(path + "name", shop.getName());
        // an offer that was taken out must not stay behind under its old index
        config.set(path + "items", null);
        List<ItemForSale> items = shop.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemForSale offer = items.get(i);
            String item = path + "items.[" + i + "].";
            config.set(item + "item", offer.getItemClone());
            config.set(item + "price", offer.getPrice());
            config.set(item + "sold", offer.getSold());
            config.set(item + "stock", offer.getLastKnownStock());
        }
        config.set(path + "items.size", items.size());
    }
}
