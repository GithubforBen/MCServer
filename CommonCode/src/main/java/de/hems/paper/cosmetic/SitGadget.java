package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Sitting down on stairs and slabs.
 * <p>
 * There is no sitting in the game, so what somebody sits on is a stand nobody can see with them riding
 * it. Which means the thing to get right is not the sitting but the getting up: a seat that outlives its
 * sitter is an invisible block in the middle of somebody's living room.
 * <p>
 * Only with an empty hand, and only on something that looks like a seat. Otherwise every right click on
 * a staircase while carrying a torch would seat its owner instead of placing the torch.
 */
public class SitGadget implements Gadget, Listener {

    /** How far above the block its sitter ends up, in blocks. */
    private static final double SEAT_HEIGHT = 0.3d;

    private final GadgetEntities seats = new GadgetEntities();

    @Override
    public String getId() {
        return Cosmetics.GADGET_SIT;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.SURVIVAL);
    }

    @Override
    public @Nullable String hint() {
        return "Sitzen: Rechtsklick mit leerer Hand auf Treppen und Stufen.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return;
        Block block = event.getClickedBlock();
        if (block == null || !seatable(block)) return;

        Player player = event.getPlayer();
        if (Gadgets.settingsFor(player, getId()) == null) return;
        if (player.isInsideVehicle()) return;
        // something has to stand on the seat, and a block that is already occupied by a wall or a chest
        // would seat somebody inside it
        if (!block.getRelative(org.bukkit.block.BlockFace.UP).isEmpty()) return;
        event.setCancelled(true);

        Location where = block.getLocation().add(0.5d, SEAT_HEIGHT, 0.5d);
        where.setYaw(player.getLocation().getYaw());
        Entity spawned = block.getWorld().spawnEntity(where, EntityType.ARMOR_STAND);
        if (!(spawned instanceof ArmorStand stand)) {
            spawned.remove();
            return;
        }
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        seats.keep(player, stand);
        stand.addPassenger(player);
    }

    /**
     * Somebody standing up leaves the stand behind, and nothing else would ever take it away: the gadget
     * is still on, so the loop that cleans up after a gadget somebody took off never gets to it.
     */
    @EventHandler
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        seats.remove(player);
    }

    @Override
    public void cleanUp(Player player) {
        seats.remove(player);
    }

    /**
     * @param block something clicked
     * @return whether it is the kind of thing people sit on
     */
    private boolean seatable(Block block) {
        return block.getBlockData() instanceof org.bukkit.block.data.type.Stairs
                || block.getBlockData() instanceof org.bukkit.block.data.type.Slab;
    }
}
