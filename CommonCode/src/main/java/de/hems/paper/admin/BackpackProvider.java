package de.hems.paper.admin;

import de.hems.types.admin.PlayerSnapshot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * How the admin website reaches backpacks.
 * <p>
 * The network has no backpack system of its own. Rather than guessing at one, this interface is the seam:
 * whichever plugin ends up owning backpacks registers an implementation with
 * {@link PlayerAdminHandler#setBackpackProvider(BackpackProvider)}, and the player manager can list, read
 * and write them without knowing anything about how they are stored. Until then the website simply reports
 * that no backpacks are available.
 */
public interface BackpackProvider {

    /**
     * @param playerId the player to look at
     * @return the backpacks that player owns, empty if none
     */
    List<PlayerSnapshot.BackpackInfo> listBackpacks(UUID playerId);

    /**
     * @param playerId   the owner
     * @param backpackId which backpack, as reported by {@link #listBackpacks(UUID)}
     * @return its contents, or {@code null} if there is no such backpack
     */
    ItemStack[] readBackpack(UUID playerId, String backpackId);

    /**
     * @param playerId   the owner
     * @param backpackId which backpack
     * @param contents   what to store
     * @return whether it was written
     */
    boolean writeBackpack(UUID playerId, String backpackId, ItemStack[] contents);
}
