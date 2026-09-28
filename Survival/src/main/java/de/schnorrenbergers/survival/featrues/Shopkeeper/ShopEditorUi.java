package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.ItemAction;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * What the owners of a shop can do with it: move it, give it another chest, and put together what it
 * sells - which items, for how much, how many at a time.
 * <p>
 * These menus used to live in {@code Inventorys}, a thousand lines of every menu of the server, each button
 * spelled out as its own anonymous class. They are the shop's, so they are here now, and a button is one
 * line.
 */
public final class ShopEditorUi {

    /** The step sizes a price can be nudged by, so a four digit price is not fifty clicks away. */
    private static final int[] PRICE_STEPS = {1, 5, 10, 50, 100, 1000};
    /** The step sizes for the amount. Capped by the stack size, so anything above 64 would be pointless. */
    private static final int[] AMOUNT_STEPS = {1, 5, 10, 16, 32, 64};
    /** The highest price an offer can be set to, so a slip of the finger cannot produce nonsense. */
    private static final int MAX_PRICE = 1_000_000;
    /** The green plus head used for "new offer". */
    private static final String NEW_OFFER_TEXTURE =
            "http://textures.minecraft.net/texture/5ff31431d64587ff6ef98c0675810681f8c13bf96f51d9cb07ed7852b2ffd1";

    private ShopEditorUi() {
    }

    /**
     * @param player an owner of the shop
     * @param shop   the shop
     */
    public static void open(Player player, Shopkeeper shop) {
        player.openInventory(settings(shop).getInventory());
    }

    /**
     * The first menu: what the owner of a shop can do with it.
     */
    static CustomInventory settings(Shopkeeper shop) {
        CustomInventory inventory = new CustomInventory(9 * 5, "Shop: " + shop.getName(), event -> {
        });
        inventory.fillPlaceHolder();
        inventory.setItem(10, new ItemApi(Material.CHEST, ChatColor.YELLOW + "Kistenstandort ändern",
                        List.of(ChatColor.GRAY + "Danach die neue Kiste anklicken")).build(),
                new SimpleItemAction(event -> startPicking(event, shop, ShopkeeperListener.CHEST_PICK,
                        "Klicke jetzt die Kiste an, die zu diesem Shop gehören soll.")));
        inventory.setItem(12, new ItemApi(Material.MINECART, ChatColor.YELLOW + "Shop verschieben",
                        List.of(ChatColor.GRAY + "Danach den Block anklicken,",
                                ChatColor.GRAY + "auf dem der Shop stehen soll",
                                ChatColor.DARK_GRAY + "Nur in eigenen Chunks")).build(),
                new SimpleItemAction(event -> startPicking(event, shop, ShopkeeperListener.SHOP_PICK,
                        "Klicke jetzt den Block an, auf dem der Shop stehen soll.")));
        inventory.setItem(14, new ItemApi(Material.DIAMOND, ChatColor.AQUA + "Gegenstände bearbeiten",
                        List.of(ChatColor.GRAY + "Angebote, Preise und Mengen")).build(),
                SimpleItemAction.opens(() -> offers(shop, 1)));
        return inventory;
    }

