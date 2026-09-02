package de.hems.paper.cosmetic;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What an admin decides about the cosmetics: which exist, which sell, and for how much.
 * <p>
 * One item per cosmetic and three ways to click it, rather than three menus deep. Every click writes
 * through to the launcher, so the price a player sees a second later is the price that was just set.
 */
public final class CosmeticAdminUi {

    private static final int SIZE = 9 * 5;
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};
    /** What one click adds to a price, and where it wraps around. */
    private static final int PRICE_STEP = 500;
    private static final int PRICE_MAX = 20000;

    private CosmeticAdminUi() {
    }

    /**
     * @param player an operator
     */
    public static void open(Player player) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Dafür bist du nicht berechtigt.");
            return;
        }
        CustomInventory.show(player, build());
    }

    private static CustomInventory build() {
        CustomInventory menu = new CustomInventory(SIZE, "Cosmetics verwalten", null);
        menu.fillPlaceHolder();
        List<CosmeticData> catalog = CosmeticService.getCatalog();
        catalog.sort((a, b) -> a.getType() == b.getType()
                ? a.getId().compareToIgnoreCase(b.getId())
                : a.getType().compareTo(b.getType()));
        for (int i = 0; i < catalog.size() && i < CONTENT.length; i++) {
            CosmeticData cosmetic = catalog.get(i);
            menu.setItem(CONTENT[i], icon(cosmetic), new SimpleItemAction(event -> {
                CosmeticData updated = cosmetic.copy();
                if (event.isShiftClick()) {
                    updated.setFree(!cosmetic.isFree());
                } else if (event.isRightClick()) {
                    updated.setPriceBits(cosmetic.getPriceBits() >= PRICE_MAX ? 0
                            : cosmetic.getPriceBits() + PRICE_STEP);
                } else if (cosmetic.isEnabled() && cosmetic.isBuyable()) {
                    updated.setBuyable(false);
                } else if (cosmetic.isEnabled()) {
                    updated.setEnabled(false);
                    updated.setBuyable(false);
                } else {
                    updated.setEnabled(true);
                    updated.setBuyable(true);
                }
                CosmeticService.saveAsync(updated);
                open((Player) event.getWhoClicked());
            }));
        }
        menu.setItem(SIZE - 9, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Aktualisieren").build(),
                new SimpleItemAction(event -> {
                    CosmeticService.refreshAsync();
                    open((Player) event.getWhoClicked());
                }));
        menu.setItem(SIZE - 1, new ItemApi(Material.BARRIER, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction(event -> CosmeticsUi.open((Player) event.getWhoClicked())));
        return menu;
    }

    private static ItemStack icon(CosmeticData cosmetic) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + cosmetic.getType().getDisplayName());
        lore.add(ChatColor.DARK_GRAY + cosmetic.getId());
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Freigeschaltet: "
                + (cosmetic.isEnabled() ? ChatColor.GREEN + "ja" : ChatColor.RED + "nein"));
        lore.add(ChatColor.GRAY + "Verkäuflich: "
                + (cosmetic.isBuyable() ? ChatColor.GREEN + "ja" : ChatColor.RED + "nein"));
        lore.add(ChatColor.GRAY + "Preis: " + ChatColor.WHITE + cosmetic.getPriceBits() + " Bits");
        lore.add(ChatColor.GRAY + "Für alle gratis: "
                + (cosmetic.isFree() ? ChatColor.GREEN + "ja" : ChatColor.DARK_GRAY + "nein"));
        if (!WinEffects.registered().contains(cosmetic.getId().toLowerCase(Locale.ROOT))
                && cosmetic.getType() == de.hems.types.cosmetic.CosmeticType.WIN_EFFECT) {
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Auf diesem Server gibt es keinen Code dafür.");
        }
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Linksklick: " + ChatColor.GRAY + "verkäuflich → nur besitzbar → aus");
        lore.add(ChatColor.YELLOW + "Rechtsklick: " + ChatColor.GRAY + "Preis +" + PRICE_STEP);
        lore.add(ChatColor.YELLOW + "Shift: " + ChatColor.GRAY + "für alle gratis an/aus");

        Material icon = Material.GRAY_DYE;
        if (cosmetic.isEnabled()) {
            Material named = cosmetic.getIcon() == null ? null
                    : Material.matchMaterial(cosmetic.getIcon().toUpperCase(Locale.ROOT));
            icon = named == null ? Material.NAME_TAG : named;
        }
        ChatColor colour = cosmetic.isEnabled() ? ChatColor.AQUA : ChatColor.DARK_GRAY;
        return new ItemApi(icon, colour + cosmetic.getDisplayName(), lore).build();
    }
}
