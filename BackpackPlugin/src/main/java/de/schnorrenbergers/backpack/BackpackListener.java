package de.schnorrenbergers.backpack;

import de.hems.paper.PayingPlayers;
import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Keeps the backpacks in step with what players do.
 */
public class BackpackListener implements Listener {

    private final BackpackManager manager;

    public BackpackListener(BackpackPlugin plugin, BackpackManager manager) {
        this.manager = manager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // a player who just started paying should not have to wait a minute for their backpack to grow
        PayingPlayers.invalidate();
        PayingPlayers.refreshIfDue();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        BackpackHolder holder = holderOf(event.getInventory().getHolder());
        if (holder == null || !(event.getPlayer() instanceof Player player)) return;
        manager.onClose(holder.getTeamName(), player);
    }

    /**
     * Stops members from taking things out when their team does not allow it. The leader is never blocked.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        BackpackHolder holder = holderOf(event.getInventory().getHolder());
        if (holder == null || !(event.getWhoClicked() instanceof Player player)) return;
        if (isTakeAllowed(player, holder.getTeamName(), event.getAction())) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "❌ Dein Team erlaubt nur dem Anführer, etwas herauszunehmen.");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        BackpackHolder holder = holderOf(event.getInventory().getHolder());
        if (holder == null || !(event.getWhoClicked() instanceof Player player)) return;
        // dragging only ever puts things in, so it is allowed whenever the backpack itself is
        TeamData team = TeamService.getTeam(holder.getTeamName());
        if (team != null && team.hasMember(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    /**
     * @param player who clicked
     * @param teamName the team the backpack belongs to
     * @param action what the click would do
     * @return whether it may go through
     */
    private static boolean isTakeAllowed(Player player, String teamName, InventoryAction action) {
        TeamData team = TeamService.getTeam(teamName);
        if (team == null) return false;
        if (!team.hasMember(player.getUniqueId())) return false;
        if (team.isLeader(player.getUniqueId())) return true;
        if (team.getSettings().getFlag(TeamSettings.Key.BACKPACK_MEMBERS_MAY_TAKE)) return true;
        return !takesSomethingOut(action);
    }

    /**
     * @param action the action of a click
     * @return whether it would move something out of the backpack
     */
    private static boolean takesSomethingOut(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                 MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                 COLLECT_TO_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT, SWAP_WITH_CURSOR -> true;
            default -> false;
        };
    }

    /**
     * @param holder the holder of the inventory that was interacted with
     * @return it as a backpack holder, or {@code null} if it is not a backpack
     */
    private static BackpackHolder holderOf(InventoryHolder holder) {
        return holder instanceof BackpackHolder backpack ? backpack : null;
    }
}
