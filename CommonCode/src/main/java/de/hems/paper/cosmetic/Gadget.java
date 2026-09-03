package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Something its owner carries into a round.
 * <p>
 * Unlike the other three kinds a gadget is not a picture: it is an item and a rule, and both of them are
 * in the game while it is being played. That is why every one of them is a {@link org.bukkit.event.Listener}
 * of its own that checks for itself whether the player in front of it is wearing it - the alternative is a
 * central switch statement that every new gadget has to be added to in three places.
 */
public interface Gadget {

    /**
     * @return the id it is stored under, the same one the catalogue uses
     */
    String getId();

    /**
     * @param cosmetic the gadget as the launcher has it, for its settings
     * @return the item its owner is handed at the start of a round, or {@code null} for a gadget that
     *         hands out nothing
     */
    @Nullable ItemStack item(CosmeticData cosmetic);

    /**
     * @return one line telling its owner what it does, said once a round, or {@code null} to say nothing
     */
    default @Nullable String hint() {
        return null;
    }
}
