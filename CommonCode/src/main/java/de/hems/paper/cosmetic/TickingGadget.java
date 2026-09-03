package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import org.bukkit.entity.Player;

/**
 * A gadget that does something on its own, without anybody clicking.
 * <p>
 * The pets, the balloon, the footsteps: they exist while their owner walks around rather than at the
 * moment a button is pressed. {@link Gadgets} runs one loop for all of them - one repeating task rather
 * than one per gadget, because a server with sixteen gadgets would otherwise have sixteen tasks running
 * for a lobby in which nobody wears any of them.
 */
public interface TickingGadget extends Gadget {

    /**
     * Runs one step for a wearer. On the main thread, a few times a second, so it has to be short.
     *
     * @param player   somebody wearing it who may use it right now
     * @param cosmetic the gadget as the launcher has it, for its settings
     */
    void tick(Player player, CosmeticData cosmetic);
}
