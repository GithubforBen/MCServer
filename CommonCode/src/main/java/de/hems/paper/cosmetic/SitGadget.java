package de.hems.paper.cosmetic;

import de.hems.paper.PaperContext;
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
        Entity seat = seats.of(player);
        Location top = seat == null || !seat.equals(event.getDismounted()) ? null : standingSpot(seat, player);
        seats.remove(player);
        if (top == null) return;
        // a tick later: the dismount under way puts them where the seat was, which is inside the block they
        // sat on - nothing holds a player up from inside a block, and they fell through whatever was below
        PaperContext.sync(() -> {
            if (player.isOnline() && player.getWorld().equals(top.getWorld())) player.teleport(top);
        });
    }

    @Override
    public void cleanUp(Player player) {
        Entity seat = seats.of(player);
        Location top = seat == null ? null : standingSpot(seat, player);
        seats.remove(player);
        // taken off while sitting: the same fall as standing up, so the same way out of the block
        if (top != null && player.isOnline() && player.getWorld().equals(top.getWorld())) player.teleport(top);
    }

    /**
     * @param seat   the stand somebody sits on
     * @param player who is getting up
     * @return on top of the stair or slab the seat is in, looking where they looked
     */
    private static Location standingSpot(Entity seat, Player player) {
        Block block = seat.getLocation().getBlock();
        double top = Math.max(block.getY(), block.getBoundingBox().getMaxY());
        Location at = new Location(block.getWorld(), block.getX() + 0.5d, top, block.getZ() + 0.5d);
        at.setYaw(player.getLocation().getYaw());
        at.setPitch(player.getLocation().getPitch());
        return at;
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
