package de.hems.paper.cosmetic;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.money.MoneyService;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cosmetics shop.
 * <p>
 * One page per kind, and every entry is one button whose meaning depends on where the player stands with
 * it: buy it, put it on, take it off. A shop that needs a second click to work out which of the three it
 * is doing is a shop nobody uses twice.
 */
public final class CosmeticsUi {

    private static final int SIZE = 9 * 5;
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};

    /** Which tab each player last had open, so buying something does not throw them back to page one. */
    private static final Map<UUID, CosmeticType> tabs = new ConcurrentHashMap<>();

    private CosmeticsUi() {
    }

    /**
     * @param player who is shopping
     */
    public static void open(Player player) {
        open(player, tabs.getOrDefault(player.getUniqueId(), CosmeticType.WIN_EFFECT));
    }

    /**
     * @param player who is shopping
     * @param type   which page
     */
    public static void open(Player player, CosmeticType type) {
        tabs.put(player.getUniqueId(), type);
        if (!CosmeticService.isLoaded()) {
            player.sendMessage(ChatColor.GRAY + "Die Cosmetics sind noch nicht geladen - gleich nochmal.");
            CosmeticService.refreshAsync();
            return;
        }
        CustomInventory.show(player, build(player, type));
    }

    private static CustomInventory build(Player player, CosmeticType type) {
        CustomInventory menu = new CustomInventory(SIZE, "Cosmetics - " + type.getDisplayName(), null);
        menu.fillPlaceHolder();

        CosmeticType[] types = CosmeticType.values();
        for (int i = 0; i < types.length; i++) {
            CosmeticType tab = types[i];
            boolean active = tab == type;
            menu.setItem(i, new ItemApi(active ? Material.LIME_DYE : Material.GRAY_DYE,
                            (active ? ChatColor.GREEN + "» " : ChatColor.GRAY.toString()) + tab.getDisplayName(),
                            List.of(ChatColor.GRAY + tab.getDescription())).build(),
                    new SimpleItemAction(event -> open((Player) event.getWhoClicked(), tab)));
        }

        List<CosmeticData> visible = CosmeticService.getVisible(type);
        for (int i = 0; i < visible.size() && i < CONTENT.length; i++) {
            CosmeticData cosmetic = visible.get(i);
            menu.setItem(CONTENT[i], icon(player, cosmetic),
                    new SimpleItemAction(event -> click((Player) event.getWhoClicked(), cosmetic, type)));
        }
        if (visible.isEmpty()) {
            menu.setItem(22, new ItemApi(Material.GRAY_DYE, ChatColor.GRAY + "Hier gibt es noch nichts",
                            List.of(ChatColor.DARK_GRAY + "Ein Admin kann hier etwas freischalten.")).build(),
                    new SimpleItemAction(event -> {
                    }));
        }

        menu.setItem(SIZE - 5, new ItemApi(Material.DIAMOND, ChatColor.AQUA + "Dein Guthaben",
                        List.of(ChatColor.WHITE + String.valueOf(MoneyService.get(player.getUniqueId()))
                                + " Bits")).build(),
                new SimpleItemAction(event -> {
                }));
        if (player.isOp()) {
            menu.setItem(SIZE - 1, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Verwalten",
                            List.of(ChatColor.GRAY + "Freischalten, verkaufen, Preise")).build(),
                    new SimpleItemAction(event -> CosmeticAdminUi.open((Player) event.getWhoClicked())));
        }
        return menu;
    }

    /**
     * One entry, drawn as what it currently is to this player.
     */
    private static ItemStack icon(Player player, CosmeticData cosmetic) {
        UUID id = player.getUniqueId();
        boolean owned = CosmeticService.owns(id, cosmetic.getId());
        CosmeticData worn = CosmeticService.getSelected(id, cosmetic.getType());
        boolean selected = worn != null && worn.getId().equalsIgnoreCase(cosmetic.getId());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + cosmetic.getDescription());
        lore.add(" ");
        if (selected) {
            lore.add(ChatColor.GREEN + "Angelegt");
            lore.add(ChatColor.YELLOW + "Klicken zum Ablegen");
        } else if (owned) {
            lore.add(ChatColor.GRAY + "Du besitzt das.");
            lore.add(ChatColor.YELLOW + "Klicken zum Anlegen");
        } else if (!cosmetic.isBuyable()) {
            lore.add(ChatColor.RED + "Steht gerade nicht zum Verkauf.");
        } else {
            int balance = MoneyService.get(id);
            lore.add(ChatColor.GRAY + "Preis: " + ChatColor.WHITE + cosmetic.getPriceBits() + " Bits");
            lore.add(balance >= cosmetic.getPriceBits()
                    ? ChatColor.YELLOW + "Klicken zum Kaufen"
                    : ChatColor.RED + "Dir fehlen " + (cosmetic.getPriceBits() - balance) + " Bits.");
        }
        Material material = material(cosmetic, owned);
        ChatColor colour = selected ? ChatColor.GREEN : (owned ? ChatColor.AQUA : ChatColor.GRAY);
        return new ItemApi(material, colour + cosmetic.getDisplayName(), lore).build();
    }

    /**
     * @param cosmetic the cosmetic
     * @param owned    whether the player has it
     * @return the item it is drawn as - its own once it is theirs, grey while it is not
     */
    private static Material material(CosmeticData cosmetic, boolean owned) {
        if (!owned) return Material.GRAY_DYE;
        Material named = cosmetic.getIcon() == null ? null
                : Material.matchMaterial(cosmetic.getIcon().toUpperCase(Locale.ROOT));
        return named == null ? Material.NAME_TAG : named;
    }

    /**
     * Buys, wears or takes off, depending on where the player stands with it.
     */
    private static void click(Player player, CosmeticData cosmetic, CosmeticType tab) {
        UUID id = player.getUniqueId();
        if (CosmeticService.owns(id, cosmetic.getId())) {
            CosmeticData worn = CosmeticService.getSelected(id, cosmetic.getType());
            boolean selected = worn != null && worn.getId().equalsIgnoreCase(cosmetic.getId());
            CosmeticService.selectAsync(id, cosmetic.getType(), selected ? null : cosmetic.getId());
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, selected ? 0.9f : 1.3f);
            open(player, tab);
            return;
        }
        if (!cosmetic.isBuyable()) {
            player.sendMessage(ChatColor.RED + "Das steht gerade nicht zum Verkauf.");
            return;
        }
        player.closeInventory();
        player.sendMessage(ChatColor.GRAY + "Kauf wird geprüft ...");
        CosmeticService.buyAsync(id, cosmetic.getId(), purchase -> {
            if (!purchase.isSuccessful()) {
                player.sendMessage(ChatColor.RED + (purchase.getMessage() == null
                        ? "Der Kauf hat nicht geklappt." : purchase.getMessage()));
                return;
            }
            player.sendMessage(ChatColor.GREEN + cosmetic.getDisplayName() + " gehört dir - "
                    + purchase.getPaid() + " Bits bezahlt, " + purchase.getBalance() + " übrig.");
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            // straight on, because the next thing anybody does after buying one is put it on
            CosmeticService.selectAsync(id, cosmetic.getType(), cosmetic.getId());
            open(player, tab);
        });
    }
}
