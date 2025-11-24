package de.schnorrenbergers.survival.utils;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.InventoryBase;
import de.hems.paper.customInventory.types.ItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.featrues.money.AtmHandler;
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
    /**
     * @return a configured {@link CustomInventory} instance
     * representing an inventory setup for adding money
     */
    public static CustomInventory ATM_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geldautomat", (event) -> {});
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
                    return de.schnorrenbergers.survival.utils.customInventory.types.Inventorys.ATM_DEPOSIT_INVENTORY();
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
                    return de.schnorrenbergers.survival.utils.customInventory.types.Inventorys.ATM_PAYOUT_INVENTORY();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return customInventory;
    }

    public static CustomInventory ATM_DEPOSIT_INVENTORY() throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geld einzahlen", (event) -> {});
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("cd283a6b-48d5-4b0b-a96a-f9f0955b20c6"), ATM_INVENTORY());

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"8e3a39d2-cb10-4fdf-b502-16fa5eaaaa13", "4181a762-de4d-490f-a00f-0134de937062", "a8606788-f848-4473-aadf-c47a7691a150"};

        for(int i = 0; i < amountMap.length; i++) {
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
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.RED + "Geld auszahlen", (event) -> {});
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("39ed12c5-6a5c-4f52-8f4b-6d8bc2869f81"), ATM_INVENTORY());

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"1db84b7f-625e-40ec-b21d-5ec010022294", "b0933f0a-9618-4255-ad83-92c91cad4b75", "843e033d-c4b6-4ec0-919a-ae90b75c138a"};

        for(int i = 0; i < amountMap.length; i++) {
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
