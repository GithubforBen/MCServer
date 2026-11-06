package com.benschnorr.survival.featrues.Shopkeeper;

import com.benschnorr.paper.CoreProtectLog;
import com.benschnorr.paper.money.MoneyService;
import com.benschnorr.paper.team.TeamService;
import com.benschnorr.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One shop: a villager customers click, a chest that holds the stock, the team that gets the money, and
 * what is on offer.
 * <p>
 * This is the shop and nothing around it. Reading and writing the config is {@link ShopkeeperStore}, the
 * menus are {@link ShopUi} and {@link ShopEditorUi}, and finding a shop by id or chest is
 * {@link ShopkeeperManager}. It used to be all of that at once, which is how a constructor ended up reading
 * the config and a save ended up killing the villager.
 */
public class Shopkeeper {

    /** Marks a villager as belonging to a shopkeeper, so it can be found again after a restart. */
    private static final NamespacedKey SHOP_ID = new NamespacedKey("shopkeeper", "shopid");

    private final UUID uuid;
    private final String name;
    private Location shop;
    private Location chest;
    private String ownerTeam;
    private List<ItemForSale> items;
    private @Nullable String skin;
    private @Nullable LivingEntity villager;

    /**
     * A shop as it is stored. No villager is put into the world here: see {@link #spawnOrAdoptVillager()}.
     *
     * @param uuid      its id
     * @param name      what it is called
     * @param shop      where the villager stands
     * @param chest     where the stock is
     * @param ownerTeam the team it belongs to
     * @param items     what it sells
     */
    public Shopkeeper(UUID uuid, String name, Location shop, Location chest, String ownerTeam,
                      List<ItemForSale> items) {
        this.uuid = uuid;
        this.name = name;
        this.shop = shop;
        this.chest = chest;
        this.ownerTeam = ownerTeam;
        this.items = items;
    }

    /**
     * A shop as it is stored, with the skin it wears.
     *
     * @param skin the Minecraft player name whose skin the figure wears, or {@code null} for a villager
     */
    public Shopkeeper(UUID uuid, String name, Location shop, Location chest, String ownerTeam,
                      List<ItemForSale> items, @Nullable String skin) {
        this(uuid, name, shop, chest, ownerTeam, items);
        this.skin = skin;
    }

