package de.schnorrenbergers.bedwars.shop.item;

import org.bukkit.Material;

import java.util.List;

/**
 * One page of the shop.
 *
 * @param id          how it is referred to
 * @param displayName the tab's name, MiniMessage
 * @param icon        the item the tab is drawn as
 * @param slot        where the tab sits in the top row
 * @param items       what is sold on it, in the order they were configured
 */
public record ShopCategory(String id, String displayName, Material icon, int slot, List<ShopItem> items) {
}
