package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class ShopkeeperListener implements Listener {

    private static boolean registered = false;

    public ShopkeeperListener() {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        System.out.println(1);
        Team playerTeam = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());
        if (playerTeam == null) return;
        System.out.println(2);
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) {
            return;
        }
        System.out.println(3);
        String s = event.getRightClicked().getPersistentDataContainer().get(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING);
        if (s == null) return;
        System.out.println(s + 4);
        Shopkeeper shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(s));
        if (shopkeeper == null) return;
        System.out.println(5);
        if (event.getPlayer().isSneaking()) {
            System.out.println(6);
            ShopkeeperManager.openManagerInventory(event.getPlayer(), UUID.fromString(s));
        }
        System.out.println(7);
    }
}
