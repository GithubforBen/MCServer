package de.schnorrenbergers.bedwars.shop;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.ShopSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.item.ShopCategory;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The item shop.
 * <p>
 * One page per category, the tabs along the top, and the menu is rebuilt after every purchase - a shop
 * that keeps showing "you cannot afford this" on an item you just paid for is worse than no prices at all.
 */
public final class ShopMenu {

    private static final int SIZE = 54;

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
     * Opens one page of the shop.
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
        for (ShopItem item : category.items()) {
            // an entry that belongs to another team's keeper is not greyed out here, it is not here at
            // all: half the point of it is that nobody sees it in their own base
            if (!item.sellableBy(seller, shopper.getTeam())) continue;
            int slot = item.slot() >= 0 ? item.slot() : slotFor(next++);
            if (slot < 0 || slot >= SIZE) continue;
            menu.setItem(slot, ShopItems.icon(item, player, shopper.getTeam(), owned(shopper, item)),
                    new SimpleItemAction(event -> {
                        event.getWhoClicked().closeInventory();
                        buy(player, shopper, item, seller, category);
                    }));
        }
        player.openInventory(menu.getInventory());
    }

    /**
     * Draws the tabs along the top row.
     */
    private static void drawTabs(CustomInventory menu, ShopSettings settings, Player player,
                                 @Nullable GameTeam seller, ShopCategory open) {
        int next = 0;
        for (ShopCategory category : settings.getCategories()) {
            int slot = category.slot() >= 0 ? category.slot() : next++;
            if (slot < 0 || slot > 8) continue;
            menu.setItem(slot, tab(category, category.id().equals(open.id())),
                    new SimpleItemAction(event -> {
                        event.getWhoClicked().closeInventory();
                        open(player, seller, category);
                    }));
        }
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
     * Buys an entry and shows the page again, so that the new prices and what is now owned are visible.
     */
    private static void buy(Player player, GamePlayer shopper, ShopItem item, @Nullable GameTeam seller,
                            ShopCategory category) {
        Game game = Bedwars.getInstance().getGame();
        if (!game.isRunning() || !shopper.isAlive()) return;
        Bedwars.getInstance().getShop().buy(game, shopper, item, seller);
        Bukkit.getScheduler().runTask(Bedwars.getInstance(), () -> {
            if (player.isOnline()) open(player, seller, category);
        });
    }

    /**
     * @return the line that says the player already has this, or {@code null} when they do not
     */
    private static @Nullable Component owned(GamePlayer shopper, ShopItem item) {
        if (item.isArmor() && shopper.getLoadout().getArmorTier() >= item.armorTier()) {
            return Messages.get("shop.owned");
        }
        if (item.isTool() && shopper.getLoadout().getToolTier(item.toolGroup()) >= item.toolTier()) {
            return Messages.get("shop.owned");
        }
        if (item.permanent() && shopper.getLoadout().getPermanent().contains(item.id())) {
            return Messages.get("shop.owned");
        }
        return null;
    }

    /**
     * @param index how many entries have been placed already
     * @return where the next one goes, -1 once the page is full
     */
    private static int slotFor(int index) {
        return index < CONTENT.length ? CONTENT[index] : -1;
    }
}
