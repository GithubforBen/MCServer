package de.schnorrenbergers.bedwars.shop.item;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.Cost;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns an entry of {@code shop.yml} into something a player can hold.
 * <p>
 * Every item that was bought carries the id it was bought under, so that later on anything can ask an item
 * what it is - a fireball has to know it is a fireball, and an addon has to be able to recognise its own
 * item without guessing from the material.
 */
public final class ShopItems {

    /** The shop page whose entries are plain building material. */
    private static final String BLOCK_CATEGORY = "blocks";
    /** The two halves of an armour set that are actually replaced; helmet and chestplate stay leather. */
    private static final String[] ARMOR_SLOTS = {"_BOOTS", "_LEGGINGS"};

    private ShopItems() {
    }

    /**
     * @return the tag every bought item carries
     */
    public static NamespacedKey key() {
        return new NamespacedKey(Bedwars.getInstance(), "shop-item");
    }

    /**
     * @param stack an item somebody is holding
     * @return what it was bought as, or {@code null} when it did not come out of the shop
     */
    public static @Nullable String idOf(@Nullable ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(key(), PersistentDataType.STRING);
    }

    /**
     * Builds what a purchase hands over.
     *
     * @param item the entry
     * @param team who is buying, for the team coloured blocks
     * @return the item, tagged with the entry it came from
     */
    public static ItemStack build(ShopItem item, @Nullable GameTeam team) {
        Material material = item.teamBlock().apply(item.material(),
                team == null ? null : team.getColor());
        ItemStack stack = new ItemStack(material, item.amount());
        // building material is handed over bare. A name and a tag are what make two stacks of wool refuse
        // to be one, and a player who mines their own wool back gets a plain block from the world - so a
        // named one would sit in a second slot next to it and neither would ever stack again
        if (plainBlock(item, material)) return stack;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(Text.item(item.displayName()));
        if (!item.lore().isEmpty()) meta.lore(item.lore().stream().map(Text::item).toList());
        for (Map.Entry<Enchantment, Integer> enchantment : item.enchantments().entrySet()) {
            meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);
        }
        // tools and armour never break: a diamond pickaxe that wears out mid round is a purchase taken back
        if (item.isTool() || item.isArmor() || item.permanent()) meta.setUnbreakable(true);
        if (meta instanceof PotionMeta potion) {
            for (PotionEffect effect : item.effects()) potion.addCustomEffect(effect, true);
            if (!item.effects().isEmpty()) potion.setColor(item.effects().getFirst().getType().getColor());
        }
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, item.id());
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * @param item     the entry
     * @param material what it is handed over as
     * @return whether it is plain building material rather than something that has to be recognisable
     */
    private static boolean plainBlock(ShopItem item, Material material) {
        // by page rather than by material: the jump pad is a pressure plate and the rescue platform is
        // bought as a block too, and both of them are found again by the tag that this would strip
        return BLOCK_CATEGORY.equals(item.category())
                && material.isBlock()
                && item.enchantments().isEmpty()
                && item.effects().isEmpty()
                && !item.isTool()
                && !item.isArmor()
                && !item.permanent();
    }

    /**
     * Builds the pieces an armour purchase puts on.
     *
     * @param item the armour entry, whose material names the family
     * @return boots and leggings of that family
     */
    public static List<ItemStack> armorPieces(ShopItem item) {
        String family = family(item.material());
        List<ItemStack> pieces = new ArrayList<>();
        for (String slot : ARMOR_SLOTS) {
            Material material = Material.matchMaterial(family + slot);
            if (material == null) continue;
            ItemStack piece = new ItemStack(material);
            ItemMeta meta = piece.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, item.id());
                piece.setItemMeta(meta);
            }
            pieces.add(piece);
        }
        return pieces;
    }

    /**
     * @param material a piece of armour
     * @return the family it belongs to, e.g. {@code DIAMOND} for diamond boots
     */
    private static String family(Material material) {
        String name = material.name();
        for (String slot : new String[]{"_BOOTS", "_LEGGINGS", "_CHESTPLATE", "_HELMET"}) {
            if (name.endsWith(slot)) return name.substring(0, name.length() - slot.length());
        }
        return name;
    }

    /**
     * Builds the button the shop menu shows, which is the item itself plus what it costs.
     *
     * @param item     the entry
     * @param player   who is looking at it
     * @param team     their team
     * @param owned    a line saying they already have it, or {@code null}
     * @return the button
     */
    public static ItemStack icon(ShopItem item, Player player, @Nullable GameTeam team,
                                 @Nullable Component owned) {
        return icon(item, player, team, owned, List.of());
    }

    /**
     * Builds the button the shop menu shows, which is the item itself plus what it costs.
     *
     * @param item   the entry
     * @param player who is looking at it
     * @param team   their team
     * @param owned  a line saying they already have it, or {@code null}
     * @param extra  lines between the name and the price, for what step of a ladder this is
     * @return the button
     */
    public static ItemStack icon(ShopItem item, Player player, @Nullable GameTeam team,
                                 @Nullable Component owned, List<Component> extra) {
        ItemStack stack = build(item, team);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // the button says what the entry is called even when the item itself is handed over bare, which
        // is what building material is: the name belongs to the shop page, not to the block in the hand
        meta.displayName(Text.item(item.displayName()));
        boolean affordable = Cost.shortfall(player, item.costs()) == null;
        List<Component> lore = new ArrayList<>();
        item.lore().forEach(line -> lore.add(Text.item(line)));
        lore.addAll(extra);
        lore.add(Component.empty());
        // one line per part of the price: an item that costs two things has to say both, or the second
        // one is a surprise the player only meets when the purchase is refused
        for (Cost cost : item.costs()) {
            lore.add(Messages.get("shop.price",
                    "amount", String.valueOf(cost.amount()),
                    "currency", cost.currency().getDisplayName()));
        }
        if (owned != null) {
            lore.add(owned);
        } else {
            lore.add(Messages.get(affordable ? "shop.click-to-buy" : "shop.too-expensive"));
        }
        meta.lore(lore.stream().map(line -> line.decoration(
                net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * @param material any material
     * @return its name the way it is written out for a player, e.g. {@code Iron Ingot}
     */
    public static String niceName(Material material) {
        return Text.niceName(material.name());
    }
}
