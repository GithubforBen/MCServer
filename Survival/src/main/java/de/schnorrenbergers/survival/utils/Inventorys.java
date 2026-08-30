package de.schnorrenbergers.survival.utils;

import de.hems.api.ItemApi;
import de.hems.api.UUIDApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.InventoryBase;
import de.hems.paper.customInventory.types.ItemAction;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ItemForSale;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperListener;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.featrues.money.AtmHandler;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.schnorrenbergers.survival.featrues.team.TeamColor;
import de.schnorrenbergers.survival.featrues.team.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
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
        customInventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"),
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

    /**
     * What the owner of a shop can do with it.
     */
    public static CustomInventory ADMIN_SHOPKEEPER_INVENTORY(Shopkeeper shopkeeper) {
        CustomInventory customInventory = new CustomInventory(9 * 5, "Shopkeeper:" + shopkeeper.getUuid(), (event) -> {
        });
        for (int i = 0; i < 9 * 5; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(10, new ItemApi(Material.CHEST, ChatColor.YELLOW + "Kistenstandort ändern",
                        List.of(ChatColor.GRAY + "Danach die neue Kiste anklicken")).build(),
                new SimpleItemAction((event) -> startPicking(event, shopkeeper,
                        ShopkeeperListener.CHEST_PICK, "Klicke jetzt die Kiste an, die zu diesem Shop gehören soll.")));

        customInventory.setItem(12, new ItemApi(Material.MINECART, ChatColor.YELLOW + "Shop verschieben",
                        List.of(ChatColor.GRAY + "Danach den Block anklicken,",
                                ChatColor.GRAY + "auf dem der Shop stehen soll",
                                ChatColor.DARK_GRAY + "Nur in eigenen Chunks")).build(),
                new SimpleItemAction((event) -> startPicking(event, shopkeeper,
                        ShopkeeperListener.SHOP_PICK, "Klicke jetzt den Block an, auf dem der Shop stehen soll.")));

        customInventory.setItem(14, new ItemApi(Material.DIAMOND, ChatColor.AQUA + "Gegenstände bearbeiten",
                        List.of(ChatColor.GRAY + "Angebote, Preise und Mengen")).build(),
                SimpleItemAction.opens(() -> {
                    try {
                        return ITEM_MANAGER_INVENTORY(shopkeeper, 1);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }));
        return customInventory;
    }

    /**
     * Puts the player into "click a block now" mode for one shop.
     * <p>
     * The mark lives on the player rather than in a map, so it survives a relog and cannot leak when
     * somebody opens the menu and walks off. A line of particles points at the shop the whole time, which
     * is the only way to tell which of several shops is being edited.
     *
     * @param event      the click that started it
     * @param shopkeeper the shop being edited
     * @param key        what is being picked
     * @param message    what to tell the player to click
     */
    private static void startPicking(InventoryClickEvent event, Shopkeeper shopkeeper,
                                     NamespacedKey key, String message) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // only one pick at a time, otherwise the next click would be claimed by whichever ran first
        player.getPersistentDataContainer().remove(ShopkeeperListener.CHEST_PICK);
        player.getPersistentDataContainer().remove(ShopkeeperListener.SHOP_PICK);
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, shopkeeper.getUuid().toString());
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + message);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()
                        || player.getPersistentDataContainer().get(key, PersistentDataType.STRING) == null) {
                    cancel();
                    return;
                }
                Shopkeeper current = ShopkeeperManager.getShopkeeper(shopkeeper.getUuid());
                if (current == null || current.getShop() == null) {
                    cancel();
                    return;
                }
                new ParticleLine(player.getLocation(), current.getShop(), Particle.HAPPY_VILLAGER, 0.1)
                        .drawParticleLine();
            }
        }.runTaskTimer(Survival.getInstance(), 0L, 10L);
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
            ItemStack item = itemForSale.getItemClone();
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
            ItemStack pending = event.getInventory().getItem(4);
            // addItem(null) throws, and closing with an empty slot is the normal case
            if (pending != null && !pending.getType().isAir()) {
                event.getPlayer().getInventory().addItem(pending);
            }
        });
        customInventory.fillPlaceHolder();
        customInventory.removeItem(4);
        customInventory.addBackButton(6, UUIDApi.fromString(shopkeeper.getUuid().toString() + ".backfromitem"), ITEM_MANAGER_INVENTORY(shopkeeper, 1));


        customInventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"), new ItemAction() {

            @Override
            public UUID getID() {
                return UUIDApi.fromString(shopkeeper.getUuid().toString() + ".imi.deeper.confirmitem");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                ItemStack offered = event.getInventory().getItem(4);
                if (offered == null || offered.getType().isAir()) {
                    event.getWhoClicked().sendMessage("Leg erst einen Gegenstand in den Slot.");
                    return;
                }
                shopkeeper.getItems().add(new ItemForSale(offered.clone(), 1));
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

    /** The step sizes a price can be nudged by, so a four digit price is not fifty clicks away. */
    private static final int[] PRICE_STEPS = {1, 5, 10, 50, 100, 1000};
    /** The step sizes for the amount. Capped by the stack size, so anything above 64 would be pointless. */
    private static final int[] AMOUNT_STEPS = {1, 5, 10, 16, 32, 64};
    /** The highest price an offer can be set to, so a slip of the finger cannot produce nonsense. */
    private static final int MAX_PRICE = 1_000_000;

    /**
     * @param steps   the steps to pick from
     * @param current the step that is selected
     * @param forward whether to go up or down the list
     * @return the next step, wrapping around at both ends
     */
    private static int nextStep(int[] steps, int current, boolean forward) {
        int index = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == current) index = i;
        }
        index = forward ? index + 1 : index - 1;
        if (index >= steps.length) index = 0;
        if (index < 0) index = steps.length - 1;
        return steps[index];
    }

    /**
     * What a click on a menu that has to know which button was pressed does.
     */
    private interface ClickedMenu {
        CustomInventory build(InventoryClickEvent event) throws MalformedURLException;
    }

    /**
     * Builds a button of the price and amount editors.
     * <p>
     * The menu is rebuilt from the changed values and drawn into the screen the player already has open,
     * which is what makes holding a step size down feel like a slider rather than a slideshow.
     *
     * @param onClick what the button changes, or {@code null} for one that only navigates
     * @param rebuild the menu to show afterwards, with the click in hand
     * @return the action for the button
     */
    private static ItemAction stepAction(java.util.function.Consumer<InventoryClickEvent> onClick,
                                         ClickedMenu rebuild) {
        return new ItemAction() {
            private final UUID id = UUID.randomUUID();

            @Override
            public UUID getID() {
                return id;
            }

            @Override
            public void onClick(InventoryClickEvent event) throws MalformedURLException {
                if (onClick != null) onClick.accept(event);
                // the menu depends on which mouse button was used, which loadInventoryOnClick never sees
                CustomInventory.show(event.getWhoClicked(), rebuild.build(event));
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
        };
    }

    /**
     * @param onClick what the button changes, or {@code null} for one that only navigates
     * @param rebuild the menu to show afterwards
     * @return the action for the button
     */
    private static ItemAction stepAction(java.util.function.Consumer<InventoryClickEvent> onClick,
                                         MenuSupplier rebuild) {
        return stepAction(onClick, (event) -> rebuild.build());
    }

    /**
     * A menu that does not care which button was pressed.
     */
    private interface MenuSupplier {
        CustomInventory build() throws MalformedURLException;
    }

    private static ItemStack minusIcon(int step) {
        return new ItemApi(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-" + step,
                List.of(ChatColor.GRAY + "Verringert um " + step)).build();
    }

    private static ItemStack plusIcon(int step) {
        return new ItemApi(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+" + step,
                List.of(ChatColor.GRAY + "Erhöht um " + step)).build();
    }

    /**
     * @param item the offer to reprice
     * @param by   how much to change it, may be negative
     */
    private static void changePrice(ItemForSale item, int by) {
        item.setPrice(Math.max(0, Math.min(MAX_PRICE, item.getPrice() + by)));
    }

    /**
     * @param item the offer to change
     * @param by   how many pieces to add or take away
     */
    private static void changeAmount(ItemForSale item, int by) {
        int amount = Math.max(1, Math.min(64, item.getItemClone().getAmount() + by));
        item.getItemOrginal().setAmount(amount);
    }

    /**
     * The button that picks how much one click changes.
     *
     * @param steps the steps to pick from
     * @param step  the step that is selected
     * @return the item to show
     */
    private static ItemStack stepIcon(int[] steps, int step) {
        StringBuilder available = new StringBuilder();
        for (int candidate : steps) {
            if (!available.isEmpty()) available.append("  ");
            available.append(candidate == step ? ChatColor.GREEN + "»x" + candidate : ChatColor.DARK_GRAY + "x" + candidate);
        }
        return new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Schrittweite: x" + step, List.of(
                available.toString(),
                ChatColor.GRAY + "Linksklick: nächste Schrittweite",
                ChatColor.GRAY + "Rechtsklick: vorherige")).build();
    }

    public static CustomInventory CHANGE_ITEM_COST_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        return CHANGE_ITEM_COST_INVENTORY(shopkeeper, item, PRICE_STEPS[0]);
    }

    /**
     * The price editor.
     * <p>
     * The step is a parameter rather than remembered state: the menu is rebuilt on every click anyway, so
     * carrying it along is all it takes, and nothing has to be cleaned up when the player walks away.
     *
     * @param shopkeeper the shop the offer belongs to
     * @param item       the offer being priced
     * @param step       how much one click changes the price
     */
    public static CustomInventory CHANGE_ITEM_COST_INVENTORY(Shopkeeper shopkeeper, ItemForSale item, int step)
            throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER,
                shopkeeper.getName() + ":" + item.getItemClone().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();

        customInventory.setItem(0, stepIcon(PRICE_STEPS, step), stepAction(null,
                event -> CHANGE_ITEM_COST_INVENTORY(shopkeeper, item,
                        nextStep(PRICE_STEPS, step, !event.isRightClick()))));

        customInventory.setItem(3, minusIcon(step), stepAction(event ->
                changePrice(item, -step), () -> CHANGE_ITEM_COST_INVENTORY(shopkeeper, item, step)));

        ItemStack clone = item.getItemClone();
        List<Component> lore = clone.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.addFirst(Component.text(ChatColor.GRAY + "Linksklick: -" + step + "   Rechtsklick: +" + step));
        lore.addFirst(Component.text(ChatColor.WHITE + "Kosten: " + item.getPrice() + " Bits"));
        clone.lore(lore);
        customInventory.setItem(4, clone, stepAction(event ->
                changePrice(item, event.isRightClick() ? step : -step),
                () -> CHANGE_ITEM_COST_INVENTORY(shopkeeper, item, step)));

        customInventory.setItem(5, plusIcon(step), stepAction(event ->
                changePrice(item, step), () -> CHANGE_ITEM_COST_INVENTORY(shopkeeper, item, step)));

        customInventory.setItem(6, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                stepAction(null, () -> ITEM_MANAGE_INVENTORY(shopkeeper, item)));
        customInventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"),
                stepAction(event -> ShopkeeperManager.saveAll(), () -> ITEM_MANAGE_INVENTORY(shopkeeper, item)));
        return customInventory;
    }

    public static CustomInventory CHANGE_ITEM_AMOUNT_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        return CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item, AMOUNT_STEPS[0]);
    }

    /**
     * The editor for how many pieces one purchase hands over.
     *
     * @param shopkeeper the shop the offer belongs to
     * @param item       the offer being changed
     * @param step       how much one click changes the amount
     */
    public static CustomInventory CHANGE_ITEM_AMOUNT_INVENTORY(Shopkeeper shopkeeper, ItemForSale item, int step)
            throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER,
                shopkeeper.getName() + ":" + item.getItemClone().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();

        customInventory.setItem(0, stepIcon(AMOUNT_STEPS, step), stepAction(null,
                event -> CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item,
                        nextStep(AMOUNT_STEPS, step, !event.isRightClick()))));

        customInventory.setItem(3, minusIcon(step), stepAction(event ->
                changeAmount(item, -step), () -> CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item, step)));

        int amount = item.getItemClone().getAmount();
        customInventory.setItem(4, new ItemApi(item.getItemClone().getType(),
                        ChatColor.WHITE + "Anzahl: " + amount,
                        // a stack of zero would render as nothing, and the button would be unclickable
                        Math.max(1, amount)).build(),
                stepAction(event -> changeAmount(item, event.isRightClick() ? step : -step),
                        () -> CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item, step)));

        customInventory.setItem(5, plusIcon(step), stepAction(event ->
                changeAmount(item, step), () -> CHANGE_ITEM_AMOUNT_INVENTORY(shopkeeper, item, step)));

        customInventory.setItem(6, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                stepAction(null, () -> ITEM_MANAGE_INVENTORY(shopkeeper, item)));
        customInventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"),
                stepAction(event -> ShopkeeperManager.saveAll(), () -> ITEM_MANAGE_INVENTORY(shopkeeper, item)));
        return customInventory;
    }

    public static CustomInventory ITEM_MANAGE_INVENTORY(Shopkeeper shopkeeper, ItemForSale item) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, shopkeeper.getName() + ":" + item.getItemClone().getType().toString(), (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.setItem(3, new ItemApi(Material.DIAMOND, "Change Cost").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItemClone().getType().toString() + ".imi.deeper.cost");
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
        ItemStack clone = item.getItemClone();
        List<Component> lore = clone.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.addFirst(Component.text("Kosten: " + item.getPrice()));
        clone.lore(lore);
        customInventory.setItem(4, clone, ItemAction.NOTMOVABLE);
        customInventory.setItem(5, new ItemApi(Material.CHEST, "Change Amount").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(item.getItemClone().getType().toString() + ".imi.deeper.amount");
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
                return UUIDApi.fromString(item.getItemClone().getType().toString() + ".imi.deeper");
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

        customInventory.setItem(8, ItemApi.CHECKMARKSKULL( ChatColor.GREEN + "Bestätigen"), new ItemAction() {

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
    /**
     * The balance of the account an ATM screen belongs to, as an item.
     * <p>
     * Without it the only way to find out what is on the account is to try a payout and read the error,
     * which is how somebody ends up guessing at the amount they can take out.
     *
     * @param account the team name or player uuid the ATM belongs to
     * @return the item to show
     */
    private static ItemStack balanceIcon(String account) {
        int bits = AtmHandler.balance(account);
        return new ItemApi(Material.GOLD_INGOT, ChatColor.GOLD + "Kontostand: " + bits + " Bits", List.of(
                ChatColor.GRAY + "Konto: " + ChatColor.WHITE + AtmHandler.nameOf(account),
                ChatColor.GRAY + "Das sind " + (bits / AtmHandler.BITS_PER_ITEM) + " Diamanten")).build();
    }

    public static CustomInventory ATM_INVENTORY(String user) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geldautomat", (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.setItem(13, balanceIcon(user), ItemAction.NOTMOVABLE);

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
                    return ATM_DEPOSIT_INVENTORY(user);
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
                    return ATM_PAYOUT_INVENTORY(user);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return customInventory;
    }

    public static CustomInventory ATM_DEPOSIT_INVENTORY(String user) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.DARK_GREEN + "Geld einzahlen", (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("cd283a6b-48d5-4b0b-a96a-f9f0955b20c6"), ATM_INVENTORY(user));
        customInventory.setItem(4, balanceIcon(user), ItemAction.NOTMOVABLE);

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"8e3a39d2-cb10-4fdf-b502-16fa5eaaaa13", "4181a762-de4d-490f-a00f-0134de937062", "a8606788-f848-4473-aadf-c47a7691a150"};

        for (int i = 0; i < amountMap.length; i++) {
            int amount = amountMap[i];

            ItemStack itemStack = new ItemApi(Material.DIAMOND, ChatColor.BLUE.toString() + amount*100 + " Bits einzahlen", amount).build();

            int finalI = i;
            customInventory.setItem(currentInventoryPos, itemStack, new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.fromString(uuidMap[finalI]);
                }

                @Override
                public void onClick(InventoryClickEvent event) throws MalformedURLException {
                    Player player = (Player) event.getWhoClicked();
                    AtmHandler.deposit(player, user, amount);
                    // the balance just changed, so the screen is drawn again with the new number
                    CustomInventory.show(player, ATM_DEPOSIT_INVENTORY(user));
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

    public static CustomInventory ATM_PAYOUT_INVENTORY(String user) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.CHEST, ChatColor.RED + "Geld auszahlen", (event) -> {
        });
        customInventory.fillPlaceHolder();
        customInventory.addBackButton(18, UUID.fromString("39ed12c5-6a5c-4f52-8f4b-6d8bc2869f81"), ATM_INVENTORY(user));
        customInventory.setItem(4, balanceIcon(user), ItemAction.NOTMOVABLE);

        // 1, 32, 64
        int currentInventoryPos = 10;
        int[] amountMap = {1, 32, 64};
        String[] uuidMap = {"1db84b7f-625e-40ec-b21d-5ec010022294", "b0933f0a-9618-4255-ad83-92c91cad4b75", "843e033d-c4b6-4ec0-919a-ae90b75c138a"};

        for (int i = 0; i < amountMap.length; i++) {
            int amount = amountMap[i];

            ItemStack itemStack = new ItemApi(Material.DIAMOND,
                    ChatColor.BLUE.toString() + amount * AtmHandler.BITS_PER_ITEM + " Bits auszahlen", amount).build();

            int finalI = i;
            customInventory.setItem(currentInventoryPos, itemStack, new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.fromString(uuidMap[finalI]);
                }

                @Override
                public void onClick(InventoryClickEvent event) throws MalformedURLException {
                    Player player = (Player) event.getWhoClicked();
                    AtmHandler.payout(player, user, amount);
                    CustomInventory.show(player, ATM_PAYOUT_INVENTORY(user));
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

    public static CustomInventory TEAM_ATM_INVENTORY(String team) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, "Team manager", (x) -> {

        });
        customInventory.fillPlaceHolder();
        customInventory.setItem(5, balanceIcon(team), ItemAction.NOTMOVABLE);
        customInventory.setItem(3, new ItemApi(Material.CHEST, ChatColor.AQUA + "Team-ATM",
                List.of(ChatColor.GRAY + "Ein- und auszahlen")).build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUIDApi.fromString(team + ".atm");
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
                return ATM_INVENTORY(team);
            }
        });
        return customInventory;
    }

}
