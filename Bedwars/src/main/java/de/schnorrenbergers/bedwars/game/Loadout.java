package de.schnorrenbergers.bedwars.game;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a player keeps when they die.
 * <p>
 * A round of bedwars is a shopping list as much as a fight, and dying resets the inventory - so the parts
 * of that list which are supposed to outlive a death cannot be kept in the inventory. They are kept here
 * and handed back out on the way to the respawn: armour and shears as they were, tools one level lower,
 * which is what makes a death cost something without making it start over.
 */
public class Loadout {

    private int armorTier;
    /** Tool chain to the level the player has reached in it. */
    private final Map<String, Integer> tools = new LinkedHashMap<>();
    /** The shop entries whose item simply comes back, by id. */
    private final Set<String> permanent = new LinkedHashSet<>();
    /** Until when traps ignore this player, in ticks of the game loop. */
    private long trapImmuneUntil;

    public int getArmorTier() {
        return armorTier;
    }

    /**
     * @param armorTier the level bought, ignored when it is not better than what they have
     */
    public void setArmorTier(int armorTier) {
        this.armorTier = Math.max(this.armorTier, armorTier);
    }

    /**
     * @param group a tool chain, e.g. {@code pickaxe}
     * @return the level the player is at in it, 0 when they never bought one
     */
    public int getToolTier(String group) {
        return tools.getOrDefault(group, 0);
    }

    /**
     * @param group a tool chain
     * @param tier  the level bought, ignored when it is not better than what they have
     */
    public void setToolTier(String group, int tier) {
        tools.merge(group, tier, Math::max);
    }

    public Map<String, Integer> getTools() {
        return Map.copyOf(tools);
    }

    /**
     * @param itemId an entry the player keeps for good
     */
    public void addPermanent(String itemId) {
        permanent.add(itemId);
    }

    public Set<String> getPermanent() {
        return Set.copyOf(permanent);
    }

    /**
     * Takes one level off every tool chain, which is what a death costs.
     * <p>
     * Never below the first level: a player who is being spawn killed would otherwise end up unable to
     * break a single block for the rest of the round.
     */
    public void onDeath() {
        tools.replaceAll((group, tier) -> Math.max(1, tier - 1));
    }

    /**
     * @param until the tick until which traps do not go off for this player
     */
    public void setTrapImmuneUntil(long until) {
        this.trapImmuneUntil = Math.max(this.trapImmuneUntil, until);
    }

    /**
     * @param tick where the game loop stands
     * @return whether traps currently ignore this player
     */
    public boolean isTrapImmune(long tick) {
        return tick < trapImmuneUntil;
    }
}
