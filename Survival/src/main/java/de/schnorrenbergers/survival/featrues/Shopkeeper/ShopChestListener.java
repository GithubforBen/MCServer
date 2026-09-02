package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The stock chest of a shop belongs to the shop.
 * <p>
 * A shop sells what is in its chest, so the chest is the shop's till and its warehouse at once - and up to
 * now anybody who could walk to it could open it, mine it, blow it up or run a hopper into it. A shop that
 * can be emptied by whoever finds it is not a shop, and the villager standing next to it was the only part
 * of the whole thing that was ever protected.
 * <p>
 * Ownership is the shop's team, the same team that may edit the shop. Nothing else is special cased: the
 * chest is either a shop's or it is a chest.
 */
public class ShopChestListener implements Listener {

    /** The four sides a second half of a double chest can be on. */
    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private static boolean registered = false;

    public ShopChestListener() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    /**
     * Opening one, and the first tick of mining one.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Shopkeeper shop = shopAt(event.getClickedBlock());
        if (shop == null || mayTouch(event.getPlayer(), shop)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("Diese Kiste gehört dem Shop \"" + shop.getName() + "\".");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Shopkeeper shop = shopAt(event.getBlock());
        if (shop == null || mayTouch(event.getPlayer(), shop)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("Diese Kiste gehört dem Shop \"" + shop.getName() + "\".");
    }

    /**
     * No blast takes one either. A chest that survives being clicked but not being stood next to with tnt
     * is a chest that is not protected at all.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> shopAt(block) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> shopAt(block) != null);
    }

    /**
     * And nothing is piped out of one.
     * <p>
     * A hopper under a shop chest is the quiet version of emptying it by hand, and the one that keeps
     * working while its owner is offline. Moving stock in is left alone: filling somebody else's shop is
     * not a way to rob it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        if (shopInventory(event.getSource()) == null) return;
        event.setCancelled(true);
    }

    /**
     * @param player who is touching it
     * @param shop   whose chest it is
     * @return whether they are on the team that owns the shop
     */
    private static boolean mayTouch(Player player, Shopkeeper shop) {
        if (player.getGameMode() == GameMode.CREATIVE && player.isOp()) return true;
        Team team = player.getScoreboard().getPlayerTeam(player);
        return team != null && team.getName().equals(shop.getOwnerTeam());
    }

    /**
     * @param block a block, possibly none
     * @return the shop whose chest it is, counting both halves of a double chest, or {@code null}
     */
    private static @Nullable Shopkeeper shopAt(@Nullable Block block) {
        if (block == null) return null;
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
        Shopkeeper direct = ShopkeeperManager.withChestAt(block.getLocation());
        if (direct != null) return direct;
        // the other half: a shop's chest is stored as one block, and a double chest shares its contents
        // with a neighbour that would otherwise be a hole straight into the stock
        for (BlockFace side : SIDES) {
            Block other = block.getRelative(side);
            if (other.getType() != block.getType()) continue;
            Shopkeeper shop = ShopkeeperManager.withChestAt(other.getLocation());
            if (shop != null) return shop;
        }
        return null;
    }

    /**
     * @param inventory the inventory something is being taken out of
     * @return the shop it belongs to, or {@code null} when it belongs to none
     * <p>
     * Asked of the holder rather than of {@code getLocation()}. A double chest reports its location as the
     * point halfway between its two halves, which is not a block at all - it only lands back on one of them
     * because of how the halves round, and a protection that rests on rounding is one that will one day
     * hand somebody the whole stock.
     */
    private static @Nullable Shopkeeper shopInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            Shopkeeper left = shopOfHolder(doubleChest.getLeftSide());
            return left != null ? left : shopOfHolder(doubleChest.getRightSide());
        }
        Shopkeeper direct = shopOfHolder(holder);
        if (direct != null) return direct;
        Location at = inventory.getLocation();
        return at == null ? null : shopAt(at.getBlock());
    }

    /**
     * @param holder whatever owns an inventory
     * @return the shop it is the chest of, or {@code null} when it is not a block at all
     */
    private static @Nullable Shopkeeper shopOfHolder(@Nullable InventoryHolder holder) {
        return holder instanceof BlockState state ? shopAt(state.getBlock()) : null;
    }
}
