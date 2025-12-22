package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.ItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Shopkeeper {
    private UUID uuid;
    private Location shop;
    private Location chest;
    private String ownerTeam;
    private List<ItemForSale> items;
    private Villager villager;
    private String name;

    public Shopkeeper(UUID uuid, String name, Location shop, Location chest, String ownerTeam, List<ItemForSale> items) {
        this.uuid = uuid;
        this.shop = shop;
        this.chest = chest;
        this.name = name;
        this.ownerTeam = ownerTeam;
        this.items = items;
        this.villager = (Villager) shop.getWorld().spawnEntity(shop, org.bukkit.entity.EntityType.VILLAGER);
        villager.setAdult();
        villager.customName(Component.text(name));
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.getPersistentDataContainer().set(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING, uuid.toString());
    }

    public Shopkeeper(UUID uuid) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        String path = "shopkeepers." + uuid.toString();
        this.uuid = uuid;
        this.name = config.getString(path + ".name");
        this.shop = config.getLocation(path + ".location.shop");
        this.chest = config.getLocation(path + ".location.chest");
        this.ownerTeam = config.getString(path + ".ownerTeam");
        this.items = getItemList(path + ".items");
        if (!shop.getChunk().isLoaded()) {
            shop.getChunk().load();
        }
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            System.out.println(shop.getChunk().getEntities().length);
            Arrays.stream(shop.getChunk().getEntities()).filter(entity -> entity instanceof Villager).filter((v) -> {
                System.out.println("Found Villager:");
                String s = v.getPersistentDataContainer().get(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING);
                System.out.println("Found Villager:");
                if (s == null) return false;
                System.out.println("Found Villager:");
                try {
                    UUID.fromString(s);
                    System.out.println("Found Villager:");
                } catch (IllegalArgumentException e) {
                    return false;
                }
                System.out.println("Found Villager:");
                return s.equals(uuid.toString());
            }).forEach((e) -> {
                this.villager = (Villager) e;
                villager.setInvulnerable(false);
                villager.damage(1000000000);
                e.remove();
            });

            this.villager = (Villager) shop.getWorld().spawnEntity(shop, org.bukkit.entity.EntityType.VILLAGER);
            villager.setAdult();
            villager.customName(Component.text(name));
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.getPersistentDataContainer().set(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING, uuid.toString());

        }, 1000L);
    }

    public void buyItem(Player player, ItemForSale item) {
        if (!chest.getBlock().getType().equals(Material.CHEST)) {
            player.sendMessage("The Shopkeeper's chest is not a Chest :(");
            return;
        }
        Chest chestBlock = (Chest) chest.getBlock().getState();
        if (calculateContent(chestBlock.getInventory(), item.getItemClone()) < item.getItemClone().getAmount()) {
            player.sendMessage("Insufficient stock in the shopkeeper's chest");
            return;
        }
        if (MoneyHandler.removeMoney(item.getPrice(), player.getUniqueId())) {
            removeItem(chestBlock.getInventory(), item.getItemClone());
            player.getInventory().addItem(item.getItemClone());
            MoneyHandler.addMoney(item.getPrice(), Objects.requireNonNull(player.getScoreboard().getTeam(ownerTeam)));//TODO: add money to owners account add way to get that moneyy
            player.sendMessage("You bought " + item.getItemClone().getAmount() + " " + item.getItemClone().getType().name() + " for " + item.getPrice() + "!");
        } else {
            player.sendMessage("You dont have enough money!");
        }
    }

    private void removeItem(Inventory inventory, ItemStack item) {
        int amount = item.clone().getAmount();
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) {
                continue;
            }
            if (amount <= 0) return;
            if (compareItemStacks(itemStack, item.clone())) {
                if (itemStack.getAmount() <= amount) {
                    amount -= itemStack.getAmount();
                    inventory.removeItem(itemStack);
                } else if (itemStack.getAmount() > amount) {
                    itemStack.setAmount(itemStack.getAmount() - amount);
                    return;
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
        return item1.isSimilar(item2);
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
        if (config.contains("shopkeepers.ids")) {
            List<String> stringList = config.getStringList("shopkeepers.ids");
            if (!stringList.contains(uuid.toString())) {
                stringList.add(uuid.toString());
                config.set("shopkeepers.ids", stringList);
            }
        } else {
            config.set("shopkeepers.ids", List.of(uuid.toString()));
        }
        String path = "shopkeepers." + uuid.toString();
        config.set(path + ".id", uuid.toString());
        config.set(path + ".location.shop", getShop());
        config.set(path + ".location.chest", getChest());
        config.set(path + ".ownerTeam", getOwnerTeam());
        config.set(path + ".name", name);
        for (int i = 0; i < items.size(); i++) {
            ItemForSale itemForSale = items.get(i);
            config.set(path + ".items.[" + i + "].item", itemForSale.getItemClone());
            config.set(path + ".items.[" + i + "].price", itemForSale.getPrice());
        }
        config.set(path + ".items.size", getItems().size());
        villager.setInvulnerable(false);
        villager.damage(1000000000);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getShop() {
        return shop;
    }

    public void setShop(Location shop) {
        this.shop = shop;
    }

    public Location getChest() {
        return chest;
    }

    public void setChest(Location chest) {
        this.chest = chest;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public List<ItemForSale> getItems() {
        return items;
    }

    public void setItems(List<ItemForSale> items) {
        this.items = items;
    }

    public CustomInventory getInventory(int page) {
        CustomInventory customInventory = new CustomInventory(9 * 6, name, (event) -> {
        });
        customInventory.fillPlaceHolder();
        for (int i = ((page - 1) * 9 * 4); i < items.size(); i++) {
            int place = i + 9 - ((page - 1) * 9 * 4);
            if (place > 9 * 5) break;
            ItemForSale itemForSale = items.get(i);
            ItemStack item = itemForSale.getItemClone();
            List<Component> lore = item.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.addFirst(Component.text("Price: " + itemForSale.getPrice() + " Bits"));
            item.lore(lore);
            customInventory.setItem(place, item, new ItemAction() {
                @Override
                public UUID getID() {
                    return itemForSale.getUuid();
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    buyItem((Player) event.getWhoClicked(), itemForSale);
                    event.getWhoClicked().closeInventory();
                }

                @Override
                public boolean isMovable() {
                    return false;
                }

                @Override
                public boolean fireEvent() {
                    return true;
                }

                @Override
                public CustomInventory loadInventoryOnClick() {
                    return null;
                }
            });
        }
        if (page > 1) {
            customInventory.setItem(9 * 6 - 9, new ItemApi(Material.ARROW, "Back").build(), new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.randomUUID();
                }

                @Override
                public void onClick(InventoryClickEvent event) {

                }

                @Override
                public boolean isMovable() {
                    return false;
                }

                @Override
                public boolean fireEvent() {
                    return false;
                }

                @Override
                public CustomInventory loadInventoryOnClick() {
                    return getInventory(page - 1);
                }
            });
        }
        if (items.size() > (page * 9 * 4)) {
            customInventory.setItem(9 * 6 - 1, new ItemApi(Material.ARROW, "Next").build(), new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.randomUUID();
                }

                @Override
                public void onClick(InventoryClickEvent event) {

                }

                @Override
                public boolean isMovable() {
                    return false;
                }

                @Override
                public boolean fireEvent() {
                    return false;
                }

                @Override
                public CustomInventory loadInventoryOnClick() {
                    return getInventory(page + 1);
                }
            });
        }
        return customInventory;
    }

    public void setVillager(Villager villager) {
        this.villager = villager;
    }

    public Villager getVillager() {
        return villager;
    }

    public String getName() {
        return name;
    }
}
