package de.schnorrenbergers.survival.listener;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ATMListener implements Listener {

    private static boolean registered = false;

    public ATMListener() {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if (!event.hasBlock()) return;
        if (!event.getPlayer().isSneaking()) return;
        if (!event.getClickedBlock().getType().equals(Material.ENDER_CHEST)) return;
    }
}
