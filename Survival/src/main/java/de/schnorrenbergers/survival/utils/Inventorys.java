package de.schnorrenbergers.survival.utils;

import de.hems.api.ItemApi;
import de.hems.api.UUIDApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.InventoryBase;
import de.hems.paper.customInventory.types.ItemAction;
import de.hems.types.FileType;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ItemForSale;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.featrues.money.AtmHandler;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Inventorys extends InventoryBase {
    /**
     * @return a configured {@link CustomInventory} instance
     * representing an inventory setup for adding money
     */
    public static CustomInventory ADD_MONEY_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DISPENSER, "Geld Hinzufügen", (event) -> {
            ItemStack item = event.getInventory().getItem(4);
            if (item == null) {
                return;
            }
            event.getPlayer().getInventory().addItem(item);
        });
        for (int i = 0; i < 4; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setPlaceHolder(5);
        customInventory.setPlaceHolder(6);
        customInventory.setPlaceHolder(7);
        customInventory.setItem(8, new ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Bestätigen").buildSkull(),
                new ItemAction() {
                    @Override
                    public UUID getID() {
                        return UUID.fromString("a2e7a6ad-a1e4-4f56-b918-124adbf4a3c9");
                    }

                    @Override
                    public void onClick(InventoryClickEvent event) {
                        event.setCancelled(true);
                        ItemStack item = event.getInventory().getItem(4);
                        if (item == null) {
                            return;
                        }
                        if (item.getType() != Material.DIAMOND) {
                            event.getWhoClicked().getInventory().addItem(item);
                            return;
                        }
                        event.getInventory().setItem(4, null);
                        event.getWhoClicked().closeInventory();
                        MoneyHandler.addMoney(item.getAmount() * 100, event.getWhoClicked().getUniqueId());
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
        return customInventory;
    }

    public static CustomInventory ADMIN_SHOPKEEPER_INVENTORY(Shopkeeper shopkeeper) {
        CustomInventory customInventory = new CustomInventory(9 * 5, "Shopkeeper:" + shopkeeper.getUuid(), (event) -> {
        });
        for (int i = 0; i < 9 * 5; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(10, new ItemApi(Material.CHEST, "Kistenstandort ändern").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("30a2efe7-a143-4507-a879-e573f2c76d97");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                event.getWhoClicked().getPersistentDataContainer().set(
                        new NamespacedKey("shopkeeper", "chestlocation"), PersistentDataType.STRING, shopkeeper.getUuid().toString());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!(event.getWhoClicked() instanceof Player)) {
                            cancel();
                            return;
                        }
                        String s = event.getWhoClicked().getPersistentDataContainer().get(
                                new NamespacedKey("shopkeeper", "chestlocation"), PersistentDataType.STRING
                        );
                        if (s == null) {
                            cancel();
                            return;
                        }
                        Shopkeeper shopkeeper1 = ShopkeeperManager.getShopkeeper(UUID.fromString(s));
                        if (shopkeeper1 == null) {
                            cancel();
                            return;
                        }
                        new ParticleLine(event.getWhoClicked().getLocation(), shopkeeper1.getShop(), Particle.HAPPY_VILLAGER, 0.1).drawParticleLine();
                    }
                }.runTaskTimer(Survival.getInstance(), 0L, 10L);
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
        customInventory.setItem(11, new ItemApi(Material.DIAMOND, "Gegenstände bearbeiten").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("41d990e3-050d-42bc-9b66-85b8f456efaa");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGER_INVENTORY(shopkeeper, 1);
            }
        });
        return customInventory;
    }

    public static CustomInventory ITEM_MANAGER_INVENTORY(Shopkeeper shopkeeper, int page) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(9 * 6, shopkeeper.getName(), (event) -> {
        });
        List<ItemForSale> items = shopkeeper.getItems();
        customInventory.fillPlaceHolder();
        for (int i = ((page - 1) * 9 * 4); i < items.size(); i++) {
            int place = i + 9 - ((page - 1) * 9 * 4);
            if (place > 9 * 5) break;
            ItemForSale itemForSale = items.get(i);
            ItemStack item = itemForSale.getItem().clone();
            List<Component> lore = item.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.addFirst(Component.text("Price: " + itemForSale.getPrice() + " Bits"));
            item.lore(lore);
            customInventory.setItem(place, item, new ItemAction() {
                @Override
                public UUID getID() {
                    return UUIDApi.fromString(itemForSale.getUuid().toString() + ".imi");
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
                    return true;
                }

                @Override
                public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                    return ITEM_MANAGE_INVENTORY(shopkeeper, itemForSale);
                }
            });
        }
        //TODO: add option
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
                public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                    return ITEM_MANAGER_INVENTORY(shopkeeper, page - 1);
                }
            });
        }
        customInventory.setItem(9*6-2, new ItemApi(new URL("http://textures.minecraft.net/texture/5ff31431d64587ff6ef98c0675810681f8c13bf96f51d9cb07ed7852b2ffd1"), "Neu erstellen").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(shopkeeper.getUuid().toString() + ".imi.newitem");
            }

            @Override
            public void onClick(InventoryClickEvent event) throws MalformedURLException {

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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ADD_ITEM_INVENTORY(shopkeeper);
            }
        });
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
                public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                    return ITEM_MANAGER_INVENTORY(shopkeeper, page + 1);
                }
            });
        }
        return customInventory;
    }

    public static CustomInventory ADD_ITEM_INVENTORY(Shopkeeper shopkeeper) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, shopkeeper.getName() + ":Add Item", (event) -> {
            event.getPlayer().getInventory().addItem(event.getInventory().getItem(4));
        });
        customInventory.fillPlaceHolder();
        customInventory.removeItem(4);
        customInventory.addBackButton(6, UUIDApi.fromString(shopkeeper.getUuid().toString() + ".backfromitem"), ITEM_MANAGER_INVENTORY(shopkeeper, 1));


        customInventory.setItem(8, new  ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Bestätigen").buildSkull(), new ItemAction() {

            @Override
            public UUID getID() {
                return UUIDApi.fromString(shopkeeper.getUuid().toString() + ".imi.deeper.confirmitem");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                shopkeeper.getItems().add(new ItemForSale(event.getInventory().getItem(4), 1));
                event.getWhoClicked().closeInventory();
                event.getWhoClicked().sendMessage("Added a new Item! Set the price!");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return null;
            }
        });
        return customInventory;
    }

    public static CustomInventory CHANGE_ITEM_COST_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, shopkeeper.getName() + ":" + item.getItem().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();

        customInventory.setItem(3, new ItemApi(new URL("http://textures.minecraft.net/texture/93d7a9ee31348a35754383c167fa33abc02e8e68ca2c4a9691400e7fe34b3eb5"), "-").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.minus");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                item.setPrice(item.getPrice() - 1);
                try {
                    event.getWhoClicked().openInventory(CHANGE_ITEM_COST_INVENTORY(
                            shopkeeper,
                            item
                    ).getInventory());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
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
        });//minus
        ItemStack clone = item.getItem().clone();
        List<Component> lore = clone.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.addFirst(Component.text("Kosten: " + item.getPrice()));
        clone.lore(lore);
        customInventory.setItem(4, clone, ItemAction.NOTMOVABLE);
        customInventory.setItem(5, new ItemApi(new URL("http://textures.minecraft.net/texture/171d8979c1878a05987a7faf21b56d1b744f9d068c74cffcde1ea1edad5852"), "+").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.plus");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                try {
                    item.setPrice(item.getPrice() + 1);
                    try {
                        event.getWhoClicked().openInventory(CHANGE_ITEM_COST_INVENTORY(
                                shopkeeper,
                                item
                        ).getInventory());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                }
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
        });//plus
        customInventory.setItem(6, new ItemApi(Material.ARROW, "Back").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItem().getType().toString() + ".imi.deeper");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGE_INVENTORY(shopkeeper, item);
            }
        });

        customInventory.setItem(8, new  ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Bestätigen").buildSkull(), new ItemAction() {

            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.confirmprice");
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
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGE_INVENTORY(shopkeeper, item);
            }
        });
        return customInventory;
    }

    public static CustomInventory CHANGE_ITEM_AMOUNT_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, shopkeeper.getName() + ":" + item.getItem().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();

        customInventory.setItem(3, new ItemApi(new URL("http://textures.minecraft.net/texture/93d7a9ee31348a35754383c167fa33abc02e8e68ca2c4a9691400e7fe34b3eb5"), "-").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.minus.amount");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (item.getItem().getAmount() - 1 <1) return;
                item.getItem().setAmount(item.getItem().getAmount() - 1);
                try {
                    event.getWhoClicked().openInventory(CHANGE_ITEM_AMOUNT_INVENTORY(
                            shopkeeper,
                            item
                    ).getInventory());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
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
        });//minus
        customInventory.setItem(4, new ItemApi(item.getItem().getType(), item.getItem().getAmount(), "Anzahl der Items").build(), ItemAction.NOTMOVABLE);
        customInventory.setItem(5, new ItemApi(new URL("http://textures.minecraft.net/texture/171d8979c1878a05987a7faf21b56d1b744f9d068c74cffcde1ea1edad5852"), "+").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.plus.amount");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (item.getItem().getAmount() + 1 > 64) {
                    event.getWhoClicked().sendMessage("Du kannst maximal 64 items veraufen.");
                    return;
                }
                item.getItem().setAmount(item.getItem().getAmount() + 1);
                try {
                    event.getWhoClicked().openInventory(CHANGE_ITEM_AMOUNT_INVENTORY(
                            shopkeeper,
                            item
                    ).getInventory());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
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
        });//plus
        customInventory.setItem(6, new ItemApi(Material.ARROW, "Back").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItem().getType().toString() + ".imi.deeper");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGE_INVENTORY(shopkeeper, item);
            }
        });

        customInventory.setItem(8, new  ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Bestätigen").buildSkull(), new ItemAction() {

            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.confirmprice");
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
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGE_INVENTORY(shopkeeper, item);
            }
        });
        return customInventory;
    }

    public static CustomInventory ITEM_MANAGE_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, shopkeeper.getName() + ":" + item.getItem().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.setItem(3, new ItemApi(Material.DIAMOND, "Change Cost").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItem().getType().toString() + ".imi.deeper.cost");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return CHANGE_ITEM_COST_INVENTORY(shopkeeper, item);
            }
        });
        ItemStack clone = item.getItem().clone();
        List<Component> lore = clone.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.addFirst(Component.text("Kosten: " + item.getPrice()));
        clone.lore(lore);
        customInventory.setItem(4, clone, ItemAction.NOTMOVABLE);
        customInventory.setItem(5, new ItemApi(Material.CHEST, "Change Amount").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItem().getType().toString() + ".imi.deeper.amount");
            }

            @Override
            public void onClick(InventoryClickEvent event) throws MalformedURLException {

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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item);
            }
        });
        customInventory.setItem(6, new ItemApi(Material.ARROW, "Back").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItem().getType().toString() + ".imi.deeper");
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGER_INVENTORY(shopkeeper, 1);
            }
        });
        customInventory.setItem(7, new ItemApi(Material.BARRIER, "Delete Item").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.delete");
            }

            @Override
            public void onClick(InventoryClickEvent event) throws MalformedURLException {
                shopkeeper.getItems().remove(item);
                event.getWhoClicked().openInventory(ITEM_MANAGER_INVENTORY(shopkeeper, 1).getInventory());
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
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return null;
            }
        });

        customInventory.setItem(8, new  ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Bestätigen").buildSkull(), new ItemAction() {

            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getUuid().toString() + ".imi.deeper.confirmchange");
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
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() throws MalformedURLException {
                return ITEM_MANAGER_INVENTORY(shopkeeper,1);
            }
        });
        return customInventory;
    }

    /**
     * @return a configured {@link CustomInventory} instance
     * representing an inventory setup for adding money
     */
    public static CustomInventory ATM_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geldautomat", (event) -> {
        });
        customInventory.fillPlaceHolder();

        // Einzahlen
        customInventory.setItem(11, new ItemApi(new URL("http://textures.minecraft.net/texture/4ef356ad2aa7b1678aecb88290e5fa5a3427e5e456ff42fb515690c67517b8"), ChatColor.GREEN + "Einzahlen").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("20cdce07-5677-4b75-bf9c-1a9c77cad6ef");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
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
                try {
                    return ATM_DEPOSIT_INVENTORY();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Auszahlen
        customInventory.setItem(15, new ItemApi(new URL("http://textures.minecraft.net/texture/f84f597131bbe25dc058af888cb29831f79599bc67c95c802925ce4afba332fc"), ChatColor.RED + "Auszahlen").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("4eee4a9c-902f-4834-b768-6310cb1d1520");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
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
                try {
                    return ATM_PAYOUT_INVENTORY();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return customInventory;
    }

    public static CustomInventory ATM_DEPOSIT_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geld einzahlen", (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("cd283a6b-48d5-4b0b-a96a-f9f0955b20c6"), ATM_INVENTORY());

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"8e3a39d2-cb10-4fdf-b502-16fa5eaaaa13", "4181a762-de4d-490f-a00f-0134de937062", "a8606788-f848-4473-aadf-c47a7691a150"};

        for (int i = 0; i < amountMap.length; i++) {
            int amount = amountMap[i];

            ItemStack itemStack = new ItemApi(Material.DIAMOND, ChatColor.BLUE.toString() + amount + " Bits einzahlen", amount).build();

            int finalI = i;
            customInventory.setItem(currentInventoryPos, itemStack, new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.fromString(uuidMap[finalI]);
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    Player player = (Player) event.getWhoClicked();
                    AtmHandler.deposit(player, amount);
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
            currentInventoryPos += 3;
        }

        /*customInventory.setItem(26, new ItemApi(Material.DARK_OAK_SIGN, ChatColor.GREEN + "Anzahl eingeben").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("e05317b2-8bdd-4364-b72d-8da5f7063a28");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL, ChatColor.AQUA + "Eingabe:");
                if(anvil instanceof AnvilInventory anvilInv) {
                    anvilInv.setFirstItem(new ItemStack(Material.DIAMOND));
                    anvilInv.setResult(new ItemStack(Material.DIAMOND));
                }

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
        });*/

        return customInventory;
    }

    public static CustomInventory ATM_PAYOUT_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.RED + "Geld auszahlen", (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("39ed12c5-6a5c-4f52-8f4b-6d8bc2869f81"), ATM_INVENTORY());

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"1db84b7f-625e-40ec-b21d-5ec010022294", "b0933f0a-9618-4255-ad83-92c91cad4b75", "843e033d-c4b6-4ec0-919a-ae90b75c138a"};

        for (int i = 0; i < amountMap.length; i++) {
            int amount = amountMap[i];

            ItemStack itemStack = new ItemApi(Material.DIAMOND, ChatColor.BLUE.toString() + amount + " Bits einzahlen", amount).build();

            int finalI = i;
            customInventory.setItem(currentInventoryPos, itemStack, new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.fromString(uuidMap[finalI]);
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    Player player = (Player) event.getWhoClicked();
                    AtmHandler.payout(player, amount);
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
            currentInventoryPos += 3;
        }

        return customInventory;
    }
}
