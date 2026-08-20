package de.schnorrenbergers.bedwars.shop.trap;

import org.bukkit.Material;

import java.util.List;

/**
 * One trap a team can put into its queue, out of {@code upgrades.yml}.
 * <p>
 * A trap is bought long before it does anything: it waits in the queue until an enemy walks into the base,
 * goes off once and is gone. That is why a trap has no level - what a team stacks is the queue, not the
 * trap.
 *
 * @param id          how it is referred to
 * @param displayName what it is called, MiniMessage
 * @param icon        the item it is drawn as
 * @param effect      what going off does
 * @param seconds     how long that lasts
 * @param amplifier   how strong it is, 0 being level one
 * @param lore        what the menu says about it, MiniMessage
 */
public record Trap(String id, String displayName, Material icon, Trap.Effect effect, int seconds,
                   int amplifier, List<String> lore) {

    /**
     * What a trap does to whoever set it off, or to the team that owns it.
     */
    public enum Effect {

        /** The intruder is blinded and slowed. */
        BLINDNESS,
        /** The intruder cannot mine. */
        MINING_FATIGUE,
        /** The team at home gets speed and jump boost to defend with. */
        COUNTER_OFFENSIVE,
        /** The intruder is made visible again and named to the team. */
        ALARM,
        /** Nothing but the announcement - for addons to hang something onto. */
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
}
