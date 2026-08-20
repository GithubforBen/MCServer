package de.schnorrenbergers.bedwars.shop.item;

import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.TeamColor;
import de.schnorrenbergers.bedwars.shop.Cost;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One entry of {@code shop.yml}.
 * <p>
 * Most entries are simply "this many of that material for this price". The four flags after it are the
 * exceptions hypixel's shop is built out of, and each one exists because a player would otherwise lose
 * something they paid for: armour and shears stay through a death, a tool falls back by one level instead
 * of disappearing, a sword replaces the wooden one rather than filling the hotbar with three of them, and
 * a block bought in a team's colour has to be that team's colour rather than white.
 *
 * @param id           how the entry is referred to, unique across the whole shop
 * @param category     which page it sits on
 * @param displayName  what it is called, MiniMessage
 * @param material     what is handed over
 * @param amount       how many
 * @param costs        what it costs, one part per currency
 * @param lore         extra lines under the name, MiniMessage
 * @param enchantments what the item comes enchanted with
 * @param effects      the potion effects the item carries, for the potions
 * @param teamBlock    which family of team coloured block the material is swapped for, if any
 * @param permanent    whether the buyer keeps it through a death
 * @param armorTier    the armour level this entry sets, 0 when it is not armour
 * @param toolGroup    which tool chain it belongs to, e.g. {@code pickaxe}, or {@code null}
 * @param toolTier     which step of that chain it is
 * @param sword        whether buying it replaces the sword the buyer carries
 * @param slot         where it sits in the menu, -1 to let the shop place it
 * @param lifetime     how many seconds what it summons stays around, 0 for as long as it likes
 * @param enemyOnly    whether it is only sold at another team's keeper, which is what makes an item
 *                     something you have to walk into a hostile base for
 */
public record ShopItem(String id, String category, String displayName, Material material, int amount,
                       List<Cost> costs, List<String> lore,
                       Map<Enchantment, Integer> enchantments, List<PotionEffect> effects,
                       ShopItem.TeamBlock teamBlock, boolean permanent, int armorTier,
                       @Nullable String toolGroup, int toolTier, boolean sword, int slot,
                       int lifetime, boolean enemyOnly) {

    /**
     * The block families that come in team colours.
     */
    public enum TeamBlock {

        NONE,
        WOOL,
        GLASS,
        TERRACOTTA,
        CONCRETE;

        /**
         * @param fallback what the entry names as its material
         * @param color    the team buying it, or {@code null} for somebody without a team
         * @return the block in that team's colour, or the fallback when this entry is not coloured
         */
        public Material apply(Material fallback, @Nullable TeamColor color) {
            if (this == NONE || color == null) return fallback;
            return switch (this) {
                case WOOL -> color.getWool();
                case GLASS -> color.getGlass();
                case TERRACOTTA -> color.getTerracotta();
                case CONCRETE -> color.getConcrete();
                default -> fallback;
            };
        }

        /**
         * @param name what a config says
         * @return that family, {@link #NONE} when it says nothing usable
         */
        public static TeamBlock byName(String name) {
            if (name == null) return NONE;
            for (TeamBlock block : values()) {
                if (block.name().equalsIgnoreCase(name)) return block;
            }
            return NONE;
        }
    }

    /**
     * @param seller whose keeper is being clicked, {@code null} for a shop that belongs to nobody
     * @param buyer  which team is looking at it
     * @return whether this entry may be bought here at all
     */
    public boolean sellableBy(@Nullable GameTeam seller, @Nullable GameTeam buyer) {
        if (!enemyOnly) return true;
        return seller != null && !seller.equals(buyer);
    }

    /**
     * @return whether this entry is an armour level rather than an item to carry
     */
    public boolean isArmor() {
        return armorTier > 0;
    }

    /**
     * @return whether this entry is a step of a tool chain
     */
    public boolean isTool() {
        return toolGroup != null && !toolGroup.isBlank();
    }

    /**
     * @return whether the buyer keeps this through a death, in one form or another
     */
    public boolean survivesDeath() {
        return permanent || isArmor() || isTool();
    }
}