    /**
     * Puts the player into "click a block now" mode for one shop.
     * <p>
     * The mark lives on the player rather than in a map, so it survives a relog and cannot leak when
     * somebody opens the menu and walks off. A line of particles points at the shop the whole time, which
     * is the only way to tell which of several shops is being edited.
     */
    private static void startPicking(InventoryClickEvent event, Shopkeeper shop, NamespacedKey key, String message) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // only one pick at a time, otherwise the next click would be claimed by whichever ran first
        ShopkeeperListener.stopPicking(player);
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, shop.getUuid().toString());
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
                Shopkeeper current = ShopkeeperManager.getShopkeeper(shop.getUuid());
                if (current == null || current.getShop() == null) {
                    cancel();
                    return;
                }
                new ParticleLine(player.getLocation(), current.getShop(), Particle.HAPPY_VILLAGER, 0.1)
                        .drawParticleLine();
            }
        }.runTaskTimer(Survival.getInstance(), 0L, 10L);
    }

    // ------------------------------------------------------------------ the offers

    /**
     * Every offer of the shop, to pick one to change, and the button for a new one.
     */
    static CustomInventory offers(Shopkeeper shop, int page) {
        CustomInventory inventory = new CustomInventory(ShopUi.SIZE, shop.getName(), event -> {
        });
        inventory.fillPlaceHolder();
        ShopUi.fillPage(inventory, shop.getItems(), page,
                offer -> ShopUi.withLoreOnTop(offer.getItemClone(), "Preis: " + offer.getPrice() + " Bits"),
                offer -> SimpleItemAction.opens(() -> offer(shop, offer)),
                other -> offers(shop, other));
        inventory.setItem(ShopUi.SIZE - 2, newOfferIcon(), SimpleItemAction.opens(() -> addOffer(shop)));
        return inventory;
    }

    private static ItemStack newOfferIcon() {
        try {
            return new ItemApi(new URL(NEW_OFFER_TEXTURE), ChatColor.GREEN + "Neues Angebot").buildSkull();
        } catch (MalformedURLException e) {
            return new ItemApi(Material.LIME_DYE, ChatColor.GREEN + "Neues Angebot").build();
        }
    }

    /**
     * A dropper with one free slot in the middle: whatever is put there becomes a new offer.
     */
    static CustomInventory addOffer(Shopkeeper shop) {
        CustomInventory inventory = new CustomInventory(InventoryType.DROPPER, shop.getName() + ": Neues Angebot",
                event -> {
                    ItemStack pending = event.getInventory().getItem(4);
                    // the item is only the pattern of the offer - the stock is what is in the chest - so it goes
                    // back to the player. addItem(null) throws, and closing with an empty slot is the normal case
                    if (pending != null && !pending.getType().isAir()) {
                        event.getPlayer().getInventory().addItem(pending);
                    }
                });
        inventory.fillPlaceHolder();
        inventory.removeItem(4);
        inventory.setItem(6, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                SimpleItemAction.opens(() -> offers(shop, 1)));
        inventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"), new SimpleItemAction(event -> {
            ItemStack offered = event.getInventory().getItem(4);
            if (offered == null || offered.getType().isAir()) {
                event.getWhoClicked().sendMessage("Leg erst einen Gegenstand in den Slot.");
                return;
            }
            shop.getItems().add(new ItemForSale(offered.clone(), 1));
            ShopkeeperManager.saveAll();
            event.getWhoClicked().closeInventory();
            event.getWhoClicked().sendMessage("Angebot hinzugefügt - jetzt noch den Preis setzen.");
        }));
        return inventory;
    }

    /**
     * One offer: its price, its amount, and taking it out.
     */
    static CustomInventory offer(Shopkeeper shop, ItemForSale offer) {
        CustomInventory inventory = new CustomInventory(InventoryType.DROPPER, title(shop, offer), event -> {
        });
        inventory.fillPlaceHolder();
        inventory.setItem(3, new ItemApi(Material.DIAMOND, ChatColor.AQUA + "Preis ändern").build(),
                SimpleItemAction.opens(() -> price(shop, offer, PRICE_STEPS[0])));
        inventory.setItem(4, ShopUi.withLoreOnTop(offer.getItemClone(), "Kosten: " + offer.getPrice() + " Bits"),
                ItemAction.NOTMOVABLE);
        inventory.setItem(5, new ItemApi(Material.CHEST, ChatColor.AQUA + "Menge ändern").build(),
                SimpleItemAction.opens(() -> amount(shop, offer, AMOUNT_STEPS[0])));
        inventory.setItem(6, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                SimpleItemAction.opens(() -> offers(shop, 1)));
        inventory.setItem(7, new ItemApi(Material.BARRIER, ChatColor.RED + "Angebot löschen").build(),
                SimpleItemAction.opens(event -> {
                    shop.getItems().remove(offer);
                    // taking an offer out is a real change, and used to wait for the next autosave
                    ShopkeeperManager.saveAll();
                }, () -> offers(shop, 1)));
        inventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"),
                SimpleItemAction.opens(() -> offers(shop, 1)));
        return inventory;
    }

    private static String title(Shopkeeper shop, ItemForSale offer) {
        return shop.getName() + ":" + offer.getItemClone().getType();
    }

    // ------------------------------------------------------------------ price and amount

    /**
     * The price editor.
     * <p>
     * The step is a parameter rather than remembered state: the menu is rebuilt on every click anyway, so
     * carrying it along is all it takes, and nothing has to be cleaned up when the player walks away.
     */
    static CustomInventory price(Shopkeeper shop, ItemForSale offer, int step) {
        ItemStack shown = ShopUi.withLoreOnTop(offer.getItemClone(),
                ChatColor.WHITE + "Kosten: " + offer.getPrice() + " Bits",
                ChatColor.GRAY + "Linksklick: -" + step + "   Rechtsklick: +" + step);
        return stepper(shop, offer, PRICE_STEPS, step, shown,
                by -> offer.setPrice(Math.max(0, Math.min(MAX_PRICE, offer.getPrice() + by))),
                next -> price(shop, offer, next));
    }

    /**
     * The editor for how many pieces one purchase hands over.
     */
    static CustomInventory amount(Shopkeeper shop, ItemForSale offer, int step) {
        int amount = offer.getItemClone().getAmount();
        ItemStack shown = new ItemApi(offer.getItemClone().getType(), ChatColor.WHITE + "Anzahl: " + amount,
                // a stack of zero would render as nothing, and the button would be unclickable
                Math.max(1, amount)).build();
        return stepper(shop, offer, AMOUNT_STEPS, step, shown,
                by -> offer.getItemOrginal().setAmount(Math.max(1, Math.min(64, offer.getItemClone().getAmount() + by))),
                next -> amount(shop, offer, next));
    }

    /**
     * The price and the amount editor are the same menu around a different number: a step size, minus,
     * the value, plus, back and confirm. The menu is rebuilt from the changed value and drawn into the
     * screen the player already has open, which makes holding a step down feel like a slider.
     *
     * @param steps   the step sizes on offer
     * @param step    the one that is selected
     * @param shown   the item in the middle, showing the value
     * @param change  changes the value by the given amount
     * @param rebuild the same menu with the given step size
     */
    private static CustomInventory stepper(Shopkeeper shop, ItemForSale offer, int[] steps, int step,
                                           ItemStack shown, Consumer<Integer> change,
                                           Function<Integer, CustomInventory> rebuild) {
        CustomInventory inventory = new CustomInventory(InventoryType.DROPPER, title(shop, offer), event -> {
        });
        inventory.fillPlaceHolder();
        inventory.setItem(0, stepIcon(steps, step),
                redraw(null, event -> rebuild.apply(nextStep(steps, step, !event.isRightClick()))));
        inventory.setItem(3, new ItemApi(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-" + step,
                        List.of(ChatColor.GRAY + "Verringert um " + step)).build(),
                redraw(event -> change.accept(-step), event -> rebuild.apply(step)));
        inventory.setItem(4, shown,
                redraw(event -> change.accept(event.isRightClick() ? step : -step), event -> rebuild.apply(step)));
        inventory.setItem(5, new ItemApi(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+" + step,
                        List.of(ChatColor.GRAY + "Erhöht um " + step)).build(),
                redraw(event -> change.accept(step), event -> rebuild.apply(step)));
        inventory.setItem(6, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                SimpleItemAction.opens(() -> offer(shop, offer)));
        inventory.setItem(8, ItemApi.CHECKMARKSKULL(ChatColor.GREEN + "Bestätigen"),
                SimpleItemAction.opens(event -> ShopkeeperManager.saveAll(), () -> offer(shop, offer)));
        return inventory;
    }

    /**
     * A button whose next menu depends on which mouse button was used - which
     * {@link ItemAction#loadInventoryOnClick()} never sees, so the redraw happens in the click itself.
     */
    private static ItemAction redraw(Consumer<InventoryClickEvent> onClick,
                                     Function<InventoryClickEvent, CustomInventory> next) {
        return new SimpleItemAction(event -> {
            if (onClick != null) onClick.accept(event);
            CustomInventory.show(event.getWhoClicked(), next.apply(event));
        });
    }

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
        index = Math.floorMod(index + (forward ? 1 : -1), steps.length);
        return steps[index];
    }

    /**
     * The button that picks how much one click changes.
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
}
