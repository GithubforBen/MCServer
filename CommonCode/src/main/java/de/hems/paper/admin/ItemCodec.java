package de.hems.paper.admin;

import de.hems.types.admin.ItemData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates between bukkit items and the shape the website works with.
 * <p>
 * Every item keeps the bytes bukkit serialises it into. The browser only ever changes material and amount;
 * anything it left alone is rebuilt from those bytes, so enchantments, custom names and plugin data are not
 * quietly lost by a round trip through a web form.
 */
public final class ItemCodec {

    private ItemCodec() {
    }

    /**
     * @param contents the slots of a container, holes included
     * @return one entry per filled slot
     */
    public static List<ItemData> toData(ItemStack[] contents) {
        List<ItemData> items = new ArrayList<>();
        if (contents == null) return items;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemData data = toData(slot, contents[slot]);
            if (data != null) items.add(data);
        }
        return items;
    }

    /**
     * @param slot where the item sits
     * @param item the item, may be {@code null} or air
     * @return its description, or {@code null} for an empty slot
     */
    public static ItemData toData(int slot, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return null;

        String displayName = null;
        List<String> lore = null;
        int damage = 0;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) displayName = meta.getDisplayName();
                if (meta.hasLore()) lore = meta.getLore();
                if (meta instanceof Damageable damageable && damageable.hasDamage()) {
                    damage = damageable.getDamage();
                }
            }
        }

        List<String> enchantments = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            enchantments.add(entry.getKey().getKey().getKey() + " " + entry.getValue());
        }

        byte[] raw;
        try {
            raw = item.serializeAsBytes();
        } catch (RuntimeException e) {
            // an item bukkit cannot serialise is still worth showing, it just cannot be edited losslessly
            raw = null;
        }

        return new ItemData(slot, item.getType().name(), item.getAmount(), displayName, lore,
                enchantments, damage, item.getType().getMaxDurability(), raw);
    }

    /**
     * Rebuilds the slots of a container from what the browser sent back.
     *
     * @param items the entries the browser sent, empty slots simply missing
     * @param size  how many slots the container has
     * @return the array to hand to {@code setContents}
     */
    public static ItemStack[] toContents(List<ItemData> items, int size) {
        ItemStack[] contents = new ItemStack[size];
        if (items == null) return contents;
        for (ItemData data : items) {
            int slot = data.getSlot();
            if (slot < 0 || slot >= size) continue;
            ItemStack item = toItem(data);
            if (item != null) contents[slot] = item;
        }
        return contents;
    }

    /**
     * @param data one slot as the browser sent it
     * @return the item, or {@code null} if the slot should stay empty
     */
    public static ItemStack toItem(ItemData data) {
        if (data == null) return null;
        Material material = data.getMaterial() == null ? null : Material.matchMaterial(data.getMaterial());
        if (material == null || material.isAir()) return null;
        int amount = Math.max(1, Math.min(material.getMaxStackSize(), data.getAmount()));

        // an untouched item comes back exactly as it was, with everything the browser never saw
        if (data.getRaw() != null && data.getRaw().length > 0) {
            try {
                ItemStack restored = ItemStack.deserializeBytes(data.getRaw());
                if (restored != null && restored.getType() == material) {
                    restored.setAmount(amount);
                    return restored;
                }
            } catch (RuntimeException e) {
                // the bytes no longer deserialise - fall through and build a plain item
            }
        }
        return new ItemStack(material, amount);
    }
}
