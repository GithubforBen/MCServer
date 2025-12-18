package de.schnorrenbergers.survival.featrues.flight;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class FlightListener implements Listener {

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!e.hasItem()) return;
        if (!e.getItem().getType().equals(Material.FEATHER)) return;
        if (!e.getItem().getItemMeta().hasDisplayName()) return;
        if (e.getItem().getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Flight")) {
            e.getPlayer().setAllowFlight(true);
            e.getPlayer().setFlying(true);
            e.getPlayer().getInventory().remove(e.getItem());
            e.getPlayer().sendMessage("You are flying!");
        }
    }
}
