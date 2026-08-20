package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.ItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Shopkeeper {

    /** Marks a villager as belonging to a shopkeeper, so it can be found again after a restart. */
    private static final NamespacedKey SHOP_ID = new NamespacedKey("shopkeeper", "shopid");

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
        applyVillagerSettings();
    }

    /**
     * Reads a shopkeeper back from the config and puts its villager back into the world.
     *
     * @param uuid the shopkeeper to load
     */
    public Shopkeeper(UUID uuid) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        String path = "shopkeepers." + uuid.toString();
        this.uuid = uuid;
        this.name = config.getString(path + ".name");
        this.shop = config.getLocation(path + ".location.shop");
        this.chest = config.getLocation(path + ".location.chest");
        this.ownerTeam = config.getString(path + ".ownerTeam");
        this.items = getItemList(path + ".items");
        if (shop == null || shop.getWorld() == null) {
            Survival.getInstance().getLogger().warning(
                    "Shopkeeper " + uuid + " has no usable location and stays unloaded.");
            return;
        }
        // no spawning here on purpose. Entities live in their own storage since 1.17, so a chunk can be
        // loaded while its entities are not - looking then finds nothing and would spawn a second
        // villager on every restart. ShopkeeperChunkListener waits for EntitiesLoadEvent instead.
    }

    /**
     * @param chunk the chunk to test
     * @return whether this shop stands in it, worked out from the coordinates so the chunk stays untouched
     */
    public boolean isInChunk(Chunk chunk) {
        return shop != null && shop.getWorld() != null
                && shop.getWorld().equals(chunk.getWorld())
                && (shop.getBlockX() >> 4) == chunk.getX()
                && (shop.getBlockZ() >> 4) == chunk.getZ();
    }

    /**
     * Lets go of the villager when its chunk unloads, so no reference to a dead entity is kept around.
     */
    public void releaseVillager() {
        this.villager = null;
    }

    /**
     * @param chunk the chunk to test
     * @return whether the chest of this shop stands in it - which is not necessarily the chunk the
     *         villager stands in, the two can straddle a border
     */
    public boolean isChestInChunk(Chunk chunk) {
        return chest != null && chest.getWorld() != null
                && chest.getWorld().equals(chunk.getWorld())
                && (chest.getBlockX() >> 4) == chunk.getX()
                && (chest.getBlockZ() >> 4) == chunk.getZ();
    }

    /**
     * Reads the chest one last time and remembers what was in it.
     * <p>
     * Called while the chunk is unloading, which is the last moment the chest can be looked at without
     * pulling it back in. Without this the remembered stock would be whatever it was when someone last
     * opened the marketplace - an owner could refill their chest, walk away, and the market would keep
     * showing the shop as empty.
     */
    public void refreshStock() {
        if (chest == null || chest.getBlock().getType() != Material.CHEST) {
            for (ItemForSale offer : items) offer.setLastKnownStock(0);
            return;
        }
        Chest chestBlock = (Chest) chest.getBlock().getState();
        for (ItemForSale offer : items) {
            ItemStack wanted = offer.getItemClone();
            if (wanted.getAmount() <= 0) {
                offer.setLastKnownStock(0);
                continue;
            }
            offer.setLastKnownStock(calculateContent(chestBlock.getInventory(), wanted) / wanted.getAmount());
        }
    }

    /**
     * Takes over the villager that already belongs to this shopkeeper, or spawns one if it is gone.
     * <p>
     * Adopting matters: spawning unconditionally leaves a second villager behind on every restart, and
     * killing the old one first loses the entity whenever its chunk happens to not be loaded yet.
     */
    public void spawnOrAdoptVillager() {
        if (shop == null || shop.getWorld() == null) return;
        if (villager != null && villager.isValid()) return;
        Villager existing = findOwnVillager();
        if (existing != null) {
            this.villager = existing;
            applyVillagerSettings();
            return;
        }
        this.villager = (Villager) shop.getWorld().spawnEntity(shop, org.bukkit.entity.EntityType.VILLAGER);
        applyVillagerSettings();
    }

    /**
     * @return the villager in this shop's chunk that carries this shopkeeper's id, or {@code null}
     */
    private Villager findOwnVillager() {
        for (Entity entity : shop.getChunk().getEntities()) {
            if (!(entity instanceof Villager candidate)) continue;
            String id = candidate.getPersistentDataContainer().get(SHOP_ID, PersistentDataType.STRING);
            if (uuid.toString().equals(id)) return candidate;
        }
        return null;
    }

    /** Puts the villager into the state a shopkeeper needs: named, still, and not killable. */
    private void applyVillagerSettings() {
        villager.setAdult();
        villager.customName(Component.text(name == null ? "Shop" : name));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.getPersistentDataContainer().set(SHOP_ID, PersistentDataType.STRING, uuid.toString());
    }

    /**
     * Sells one lot of an offer to a player.
     * <p>
     * Every step is checked and undone if the next one fails. Handing out the item while the chest still
     * holds it - or taking the money while the item never arrives - are the two ways this can go wrong, and
     * both used to be possible.
     *
     * @param player who is buying
     * @param item   the offer being bought
     */
    public void buyItem(Player player, ItemForSale item) {
        if (chest == null || chest.getBlock().getType() != Material.CHEST) {
            player.sendMessage("Die Kiste dieses Shops gibt es nicht mehr.");
            return;
        }
        ItemStack wanted = item.getItemClone();
        if (wanted.getType().isAir() || wanted.getAmount() <= 0) {
            player.sendMessage("Dieses Angebot ist kaputt und kann nicht gekauft werden.");
            return;
        }
        Team seller = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(ownerTeam);
        if (seller == null) {
            player.sendMessage("Diesen Shop gibt es nicht mehr - sein Team wurde aufgelöst.");
            return;
        }
        Chest chestBlock = (Chest) chest.getBlock().getState();
        Inventory stock = chestBlock.getInventory();
        if (calculateContent(stock, wanted) < wanted.getAmount()) {
            player.sendMessage("Insufficient stock in the shopkeeper's chest");
            return;
        }
        if (!MoneyHandler.removeMoney(item.getPrice(), player.getUniqueId())) {
            player.sendMessage("You dont have enough money!");
            return;
        }

        // Bukkit reports what it could not take out. The hand written loop this replaces wrote through
        // ItemStack mirrors from getContents(), which silently removes nothing if those are ever copies -
        // the buyer would get the goods and the chest would keep them.
        Map<Integer, ItemStack> notRemoved = stock.removeItem(wanted.clone());
        if (!notRemoved.isEmpty()) {
            MoneyHandler.addMoney(item.getPrice(), player.getUniqueId());
            player.sendMessage("Der Kauf hat nicht geklappt - die Kiste hat sich gerade geändert.");
            return;
        }

        Map<Integer, ItemStack> notDelivered = player.getInventory().addItem(wanted.clone());
        if (!notDelivered.isEmpty()) {
            // the goods are already out of the chest, so they have to go back before anyone is charged
            for (ItemStack leftover : notDelivered.values()) {
                for (ItemStack spill : stock.addItem(leftover).values()) {
                    // the chest filled up meanwhile - dropping is better than deleting it
                    chest.getWorld().dropItemNaturally(chest.getBlock().getLocation().add(0.5, 1, 0.5), spill);
                }
            }
            MoneyHandler.addMoney(item.getPrice(), player.getUniqueId());
            player.sendMessage("In deinem Inventar ist kein Platz!");
            return;
        }

        MoneyHandler.addMoney(item.getPrice(), seller);
        item.recordSale();
        item.setLastKnownStock(calculateContent(stock, wanted) / wanted.getAmount());
        // a sale changes both the stock and the counter, so it must not wait for the next shutdown
        ShopkeeperManager.saveAll();
        player.sendMessage("You bought " + wanted.getAmount() + " " + wanted.getType().name()
                + " for " + item.getPrice() + "!");
    }

    /**
     * @param offer the offer to look up
     * @return how many whole stacks of that offer the chest can still deliver
     */
    public int getStock(ItemForSale offer) {
        if (chest == null || chest.getWorld() == null) return 0;
        ItemStack wanted = offer.getItemClone();
        if (wanted.getAmount() <= 0) return 0;
        // touching the block would load its chunk. The marketplace asks every shop at once, so that would
        // drag every shop chunk on the server in just to draw a list - an unloaded shop answers from the
        // stock it last saw instead.
        if (!chest.getWorld().isChunkLoaded(chest.getBlockX() >> 4, chest.getBlockZ() >> 4)) {
            return offer.getLastKnownStock();
        }
        if (chest.getBlock().getType() != Material.CHEST) return 0;
        Chest chestBlock = (Chest) chest.getBlock().getState();
        int stock = calculateContent(chestBlock.getInventory(), wanted) / wanted.getAmount();
        offer.setLastKnownStock(stock);
        return stock;
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
            ItemForSale offer = getItemForSale(path + ".[" + i + "]");
            // an offer without an item cannot be drawn or bought, and would break every view it appears in
            if (offer.isValid()) result.add(offer);
        }
        return result;
    }

    private ItemForSale getItemForSale(String path) {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        ItemForSale offer = new ItemForSale(config.getItemStack(path + ".item"),
                config.getInt(path + ".price"), config.getInt(path + ".sold", 0));
        offer.setLastKnownStock(config.getInt(path + ".stock", 0));
        return offer;
    }

    /**
     * Writes this shopkeeper into the config.
     * <p>
     * This only stores data. It used to kill the villager as a side effect, which meant nothing could be
     * saved without destroying the shop - so saving could only ever happen on shutdown, and a crash lost
     * everything. Removing the villager is {@link #despawn()} now.
     */
    public void save() {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        List<String> ids = new ArrayList<>(config.getStringList("shopkeepers.ids"));
        if (!ids.contains(uuid.toString())) {
            ids.add(uuid.toString());
            config.set("shopkeepers.ids", ids);
        }
        String path = "shopkeepers." + uuid.toString();
        config.set(path + ".id", uuid.toString());
        config.set(path + ".location.shop", getShop());
        config.set(path + ".location.chest", getChest());
        config.set(path + ".ownerTeam", getOwnerTeam());
        config.set(path + ".name", name);
        config.set(path + ".items", null);
        for (int i = 0; i < items.size(); i++) {
            ItemForSale itemForSale = items.get(i);
            config.set(path + ".items.[" + i + "].item", itemForSale.getItemClone());
            config.set(path + ".items.[" + i + "].price", itemForSale.getPrice());
            config.set(path + ".items.[" + i + "].sold", itemForSale.getSold());
            config.set(path + ".items.[" + i + "].stock", itemForSale.getLastKnownStock());
        }
        config.set(path + ".items.size", items.size());
    }

    /**
     * Removes the villager from the world, without touching the stored data. Used when the server shuts
     * down, so no second villager is left behind for the next start to find.
     */
    public void despawn() {
        if (villager == null) return;
        villager.remove();
        villager = null;
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
        // the chest moved to another chunk, so the lookup used on chunk unload has to be rebuilt
        ShopkeeperManager.invalidateChestIndex();
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
            if (!itemForSale.isValid()) continue;
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
