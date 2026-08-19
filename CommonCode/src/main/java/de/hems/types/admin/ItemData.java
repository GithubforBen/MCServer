package de.hems.types.admin;

import java.io.Serializable;
import java.util.Base64;
import java.util.List;

/**
 * One item of an inventory, in the shape it travels over the network and reaches the browser.
 * <p>
 * Next to the readable fields it carries the bytes bukkit itself serialises the item into. That is what
 * makes editing safe: an item the admin did not touch is written back from exactly those bytes, so
 * enchantments, custom names, durability and any plugin data survive a round trip through the website even
 * though the browser knows nothing about them.
 */
public class ItemData implements Serializable {

    private static final long serialVersionUID = 3001L;

    private int slot;
    private String material;
    private int amount;
    private String displayName;
    private List<String> lore;
    private List<String> enchantments;
    private int damage;
    private int maxDurability;
    /** What {@code ItemStack.serializeAsBytes()} produced, for a lossless write back. */
    private byte[] raw;

    public ItemData() {
    }

    public ItemData(int slot, String material, int amount, String displayName, List<String> lore,
                    List<String> enchantments, int damage, int maxDurability, byte[] raw) {
        this.slot = slot;
        this.material = material;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = lore;
        this.enchantments = enchantments;
        this.damage = damage;
        this.maxDurability = maxDurability;
        this.raw = raw;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public int getDamage() {
        return damage;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    /**
     * @return the raw bytes as base64, which is how they are handed to the browser and back
     */
    public String getRawBase64() {
        return raw == null ? null : Base64.getEncoder().encodeToString(raw);
    }

    /**
     * @param base64 the bytes as the browser sent them back
     */
    public void setRawBase64(String base64) {
        this.raw = base64 == null || base64.isBlank() ? null : Base64.getDecoder().decode(base64);
    }
}
