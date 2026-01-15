package de.schnorrenbergers.survival.antiEnd;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class AntiEndListener implements Listener {
    public AntiEndListener() {
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
    }

    @EventHandler
    public void onEnd(PlayerPortalEvent e) {
        if (e.getTo().getWorld().getName().equals("world_the_end")) {
            if (!AntiEndHandler.allowEnd()) {
                e.setCancelled(true);
            }
        }
    }
}
