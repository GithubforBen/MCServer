package de.schnorrenbergers.bedwars.shop;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.ShopSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.item.ShopCategory;
import de.schnorrenbergers.bedwars.shop.item.ShopChain;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The item shop.
 * <p>
 * One page per category, the tabs along the top, and the page is redrawn after every purchase - a shop
 * that keeps showing "you cannot afford this" on an item you just paid for is worse than no prices at all.
 * <p>
 * Redrawn, not reopened. Every click used to close the menu and open it again a tick later, which is what
 * made the shop blink and jump back to the first page under the player's hand.
 * <p>
 * The armour levels and the tool chains are drawn as one button each, showing the step the player may buy
 * next. Four pickaxes side by side read as four items rather than as one that is upgraded, and three of
 * the four could never be bought anyway.
 */
public final class ShopMenu {

    private static final int SIZE = 54;
    /** How many tabs fit along the top. */
    private static final int TAB_ROW = 9;

    /** Where entries are put when they do not name a slot themselves: the four rows under the tabs. */
    private static final int[] CONTENT = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            46, 47, 48, 49, 50, 51, 52};

    private ShopMenu() {
    }

    /**
     * Opens the shop on its first page.
     *
     * @param player who is shopping
     * @param seller whose shop it is, {@code null} for one that belongs to nobody
     */
    public static void open(Player player, @Nullable GameTeam seller) {
        ShopCategory first = Bedwars.getInstance().getShopSettings().getFirstCategory();
        if (first == null) {
            Messages.send(player, "shop.empty");
            return;
        }
        open(player, seller, first);
    }

    /**
     * Draws one page of the shop onto the player's screen.
     *
     * @param player   who is shopping
     * @param seller   whose shop it is
     * @param category which page
     */
    public static void open(Player player, @Nullable GameTeam seller, ShopCategory category) {
        Game game = Bedwars.getInstance().getGame();
        ShopSettings settings = Bedwars.getInstance().getShopSettings();
        GamePlayer shopper = game.get(player);
        if (shopper == null) return;

        CustomInventory menu = new CustomInventory(SIZE, Text.legacy(Messages.get("shop.title",
                "category", Text.plain(category.displayName()))), null);
        menu.fillPlaceHolder();
        drawTabs(menu, settings, player, seller, category);

        int next = 0;
        for (Object entry : ShopChain.group(sellableOn(category, shopper, seller))) {
            int slot = slotOf(entry) >= 0 ? slotOf(entry) : slotFor(next++);
            if (slot < 0 || slot >= SIZE) continue;
            if (entry instanceof ShopChain chain) {
                drawStep(menu, slot, chain, player, shopper, seller, category);
            } else {
                drawItem(menu, slot, (ShopItem) entry, player, shopper, seller, category);
            }
        }
        CustomInventory.show(player, menu);
    }

    /**
     * @return the entries of a page this player may buy at this keeper
     */
    private static List<ShopItem> sellableOn(ShopCategory category, GamePlayer shopper,
                                             @Nullable GameTeam seller) {
        List<ShopItem> sellable = new ArrayList<>();
        for (ShopItem item : category.items()) {
            // an entry that belongs to another team's keeper is not greyed out here, it is not here at
            // all: half the point of it is that nobody sees it in their own base
            if (item.sellableBy(seller, shopper.getTeam())) sellable.add(item);
        }
        return sellable;
    }

    /**
     * Draws a plain entry.
     */
    private static void drawItem(CustomInventory menu, int slot, ShopItem item, Player player,
                                 GamePlayer shopper, @Nullable GameTeam seller, ShopCategory category) {
        Component owned = alreadyOwns(shopper, item) ? Messages.get("shop.owned") : null;
        menu.setItem(slot, ShopItems.icon(item, player, shopper.getTeam(), owned, List.of()),
                new SimpleItemAction(event -> buy(player, shopper, item, seller, category)));
    }

    /**
     * @return whether this entry is one the player cannot buy again
     */
    private static boolean alreadyOwns(GamePlayer shopper, ShopItem item) {
        // armour is not a chain and every level is bought on its own, so the shop has to say which levels
        // are already behind the buyer - otherwise the chainmail sits there looking buyable for the whole
        // round to somebody who is wearing diamond
        if (item.isArmor()) return shopper.getLoadout().getArmorTier() >= item.armorTier();
        return item.permanent() && shopper.getLoadout().getPermanent().contains(item.id());
    }

    /**
     * Draws one rung of a chain: the step that is up next, or the top one once there is nothing left.
     */
    private static void drawStep(CustomInventory menu, int slot, ShopChain chain, Player player,
                                 GamePlayer shopper, @Nullable GameTeam seller, ShopCategory category) {
        ShopItem step = chain.offer(shopper.getLoadout());
        boolean maxed = chain.isMaxed(shopper.getLoadout());
        List<Component> extra = List.of(Messages.get("shop.step",
                "level", String.valueOf(chain.level(shopper.getLoadout())),
                "maximum", String.valueOf(chain.size())));
        menu.setItem(slot, ShopItems.icon(step, player, shopper.getTeam(),
                        maxed ? Messages.get("shop.maxed") : null, extra),
                new SimpleItemAction(event -> {
                    if (maxed) {
                        Messages.send(player, "shop.already-owned", "item", Text.plain(step.displayName()));
                        return;
                    }
                    buy(player, shopper, step, seller, category);
                }));
    }

    /**
     * @param entry a button of the page
     * @return the slot it asks for, or -1 when it takes whatever is free
     */
    private static int slotOf(Object entry) {
        return entry instanceof ShopChain chain ? chain.slot() : ((ShopItem) entry).slot();
    }

    /**
     * Draws the tabs along the top row.
     */
    private static void drawTabs(CustomInventory menu, ShopSettings settings, Player player,
                                 @Nullable GameTeam seller, ShopCategory open) {
        boolean[] taken = new boolean[TAB_ROW];
        for (ShopCategory category : settings.getCategories()) {
            int slot = freeTab(taken, category.slot());
            if (slot < 0) continue;
            taken[slot] = true;
            menu.setItem(slot, tab(category, category.id().equals(open.id())),
                    new SimpleItemAction(event -> open(player, seller, category)));
        }
    }

    /**
     * Finds a tab a page can actually be drawn in.
     * <p>
     * Two pages that ask for the same slot used to be one page: the second was drawn over the first and
     * the first became unreachable, which is what happened to the potions the moment an addon added a page
     * of its own at the slot the potions were configured for. The wish is honoured where it can be, and
     * where it cannot the page moves rather than disappears.
     *
     * @param taken which tabs are already drawn
     * @param wish  where the page would like to sit, or negative for no preference
     * @return the slot to draw it in, or -1 when the top row is full
     */
    private static int freeTab(boolean[] taken, int wish) {
        int start = wish >= 0 && wish < TAB_ROW ? wish : 0;
        for (int offset = 0; offset < TAB_ROW; offset++) {
            int slot = (start + offset) % TAB_ROW;
            if (!taken[slot]) return slot;
        }
        return -1;
    }

    /**
     * @param category the page
     * @param current  whether it is the one being looked at
     * @return the tab item
     */
    private static ItemStack tab(ShopCategory category, boolean current) {
        ItemStack stack = new ItemStack(category.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(category.displayName()));
            if (current) meta.lore(List.of(Messages.get("shop.category.open")));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Buys an entry and redraws the page, so that the new prices and what is now owned are visible.
     */
    private static void buy(Player player, GamePlayer shopper, ShopItem item, @Nullable GameTeam seller,
                            ShopCategory category) {
        Game game = Bedwars.getInstance().getGame();
        if (!game.isRunning() || !shopper.isAlive()) return;
        Bedwars.getInstance().getShop().buy(game, shopper, item, seller);
        open(player, seller, category);
    }

    /**
     * @param index how many entries have been placed already
     * @return where the next one goes, -1 once the page is full
     */
    private static int slotFor(int index) {
        return index < CONTENT.length ? CONTENT[index] : -1;
    }
}
