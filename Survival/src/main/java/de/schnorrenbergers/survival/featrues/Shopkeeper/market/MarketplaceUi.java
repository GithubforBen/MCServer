package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import de.hems.paper.customInventory.CustomInventory;
import de.schnorrenbergers.survival.Survival;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The marketplace panel: every offer on the server in one place, with tabs, sorting and the competing
 * offers behind each item.
 * <p>
 * One of these belongs to one player - it carries which tab they opened and how they sorted it.
 */
public class MarketplaceUi {

    /** Where the listings start and how many fit on a page. */
    private static final int CONTENT_START = 9;
    private static final int PAGE_SIZE = 36;
    private static final int SIZE = 9 * 6;

    private final Player player;
    private MarketCategory category = MarketCategory.ALL;
    private MarketSort sort = MarketSort.NAME;
    private boolean showOutOfStock;
    private boolean onlySold;
    private int page;

    public MarketplaceUi(Player player) {
        this.player = player;
    }

    /**
     * Opens the marketplace for a player.
     *
     * @param player who wants to shop
     */
    public static void open(Player player) {
        MarketplaceUi ui = new MarketplaceUi(player);
        CustomInventory.show(player, ui.build());
    }

    /**
     * @return the panel as it looks with the current tab, sorting and filters
     */
    public CustomInventory build() {
        List<MarketListing> listings = Marketplace.view(category, sort, showOutOfStock, onlySold);
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) PAGE_SIZE));
        if (page >= pages) page = pages - 1;

        CustomInventory inventory = new CustomInventory(SIZE, "Marktplatz - " + category.getTitle(), e -> {
        });
        inventory.fillPlaceHolder();
        drawTabs(inventory);
        drawControls(inventory, listings.size());
        drawListings(inventory, listings);
        drawPaging(inventory, pages);
        return inventory;
    }

    /** The category tabs along the top row. */
    private void drawTabs(CustomInventory inventory) {
        MarketCategory[] categories = MarketCategory.values();
        for (int i = 0; i < categories.length && i < 6; i++) {
            MarketCategory tab = categories[i];
            boolean active = tab == category;
            ItemStack icon = label(tab.getIcon(), (active ? "» " : "") + tab.getTitle(),
                    active ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                    active ? List.of("Wird gerade angezeigt") : List.of("Klicken zum Wechseln"));
            inventory.setItem(i, icon, MarketAction.opens(() -> {
                category = tab;
                page = 0;
                return build();
            }));
        }
    }

    /** Sorting and the two filters, on the right of the top row. */
    private void drawControls(CustomInventory inventory, int total) {
        inventory.setItem(6, label(sort.getIcon(), "Sortierung: " + sort.getTitle(), NamedTextColor.AQUA,
                List.of("Klicken für die nächste Sortierung")), MarketAction.opens(() -> {
            sort = sort.next();
            page = 0;
            return build();
        }));

        inventory.setItem(7, label(showOutOfStock ? Material.LIME_DYE : Material.GRAY_DYE,
                "Ausverkaufte anzeigen: " + (showOutOfStock ? "an" : "aus"),
                showOutOfStock ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                List.of("Zeigt auch Angebote, deren Kiste leer ist")), MarketAction.opens(() -> {
            showOutOfStock = !showOutOfStock;
            page = 0;
            return build();
        }));

        inventory.setItem(8, label(onlySold ? Material.LIME_DYE : Material.GRAY_DYE,
                "Nur schon Verkauftes: " + (onlySold ? "an" : "aus"),
                onlySold ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                List.of("Blendet Angebote aus, die noch nie gekauft wurden",
                        "Hilft gegen zugespammte Listen")), MarketAction.opens(() -> {
            onlySold = !onlySold;
            page = 0;
            return build();
        }));
    }

    /** The offers themselves. */
    private void drawListings(CustomInventory inventory, List<MarketListing> listings) {
        // unused slots keep the placeholder from fillPlaceHolder(). Clearing them would leave truly empty
        // slots, and the click listener ignores those - so a player could park items in the marketplace
        // and lose them when it closes.
        int first = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && first + i < listings.size(); i++) {
            MarketListing listing = listings.get(first + i);
            inventory.setItem(CONTENT_START + i, icon(listing), MarketAction.handles(event -> {
                if (event.isRightClick()) {
                    openLater(() -> new OfferListUi(player, this, listing).build());
                    return;
                }
                Marketplace.buy(player, listing);
                // the purchase changed the stock, so the panel has to be redrawn from fresh numbers
                openLater(this::build);
            }));
        }
    }

    /** Page arrows and the summary in the bottom row. */
    private void drawPaging(CustomInventory inventory, int pages) {
        if (page > 0) {
            inventory.setItem(45, label(Material.ARROW, "Zurück", NamedTextColor.YELLOW,
                    List.of("Seite " + page + " von " + pages)), MarketAction.opens(() -> {
                page--;
                return build();
            }));
        }
        inventory.setItem(49, label(Material.BOOK, "Seite " + (page + 1) + " von " + pages,
                NamedTextColor.WHITE, List.of(
                        "Linksklick auf ein Item kauft es",
                        "beim günstigsten Anbieter.",
                        "Rechtsklick zeigt alle Anbieter.")), MarketAction.handles(event -> {
        }));
        if (page + 1 < pages) {
            inventory.setItem(53, label(Material.ARROW, "Weiter", NamedTextColor.YELLOW,
                    List.of("Seite " + (page + 2) + " von " + pages)), MarketAction.opens(() -> {
                page++;
                return build();
            }));
        }
    }

    /**
     * @param listing the item to draw
     * @return its icon, with price, stock and competition in the tooltip
     */
    private ItemStack icon(MarketListing listing) {
        ItemStack item = listing.getSample();
        List<MarketOffer> available = listing.getAvailableOffers();
        List<String> lore = new ArrayList<>();
        if (available.isEmpty()) {
            lore.add("Ausverkauft");
        } else {
            MarketOffer best = available.getFirst();
            lore.add("Bester Preis: " + best.offer().getPrice() + " Bits");
            lore.add("für " + best.offer().getItemClone().getAmount() + " Stück");
            lore.add("Anbieter: " + best.sellerName());
            int stock = 0;
            for (MarketOffer offer : available) stock += offer.stock();
            lore.add("Verfügbar: " + stock + "x");
        }
        lore.add("Anbieter insgesamt: " + listing.sellerCount());
        lore.add("Bisher verkauft: " + listing.totalSold() + "x");
        lore.add("");
        lore.add("Linksklick: günstigstes kaufen");
        lore.add("Rechtsklick: alle Anbieter");
        return withLore(item, listing.displayName(),
                available.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.WHITE, lore);
    }

    /**
     * Opens a panel on the next tick. Rebuilding inside the click itself would read the chest before the
     * purchase has landed, so the player would be shown the old stock.
     *
     * @param supplier what to build once the click is done
     */
    void openLater(java.util.function.Supplier<CustomInventory> supplier) {
        Bukkit.getScheduler().runTask(Survival.getInstance(),
                () -> CustomInventory.show(player, supplier.get()));
    }

    static ItemStack label(Material material, String title, NamedTextColor color, List<String> lore) {
        return withLore(new ItemStack(material), title, color, lore);
    }

    static ItemStack withLore(ItemStack item, String title, NamedTextColor color, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    Player getPlayer() {
        return player;
    }
}
