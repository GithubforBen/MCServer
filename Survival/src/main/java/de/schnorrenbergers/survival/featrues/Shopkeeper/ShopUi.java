package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.ItemAction;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.survival.Survival;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * What a customer sees when they click a shop's villager: every offer, with price and stock, one click to
 * buy.
 */
public final class ShopUi {

    /** Rows two to five hold the offers; the first row is the frame and the last one the page buttons. */
    static final int FIRST_SLOT = 9;
    static final int PAGE_SIZE = 36;
    static final int SIZE = 9 * 6;

    private ShopUi() {
    }

    /**
     * @param player who clicked
     * @param shop   the shop
     */
    public static void open(Player player, Shopkeeper shop) {
        player.openInventory(build(shop, 1).getInventory());
    }

    /**
     * @param shop the shop
     * @param page which page, from 1
     * @return the menu
     */
    static CustomInventory build(Shopkeeper shop, int page) {
        CustomInventory inventory = new CustomInventory(SIZE, shop.getName(), event -> {
        });
        inventory.fillPlaceHolder();
        List<ItemForSale> offers = new ArrayList<>();
        for (ItemForSale offer : shop.getItems()) if (offer.isValid()) offers.add(offer);
        fillPage(inventory, offers, page,
                offer -> withLoreOnTop(offer.getItemClone(),
                        "Preis: " + offer.getPrice() + " Bits",
                        "Auf Lager: " + shop.getStock(offer) + "x"),
                offer -> new SimpleItemAction(event -> {
                    Player buyer = (Player) event.getWhoClicked();
                    shop.buyItem(buyer, offer);
                    // the shop stays open and redraws itself, so buying twice does not mean walking up to
                    // the villager again. Next tick, because the chest has not changed yet inside the click
                    Bukkit.getScheduler().runTask(Survival.getInstance(),
                            () -> CustomInventory.show(buyer, build(shop, page)));
                }),
                other -> build(shop, other));
        return inventory;
    }

    /**
     * Lays one page of offers into a menu, with the buttons to the pages before and after it.
     * <p>
     * Shared by the shop and its editor, which used to carry a copy each - with the same off-by-one: the
     * 37th offer of a page was drawn into the slot of the back button, and again at the top of the next page.
     *
     * @param inventory the menu
     * @param offers    everything there is to show
     * @param page      which page, from 1
     * @param icon      what an offer looks like
     * @param action    what clicking an offer does
     * @param openPage  builds another page
     */
    static void fillPage(CustomInventory inventory, List<ItemForSale> offers, int page,
                         Function<ItemForSale, ItemStack> icon, Function<ItemForSale, ItemAction> action,
                         IntFunction<CustomInventory> openPage) {
        int from = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && from + i < offers.size(); i++) {
            ItemForSale offer = offers.get(from + i);
            inventory.setItem(FIRST_SLOT + i, icon.apply(offer), action.apply(offer));
        }
        if (page > 1) {
            inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                    SimpleItemAction.opens(() -> openPage.apply(page - 1)));
        }
        if (offers.size() > page * PAGE_SIZE) {
            inventory.setItem(SIZE - 1, new ItemApi(Material.ARROW, ChatColor.GRAY + "Weiter").build(),
                    SimpleItemAction.opens(() -> openPage.apply(page + 1)));
        }
    }

    /**
     * @param item  an item, which is changed
     * @param lines what to put in front of its own lore
     * @return the item
     */
    static ItemStack withLoreOnTop(ItemStack item, String... lines) {
        List<Component> lore = item.lore() == null ? new ArrayList<>() : new ArrayList<>(item.lore());
        for (int i = lines.length - 1; i >= 0; i--) lore.addFirst(Component.text(lines[i]));
        item.lore(lore);
        return item;
    }
}
