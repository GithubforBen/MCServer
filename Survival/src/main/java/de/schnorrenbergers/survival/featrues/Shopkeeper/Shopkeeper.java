package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public void buyItem(Player player, ItemForSale item) {
        if (!chest.getBlock().getType().equals(Material.CHEST)) {
            player.sendMessage("The Shopkeeper's chest is not a Chest :(");
            return;
        }
        Chest chestBlock = (Chest) chest.getBlock();
        if (calculateContent(chestBlock.getInventory(), item.getItem()) < item.getItem().getAmount()) {
            player.sendMessage("Insuficent stock in the shopkeeper's chest");
            return;
        }
        if (MoneyHandler.removeMoney(item.getPrice(), player.getUniqueId())) {
            removeItem(chestBlock.getInventory(), item.getItem());
            player.getInventory().addItem(item.getItem());
            player.sendMessage("You bought " + item.getItem().getAmount() + " " + item.getItem().getType().name() + " for " + item.getPrice() + "!");
        } else {
            player.sendMessage("You dont have enough money!");
        }
    }

    private void removeItem(Inventory inventory, ItemStack item) {
        int amount = item.getAmount();
        for (ItemStack itemStack : inventory) {
            if (itemStack == null) {
                continue;
            }
            if (amount <= 0) return;
            if (compareItemStacks(itemStack, item)) {
                if (itemStack.getAmount() <= amount) {
                    amount -= itemStack.getAmount();
                    inventory.removeItem(itemStack);
                } else if (itemStack.getAmount() > amount) {
                    itemStack.setAmount(itemStack.getAmount() - amount);
                }
            }
        }
    }

    private int calculateContent(Inventory inventory, ItemStack item) {
        int amount = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            }
            if (compareItemStacks(stack, item)) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    private boolean compareItemStacks(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }

        // Compare the Material (type)
        if (item1.getType() != item2.getType()) {
            return false; // If the materials are different, return false
        }
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 != null && meta2 != null) {
            String name1 = meta1.hasDisplayName() ? meta1.getDisplayName() : "";
            String name2 = meta2.hasDisplayName() ? meta2.getDisplayName() : "";

            if (!name1.equals(name2)) {
                return false; // If names are different, return false
            }
        } else if (meta1 != meta2) {
            return false; // One item has meta, the other doesn't
        }
        Map<Enchantment, Integer> enchants1 = meta1 != null ? meta1.getEnchants() : null;
        Map<Enchantment, Integer> enchants2 = meta2 != null ? meta2.getEnchants() : null;

        if (enchants1 != null && enchants2 != null) {
            if (!enchants1.equals(enchants2)) {
                return false; // If enchantments are different, return false
            }
        } else if (enchants1 != enchants2) {
            return false; // One item has enchantments, the other doesn't
        }
        return true;
    }


    private List<ItemForSale> getItemList(@NotNull String path) {
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

    private ItemForSale getItemForSale(String path) {
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
