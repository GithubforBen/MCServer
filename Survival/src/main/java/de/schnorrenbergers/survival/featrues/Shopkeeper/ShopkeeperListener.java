package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.featrues.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
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
        Team playerTeam = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());
        if (playerTeam == null) return;
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) {
            return;
        }
        String s = event.getRightClicked().getPersistentDataContainer().get(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING);
        if (s == null) return;
        Shopkeeper shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(s));
        if (shopkeeper == null) return;
        if (event.getPlayer().isSneaking()) {
            ShopkeeperManager.openManagerInventory(event.getPlayer(), UUID.fromString(s));
        } else {
            ShopkeeperManager.openShopInventory(event.getPlayer(), UUID.fromString(s));
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        String s = event.getPlayer().getPersistentDataContainer().get(
                new NamespacedKey("shopkeeper", "chestlocation"), PersistentDataType.STRING
        );
        Team team = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());
        if (s != null) {//Set shoptkeeper chest
            if (!event.hasBlock()) return;
            if (!event.getClickedBlock().getType().equals(Material.CHEST)) return;
            if (team == null) return;
            if (ClaimManager.getTeamOfChunk(event.getClickedBlock().getChunk()) == null) {
                return;
            }
            if (!ClaimManager.getTeamOfChunk(event.getClickedBlock().getChunk()).equals(team.getName())) {
                event.getPlayer().sendMessage("You need to claim this chunk first!");
                return;
            }
            Shopkeeper shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(s));
            if (shopkeeper == null) return;
            shopkeeper.setChest(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage("Set chest to location: (" + shopkeeper.getChest().getBlockX() + ", " + shopkeeper.getChest().getBlockY() + ", " + shopkeeper.getChest().getBlockZ() + ")");
            event.getPlayer().getPersistentDataContainer().remove(new NamespacedKey("shopkeeper", "chestlocation"));
            return;
        }
    }
}
