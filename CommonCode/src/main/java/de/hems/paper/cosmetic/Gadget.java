package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Something its owner carries into a round.
 * <p>
 * Unlike the other three kinds a gadget is not a picture: it is an item and a rule, and both of them are
 * in the game while it is being played. That is why every one of them is a {@link org.bukkit.event.Listener}
 * of its own that checks for itself whether the player in front of it is wearing it - the alternative is a
 * central switch statement that every new gadget has to be added to in three places.
 * <p>
 * Which servers it belongs on is the gadget's own answer, not an admin's: a harvest helper on a bedwars
 * map has nothing to harvest, and the shop has to be able to say where something works before somebody
 * pays for it.
 */
public interface Gadget {

    /**
     * @return the id it is stored under, the same one the catalogue uses
     */
    String getId();

    /**
     * @return the slots it can be worn in; never empty, or the gadget can never be used
     */
    Set<GadgetSlot> slots();

    /**
     * @param cosmetic the gadget as the launcher has it, for its settings
     * @return the item its owner is handed at the start of a round, or {@code null} for a gadget that
     *         hands out nothing - the passive ones work without anything in the hotbar
     */
    default @Nullable ItemStack item(CosmeticData cosmetic) {
        return null;
    }

    /**
     * @return one line telling its owner what it does, said once a round, or {@code null} to say nothing
     */
    default @Nullable String hint() {
        return null;
    }

    /**
     * Takes back whatever the gadget left in the world for somebody.
     * <p>
     * Called when its wearer leaves, when they take it off, and when they walk out of the place it works
     * in. Everything a gadget spawns has to come back here, because the alternative is a lobby slowly
     * filling up with pets whose owners logged out three restarts ago.
     *
     * @param player who
     */
    default void cleanUp(Player player) {
    }
}
