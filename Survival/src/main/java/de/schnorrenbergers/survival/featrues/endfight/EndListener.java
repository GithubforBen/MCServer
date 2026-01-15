package de.schnorrenbergers.survival.featrues.endfight;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class EndListener implements Listener {

    public EndListener() {
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            Random r = new Random();
            if (3 < r.nextInt(10) ||e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                e.setCancelled(true);
                Entity causingEntity = e.getDamageSource().getCausingEntity();
                if (causingEntity instanceof Player) {
                    ((Player) causingEntity).sendMessage("Ender dragon's shell absorbed the damage!");
                }
            }
        }
    }
}
