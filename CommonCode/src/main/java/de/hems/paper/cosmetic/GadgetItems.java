package de.hems.paper.cosmetic;

import de.hems.api.ItemApi;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

/**
 * The items the gadgets hand out, and how one is told apart from the same thing bought in a shop.
 * <p>
 * Every gadget item carries the id of the gadget it belongs to. Without that the snowball cannon would
 * answer to any snowball on the server and the mobile workbench would answer to every crafting table
 * anybody ever placed - which on survival is the difference between a cosmetic and a change to the game.
 */
public final class GadgetItems {

    /** The id of the gadget an item belongs to. */
    private static final NamespacedKey GADGET = new NamespacedKey("hems", "gadget");

    private GadgetItems() {
    }

    /**
     * @param material what it looks like
     * @param gadgetId which gadget it belongs to
     * @param name     what it is called, without colours
     * @param lore     the lines under the name
     * @return the item, marked as that gadget's
     */
    public static ItemStack of(Material material, String gadgetId, String name, String... lore) {
        ItemStack item = new ItemApi(material, ChatColor.LIGHT_PURPLE + name, List.of(lore)).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(GADGET, PersistentDataType.STRING, key(gadgetId));
            // a gadget item is not a tool, and one that wears out is one its owner cannot get back
            // without dying first
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * @param item     something in somebody's hand, possibly nothing
     * @param gadgetId a gadget
     * @return whether that item is that gadget's
     */
    public static boolean is(ItemStack item, String gadgetId) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String owner = meta.getPersistentDataContainer().get(GADGET, PersistentDataType.STRING);
        return owner != null && owner.equals(key(gadgetId));
    }

    /**
     * @param player   somebody
     * @param gadgetId a gadget
     * @return whether they are already carrying that gadget's item
     */
    public static boolean has(Player player, String gadgetId) {
        for (ItemStack carried : player.getInventory().getContents()) {
            if (is(carried, gadgetId)) return true;
        }
        return false;
    }

    /**
     * Takes a gadget's items back out of somebody's inventory.
     * <p>
     * Called when they stop wearing it, which includes walking out of the world it works in - a rocket
     * that is still in the hotbar on a server where it does nothing is a rocket somebody clicks at.
     *
     * @param player   who
     * @param gadgetId which gadget's items
     */
    public static void take(Player player, String gadgetId) {
        ItemStack[] carried = player.getInventory().getContents();
        for (int i = 0; i < carried.length; i++) {
            if (is(carried[i], gadgetId)) player.getInventory().setItem(i, null);
        }
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}
