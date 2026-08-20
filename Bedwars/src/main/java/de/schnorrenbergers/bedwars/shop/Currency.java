package de.schnorrenbergers.bedwars.shop;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The four things a player pays with.
 * <p>
 * A currency is nothing but a material that is counted rather than used, which is why paying lives here
 * and not in the shop: the upgrade villager, the trap queue and every addon that costs something take
 * their price the same way.
 */
public enum Currency {

    IRON(Material.IRON_INGOT, "Iron", NamedTextColor.WHITE),
    GOLD(Material.GOLD_INGOT, "Gold", NamedTextColor.GOLD),
    DIAMOND(Material.DIAMOND, "Diamond", NamedTextColor.AQUA),
    EMERALD(Material.EMERALD, "Emerald", NamedTextColor.GREEN);

    private final Material material;
    private final String displayName;
    private final NamedTextColor color;

    Currency(Material material, String displayName, NamedTextColor color) {
        this.material = material;
        this.displayName = displayName;
        this.color = color;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    /**
     * @param player who to look at
     * @return how much of this they are carrying
     */
    public int count(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) total += stack.getAmount();
        }
        return total;
    }

    /**
     * Takes a price out of somebody's inventory.
     * <p>
     * Counts first and only then takes, so a purchase that turns out to be too expensive never leaves the
     * buyer with half of it paid.
     *
     * @param player who pays
     * @param amount how much
     * @return whether they could afford it
     */
    public boolean take(Player player, int amount) {
        if (amount <= 0) return true;
        if (count(player) < amount) return false;
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && left > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() != material) continue;
            int taken = Math.min(left, stack.getAmount());
            left -= taken;
            if (taken >= stack.getAmount()) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
                player.getInventory().setItem(slot, stack);
            }
        }
        return true;
    }

    /**
     * @param name a currency as it is written in a config
     * @return that currency, or {@code null} when there is none by that name
     */
    public static @Nullable Currency byName(String name) {
        if (name == null) return null;
        for (Currency currency : values()) {
            if (currency.name().equalsIgnoreCase(name)) return currency;
        }
        return null;
    }
}
