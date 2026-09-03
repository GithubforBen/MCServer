package de.schnorrenbergers.bedwars.shop.upgrade;

import de.schnorrenbergers.bedwars.shop.Currency;
import org.bukkit.Material;

import java.util.List;

/**
 * One thing a team can buy for the whole team, out of {@code upgrades.yml}.
 * <p>
 * What an upgrade <em>does</em> is one of a handful of known effects rather than free text: a config that
 * could describe arbitrary behaviour would need a language of its own, while these seven cover the
 * hypixel list, and an eighth is a new constant plus one branch in {@link UpgradeService}.
 *
 * @param id          how it is referred to
 * @param displayName what it is called, MiniMessage
 * @param icon        the item it is drawn as
 * @param slot        where it sits in the menu
 * @param effect      what buying it does
 * @param maxLevel    how far it can be pushed
 * @param prices      what each level costs, first entry being level one
 * @param currency    what those prices are in
 * @param lore        what the menu says about it, MiniMessage
 */
public record Upgrade(String id, String displayName, Material icon, int slot, Upgrade.Effect effect,
                      int maxLevel, List<Integer> prices, Currency currency, List<String> lore) {

    /**
     * What an upgrade does once it is bought.
     */
    public enum Effect {

        /** Every sword and axe of the team is sharpened. */
        SHARPNESS,
        /** Every piece of team armour is protected, one level per upgrade level. */
        PROTECTION,
        /** The team digs faster, one haste level per upgrade level. */
        HASTE,
        /** The team's own generators run one tier faster per upgrade level. */
        FORGE,
        /** Team members regenerate while they are at their own base. */
        HEAL_POOL,
        /** The team gets a second dragon in sudden death. */
        DRAGON_BUFF,
        /** The team gets one more wither per wave of the sudden death, per level. */
        WITHER_BUFF,
        /** Bought and remembered, but nothing in the plugin acts on it - for addons. */
        NONE;

        /**
         * @param name what the config says
         * @return that effect, {@link #NONE} when it says something unknown
         */
        public static Effect byName(String name) {
            if (name == null) return NONE;
            for (Effect effect : values()) {
                if (effect.name().equalsIgnoreCase(name)) return effect;
            }
            return NONE;
        }
    }

    /**
     * @param level the level to be reached, starting at one
     * @return what it costs, the last configured price when the list is shorter than the level
     */
    public int priceFor(int level) {
        if (prices.isEmpty()) return 0;
        int index = Math.max(0, Math.min(prices.size() - 1, level - 1));
        return Math.max(0, prices.get(index));
    }
}
