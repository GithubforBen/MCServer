package de.schnorrenbergers.survival.listener;

import de.schnorrenbergers.survival.Survival;
import jdk.jfr.Label;
import net.md_5.bungee.api.ChatColor;
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
        e.setJoinMessage(ChatColor.GREEN +">>" + player.getName());
        if (!player.getPersistentDataContainer().has(NamespacedKey.fromString("spawn"))) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.getPersistentDataContainer().set(NamespacedKey.fromString("spawn"), PersistentDataType.STRING, "true");
            System.out.println("Spawn teleported for " + player.getName());
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        e.setQuitMessage(ChatColor.RED + "<<"+ e.getPlayer().getName());
    }
}
