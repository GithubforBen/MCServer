package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import de.hems.paper.customInventory.CustomInventory;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Every offer for one item, so a seller can see who they are competing with and how far they would have to
 * go down to be the cheapest.
 */
public class OfferListUi {

    private static final int CONTENT_START = 9;
    private static final int PAGE_SIZE = 36;
    private static final int SIZE = 9 * 6;

    private final Player player;
    private final MarketplaceUi parent;
    private final MarketListing listing;

    public OfferListUi(Player player, MarketplaceUi parent, MarketListing listing) {
        this.player = player;
        this.parent = parent;
        this.listing = listing;
    }

    /**
     * @return the panel listing every seller of this item, cheapest first
     */
    public CustomInventory build() {
        CustomInventory inventory = new CustomInventory(SIZE, "Anbieter - " + listing.displayName(), e -> {
        });
        inventory.fillPlaceHolder();

        inventory.setItem(4, MarketplaceUi.withLore(listing.getSample(), listing.displayName(),
                NamedTextColor.WHITE, List.of(
                        "Anbieter: " + listing.sellerCount(),
                        "Bisher verkauft: " + listing.totalSold() + "x")),
                MarketAction.handles(event -> {
                }));

        List<MarketOffer> offers = listing.getOffers();
        double best = offers.isEmpty() ? 0 : offers.getFirst().unitPrice();
        for (int i = 0; i < PAGE_SIZE && i < offers.size(); i++) {
            MarketOffer offer = offers.get(i);
            inventory.setItem(CONTENT_START + i, icon(offer, best), MarketAction.handles(event -> {
                if (offer.stock() <= 0) {
                    player.sendMessage("Dieser Anbieter ist gerade ausverkauft.");
                    return;
                }
                offer.shopkeeper().buyItem(player, offer.offer());
                parent.openLater(() -> new OfferListUi(player, parent, listing).build());
            }));
        }

        inventory.setItem(49, MarketplaceUi.label(Material.ARROW, "Zurück zum Marktplatz",
                NamedTextColor.YELLOW, List.of()), MarketAction.opens(parent::build));
        return inventory;
    }

    /**
     * @param offer the offer to draw
     * @param best  the best price per item on this listing, to mark who is currently cheapest
     * @return the icon for one seller
     */
    private ItemStack icon(MarketOffer offer, double best) {
        int stock = offer.stock();
        boolean cheapest = offer.unitPrice() - best < 1.0E-9;
        List<String> lore = new ArrayList<>();
        lore.add("Anbieter: " + offer.sellerName());
        lore.add("Preis: " + offer.offer().getPrice() + " Bits");
        lore.add("Menge: " + offer.offer().getItemClone().getAmount() + " Stück");
        lore.add("Pro Stück: " + String.format(java.util.Locale.ROOT, "%.2f", offer.unitPrice()) + " Bits");
        lore.add("Auf Lager: " + stock + "x");
        lore.add("Verkauft: " + offer.offer().getSold() + "x");
        lore.add("");
        if (stock <= 0) {
            lore.add("Ausverkauft");
        } else if (cheapest) {
            lore.add("Günstigster Anbieter");
            lore.add("Klicken zum Kaufen");
        } else {
            lore.add("Teurer als das beste Angebot");
            lore.add("Klicken zum Kaufen");
        }
        NamedTextColor color = stock <= 0 ? NamedTextColor.GRAY
                : (cheapest ? NamedTextColor.GREEN : NamedTextColor.WHITE);
        return MarketplaceUi.withLore(offer.offer().getItemClone(), offer.sellerName(), color, lore);
    }
}
