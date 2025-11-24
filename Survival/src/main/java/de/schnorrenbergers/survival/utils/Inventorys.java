package de.schnorrenbergers.survival.utils;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.InventoryBase;
import de.hems.paper.customInventory.types.ItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.net.MalformedURLException;
import java.net.URL;
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
        CustomInventory customInventory = new CustomInventory(9*5, "Shopkeeper:" + shopkeeper.getUuid(), (event) -> {});
        for (int i = 0; i < 9*5; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(10, new ItemApi(Material.CHEST, "Set Chest Location").build(), new ItemAction() {
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
                }.runTaskTimer(Survival.getInstance(), 0L, 100L);
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
        return customInventory;
    }
}