    /**
     * @param entity any entity
     * @return the id of the shop the entity is the villager of, or {@code null} when it is none
     */
    public static @Nullable UUID shopIdOf(Entity entity) {
        if (!(entity instanceof Villager) && !(entity instanceof Mannequin)) return null;
        String id = entity.getPersistentDataContainer().get(SHOP_ID, PersistentDataType.STRING);
        if (id == null) return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ the villager

    /**
     * @param chunk the chunk to test
     * @return whether this shop stands in it, worked out from the coordinates so the chunk stays untouched
     */
    public boolean isInChunk(Chunk chunk) {
        return isIn(shop, chunk);
    }

    /**
     * @param chunk the chunk to test
     * @return whether the chest of this shop stands in it - which is not necessarily the chunk the
     *         villager stands in, the two can straddle a border
     */
    public boolean isChestInChunk(Chunk chunk) {
        return isIn(chest, chunk);
    }

    private static boolean isIn(@Nullable Location location, Chunk chunk) {
        return location != null && location.getWorld() != null
                && location.getWorld().equals(chunk.getWorld())
                && (location.getBlockX() >> 4) == chunk.getX()
                && (location.getBlockZ() >> 4) == chunk.getZ();
    }

    /**
     * Takes over the villager that already belongs to this shopkeeper, or spawns one if it is gone.
     * <p>
     * Adopting matters: spawning unconditionally leaves a second villager behind on every restart, and
     * killing the old one first loses the entity whenever its chunk happens to not be loaded yet.
     */
    public void spawnOrAdoptVillager() {
        if (shop == null || shop.getWorld() == null) return;
        if (villager != null && villager.isValid()) {
            if (isRightKind(villager)) return;
            villager.remove();
            villager = null;
        }
        LivingEntity existing = findOwnFigure(shop);
        if (existing == null) {
            existing = skin == null
                    ? (LivingEntity) shop.getWorld().spawnEntity(shop, EntityType.VILLAGER)
                    : shop.getWorld().spawn(shop, Mannequin.class);
        }
        villager = existing;
        applyFigureSettings();
    }

    /** @return whether the entity is the kind this shop stands as now: a mannequin with a skin, else a villager */
    private boolean isRightKind(Entity entity) {
        return skin == null ? entity instanceof Villager : entity instanceof Mannequin;
    }

    /**
     * @param location the place to look at
     * @return the figure of this shopkeeper standing in that chunk, or {@code null} - also when the chunk
     *         is not loaded, which is never loaded just to look. A figure of the wrong kind, left over
     *         from before the skin changed, is removed on the way, so exactly one figure is left.
     */
    private @Nullable LivingEntity findOwnFigure(Location location) {
        if (location.getWorld() == null) return null;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return null;
        LivingEntity found = null;
        for (Entity entity : location.getChunk().getEntities()) {
            if (!uuid.equals(shopIdOf(entity))) continue;
            if (!isRightKind(entity) || found != null) entity.remove();
            else found = (LivingEntity) entity;
        }
        return found;
    }

    /** Puts the figure into the state a shopkeeper needs: named, still, and not killable. */
    private void applyFigureSettings() {
        if (villager == null) return;
        if (villager instanceof Villager v) v.setAdult();
        villager.customName(Component.text(name == null ? "Shop" : name));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        if (villager instanceof Mannequin m) {
            m.setSilent(true);
            m.setCollidable(false);
            m.setImmovable(true);
            // description null, so the nameplate reads like the villager's
            m.setDescription(null);
            if (skin != null) m.setProfile(ResolvableProfile.resolvableProfile().name(skin).build());
        }
        villager.getPersistentDataContainer().set(SHOP_ID, PersistentDataType.STRING, uuid.toString());
    }

    /** @return the skin name, or {@code null} when the shop stands as a villager */
    public @Nullable String getSkin() {
        return skin;
    }

    /**
     * Gives the shop another skin and swaps its figure at once when its chunk is loaded; otherwise the
     * wrong figure is replaced when the chunk loads. Saves. The name is not validated here.
     *
     * @param name a Minecraft player name, or {@code null} to stand as a villager again
     */
    public void setSkin(@Nullable String name) {
        this.skin = name;
        if (villager != null && villager.isValid() && !isRightKind(villager)) {
            villager.remove();
            villager = null;
        }
        if (villager != null && villager.isValid()) applyFigureSettings();
        else {
            villager = null;
            spawnOrAdoptVillager();
        }
        ShopkeeperManager.saveAll();
    }

    /**
     * Lets go of the villager when its chunk unloads, so no reference to a dead entity is kept around.
     */
    public void releaseVillager() {
        this.villager = null;
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

    /**
     * Moves the shop to a new spot, villager and all.
     * <p>
     * The villager is teleported rather than replaced: it carries the id that ties it to this shopkeeper,
     * and killing and respawning it is how a shop ends up with two villagers - or with none, when the
     * target chunk has no entities loaded yet. A villager that is genuinely gone is spawned fresh.
     *
     * @param target where the shop should stand
     */
    public void moveTo(Location target) {
        if (target == null || target.getWorld() == null) return;
        this.shop = target;
        if (villager != null && villager.isValid()) {
            villager.teleport(target);
            applyFigureSettings();
            return;
        }
        // the old villager is not loaded, so it cannot be moved - it is taken out where it stands and a
        // new one is put down here, which keeps exactly one villager per shop either way
        LivingEntity stale = findOwnFigure(target);
        if (stale != null) stale.remove();
        this.villager = null;
        spawnOrAdoptVillager();
    }

    // ------------------------------------------------------------------ the stock

    /**
     * @return the stock chest, or {@code null} when there is no chest at the spot any more
     */
    private @Nullable Inventory chestInventory() {
        if (chest == null || chest.getBlock().getType() != Material.CHEST) return null;
        return ((Chest) chest.getBlock().getState()).getInventory();
    }

    /**
     * @param stock the chest
     * @param offer an offer
     * @return how many times the offer can be bought out of that chest
     */
    private static int lotsIn(Inventory stock, ItemForSale offer) {
        ItemStack wanted = offer.getItemClone();
        if (wanted == null || wanted.getAmount() <= 0) return 0;
        int amount = 0;
        for (ItemStack stack : stock.getContents()) {
            if (stack != null && stack.isSimilar(wanted)) amount += stack.getAmount();
        }
        return amount / wanted.getAmount();
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
        Inventory stock = chestInventory();
        for (ItemForSale offer : items) offer.setLastKnownStock(stock == null ? 0 : lotsIn(stock, offer));
    }

    /**
     * @param offer the offer to look up
     * @return how many whole lots of that offer the chest can still deliver
     */
    public int getStock(ItemForSale offer) {
        if (chest == null || chest.getWorld() == null) return 0;
        // touching the block would load its chunk. The marketplace asks every shop at once, so that would
        // drag every shop chunk on the server in just to draw a list - an unloaded shop answers from the
        // stock it last saw instead.
        if (!chest.getWorld().isChunkLoaded(chest.getBlockX() >> 4, chest.getBlockZ() >> 4)) {
            return offer.getLastKnownStock();
        }
        Inventory stock = chestInventory();
        int lots = stock == null ? 0 : lotsIn(stock, offer);
        offer.setLastKnownStock(lots);
        return lots;
    }

    // ------------------------------------------------------------------ selling

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
        Inventory stock = chestInventory();
        if (stock == null) {
            player.sendMessage("Die Kiste dieses Shops gibt es nicht mehr.");
            return;
        }
        ItemStack wanted = item.getItemClone();
        if (wanted == null || wanted.getType().isAir() || wanted.getAmount() <= 0) {
            player.sendMessage("Dieses Angebot ist kaputt und kann nicht gekauft werden.");
            return;
        }
        if (TeamService.getTeam(ownerTeam) == null) {
            player.sendMessage(TeamService.isLoaded()
                    ? "Diesen Shop gibt es nicht mehr - sein Team wurde aufgelöst."
                    : "Die Teams werden gerade noch geladen - versuch es gleich noch einmal.");
            return;
        }
        if (lotsIn(stock, item) < 1) {
            player.sendMessage("Die Kiste dieses Shops hat davon nichts mehr auf Lager.");
            return;
        }
        if (!MoneyHandler.removeMoney(item.getPrice(), player.getUniqueId())) {
            player.sendMessage("Dafür hast du nicht genug Bits.");
            return;
        }

        // the chest changes without a player opening it, which CoreProtect would never see: the buyer is
        // who took the goods out, so the log names them
        CoreProtectLog.container(player.getName(), chest.getBlock().getLocation());
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

        MoneyHandler.addTeamMoney(item.getPrice(), ownerTeam);
        item.recordSale();
        item.setLastKnownStock(lotsIn(stock, item));
        // a sale changes both the stock and the counter, so it must not wait for the next autosave
        ShopkeeperManager.saveAll();
        player.sendMessage("Gekauft: " + wanted.getAmount() + "x " + wanted.getType().name()
                + " für " + item.getPrice() + " Bits.");
    }

    // ------------------------------------------------------------------ buying back

    /**
     * Buys one lot of an offer from a player, if the offer buys back.
     * <p>
     * The order is fixed by the change design: check everything and take the lot out of the player's
     * inventory on the main thread, debit the team through the launcher (never the local copy, which two
     * sellers could both be told yes), and only on its answer put the lot into the chest and pay the player.
     * Any failure on the way gives the lot back and leaves no bits moved.
     *
     * @param player who is selling
     * @param item   the offer being sold into
     * @param done   run on the main thread once a started sale has ended, whatever its outcome (a sale
     *               refused up front never starts and does not call it); may be {@code null}. Callers
     *               redraw their menu here and must check the player still has it open.
     */
    public void sellItem(Player player, ItemForSale item, @Nullable Runnable done) {
        Inventory stock = chestInventory();
        ItemStack lot = item.getItemClone();
        boolean valid = item.isValid() && lot != null;
        int lotSize = valid ? lot.getAmount() : 0;
        int price = item.getBuybackPrice();
        BuybackRules.Refusal refusal = BuybackRules.check(valid, item.isBuyback(), stock != null,
                TeamService.getTeam(ownerTeam) != null, lotSize,
                valid ? countHeld(player, lot) : 0, valid && stock != null ? roomFor(stock, lot) : 0,
                MoneyService.get(ownerTeam), price);
        if (refusal != null) {
            player.sendMessage(refusal.message());
            return;
        }

        takeFromPlayer(player, lot);
        MoneyService.changeAsync(ownerTeam, -price, true, "survival buyback", result -> {
            if (!result.isSuccessful()) {
                giveBack(player, lot);
                player.sendMessage(result.getMessage() == null || result.getMessage().isBlank()
                        ? "Der Verkauf hat nicht geklappt - nichts wurde bewegt."
                        : "Der Verkauf hat nicht geklappt: " + result.getMessage());
                finish(done);
                return;
            }
            Inventory now = chestInventory();
            Map<Integer, ItemStack> unplaced = now == null ? null : now.addItem(lot.clone());
            if (now == null || !unplaced.isEmpty()) {
                // the chest changed while the host was answering: what addItem did place comes out again,
                // so the lot exists exactly once, and the team gets its bits back
                if (now != null) {
                    int left = 0;
                    for (ItemStack rest : unplaced.values()) left += rest.getAmount();
                    if (left < lotSize) {
                        ItemStack placed = lot.clone();
                        placed.setAmount(lotSize - left);
                        now.removeItem(placed);
                    }
                }
                MoneyService.change(ownerTeam, price, false, "survival buyback refund");
                giveBack(player, lot);
                player.sendMessage("Die Kiste des Shops ist inzwischen voll - nichts wurde verkauft.");
                finish(done);
                return;
            }
            CoreProtectLog.container(player.getName(), chest.getBlock().getLocation());
            MoneyHandler.addMoney(price, player.getUniqueId());
            item.setLastKnownStock(lotsIn(now, item));
            player.sendMessage("Verkauft: " + lotSize + "x " + lot.getType().name() + " für " + price + " Bits.");
            finish(done);
        });
    }

    private static void finish(@Nullable Runnable done) {
        if (done != null) done.run();
    }

    /** @return how many items identical to the lot the player carries, armor and off hand left out */
    private static int countHeld(Player player, ItemStack lot) {
        int held = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(lot)) held += stack.getAmount();
        }
        return held;
    }

    /** @return how many items like the lot still fit into the inventory */
    private static int roomFor(Inventory inventory, ItemStack lot) {
        int room = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) room += lot.getMaxStackSize();
            else if (stack.isSimilar(lot)) room += Math.max(0, stack.getMaxStackSize() - stack.getAmount());
        }
        return room;
    }

    /** Removes the lot from the player's storage slots; the caller has checked they hold it. */
    private static void takeFromPlayer(Player player, ItemStack lot) {
        ItemStack[] slots = player.getInventory().getStorageContents();
        int open = lot.getAmount();
        for (int i = 0; i < slots.length && open > 0; i++) {
            ItemStack stack = slots[i];
            if (stack == null || !stack.isSimilar(lot)) continue;
            int take = Math.min(open, stack.getAmount());
            open -= take;
            if (take == stack.getAmount()) slots[i] = null;
            else stack.setAmount(stack.getAmount() - take);
        }
        player.getInventory().setStorageContents(slots);
    }

    /** Hands a reserved lot back; what does not fit, or a player who left, ends up in the world. */
    private void giveBack(Player player, ItemStack lot) {
        Map<Integer, ItemStack> spill = player.isOnline()
                ? player.getInventory().addItem(lot.clone()) : Map.of(0, lot.clone());
        for (ItemStack rest : spill.values()) {
            Location at = player.isOnline() ? player.getLocation()
                    : chest.getBlock().getLocation().add(0.5, 1, 0.5);
            at.getWorld().dropItemNaturally(at, rest);
        }
    }

    // ------------------------------------------------------------------ plain data

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Location getShop() {
        return shop;
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

    /**
     * @param ownerTeam the team the shop belongs to from now on - which is only ever the same team under
     *                  its new name, see {@link ShopkeeperManager#onTeamRenamed}
     */
    void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public List<ItemForSale> getItems() {
        return items;
    }

    public void setItems(List<ItemForSale> items) {
        this.items = items;
    }
}
