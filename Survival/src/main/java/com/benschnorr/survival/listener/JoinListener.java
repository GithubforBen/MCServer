package com.benschnorr.survival.listener;

import com.benschnorr.paper.event.AwardService;
import com.benschnorr.paper.event.EventAnnouncer;
import com.benschnorr.survival.Survival;
import jdk.jfr.Label;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

public class JoinListener implements Listener {

    private static boolean registered = false;

    public JoinListener() {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!player.getPersistentDataContainer().has(NamespacedKey.fromString("spawn"))) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.getPersistentDataContainer().set(NamespacedKey.fromString("spawn"), PersistentDataType.STRING, "true");
            System.out.println("Spawn teleported for " + player.getName());
        }
        // a tick later, so the join message does not get lost above the server's own greeting
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            EventAnnouncer.sendJoinMessage(player);
            AwardService.deliverAsync(player);
        }, 20L);
    }

}
