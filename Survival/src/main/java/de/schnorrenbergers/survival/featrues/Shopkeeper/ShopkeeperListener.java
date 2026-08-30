package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.featrues.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class ShopkeeperListener implements Listener {

    /** The key a player carries while they are picking the chest of a shop. */
    public static final NamespacedKey CHEST_PICK = new NamespacedKey("shopkeeper", "chestlocation");
    /** The key a player carries while they are picking where a shop should stand. */
    public static final NamespacedKey SHOP_PICK = new NamespacedKey("shopkeeper", "shoplocation");

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
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) {
            return;
        }
        String s = event.getRightClicked().getPersistentDataContainer().get(new NamespacedKey("shopkeeper", "shopid"), PersistentDataType.STRING);
        if (s == null) return;
        Shopkeeper shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(s));
        if (shopkeeper == null) return;
        if (event.getPlayer().isSneaking()) {
            Team playerTeam = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());
            if (playerTeam == null) return;
            if (!shopkeeper.getOwnerTeam().equals(playerTeam.getName())) {
                event.getPlayer().sendMessage("You don't own this shop!");
                return;
            }
            ShopkeeperManager.openManagerInventory(event.getPlayer(), UUID.fromString(s));
        } else {
            ShopkeeperManager.openShopInventory(event.getPlayer(), UUID.fromString(s));
        }
    }

    /**
     * The second half of "change the chest" and "move the shop": the player was told to click a block, and
     * this is that click.
     */
    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String chestPick = player.getPersistentDataContainer().get(CHEST_PICK, PersistentDataType.STRING);
        if (chestPick != null) {
            pickChest(event, chestPick);
            return;
        }
        String shopPick = player.getPersistentDataContainer().get(SHOP_PICK, PersistentDataType.STRING);
        if (shopPick != null) {
            pickShopLocation(event, shopPick);
        }
    }

    private void pickChest(PlayerInteractEvent event, String id) {
        if (!event.hasBlock()) return;
        if (!event.getClickedBlock().getType().equals(Material.CHEST)) return;
        Shopkeeper shopkeeper = shopOf(event.getPlayer(), id);
        if (shopkeeper == null) return;
        if (!ownsChunk(event.getPlayer(), event.getClickedBlock().getChunk())) return;
        event.setCancelled(true);
        shopkeeper.setChest(event.getClickedBlock().getLocation());
        // binding the chest is a real change - it must not wait for the next autosave
        ShopkeeperManager.saveAll();
        Location chest = shopkeeper.getChest();
        event.getPlayer().sendMessage("Kiste gesetzt: (" + chest.getBlockX() + ", " + chest.getBlockY()
                + ", " + chest.getBlockZ() + ")");
        event.getPlayer().getPersistentDataContainer().remove(CHEST_PICK);
    }

    /**
     * Puts the shop on top of the block that was clicked.
     * <p>
     * On top, not in it: standing the villager inside the block would push it out again the moment the
     * chunk ticks, and the shop would drift away from where its owner put it.
     */
    private void pickShopLocation(PlayerInteractEvent event, String id) {
        if (!event.hasBlock()) return;
        Shopkeeper shopkeeper = shopOf(event.getPlayer(), id);
        if (shopkeeper == null) return;
        Block block = event.getClickedBlock();
        if (!ownsChunk(event.getPlayer(), block.getChunk())) return;
        event.setCancelled(true);
        Location target = block.getLocation().add(0.5, 1, 0.5);
        // facing the way the owner is looking, so a shop can be turned towards the customers
        target.setYaw(event.getPlayer().getLocation().getYaw());
        shopkeeper.moveTo(target);
        ShopkeeperManager.saveAll();
        event.getPlayer().sendMessage("Shop verschoben: (" + target.getBlockX() + ", " + target.getBlockY()
                + ", " + target.getBlockZ() + ")");
        event.getPlayer().getPersistentDataContainer().remove(SHOP_PICK);
    }

    /**
     * @param player the player that is picking
     * @param id     the shop they are picking for
     * @return that shop, or {@code null} when it is gone or not theirs
     */
    private Shopkeeper shopOf(Player player, String id) {
        Shopkeeper shopkeeper;
        try {
            shopkeeper = ShopkeeperManager.getShopkeeper(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            player.getPersistentDataContainer().remove(CHEST_PICK);
            player.getPersistentDataContainer().remove(SHOP_PICK);
            return null;
        }
        if (shopkeeper == null) {
            player.sendMessage("Diesen Shop gibt es nicht mehr.");
            player.getPersistentDataContainer().remove(CHEST_PICK);
            player.getPersistentDataContainer().remove(SHOP_PICK);
            return null;
        }
        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team == null || !shopkeeper.getOwnerTeam().equals(team.getName())) {
            player.sendMessage("Dieser Shop gehört dir nicht.");
            return null;
        }
        return shopkeeper;
    }

    /**
     * @param player the player
     * @param chunk  the chunk they clicked in
     * @return whether their team has claimed it, so a shop cannot be parked on somebody else's land
     */
    private boolean ownsChunk(Player player, Chunk chunk) {
        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage("Du brauchst dafür ein Team.");
            return false;
        }
        String owner = ClaimManager.getTeamOfChunk(chunk);
        if (owner == null || !owner.equals(team.getName())) {
            player.sendMessage("Dieser Chunk gehört deinem Team nicht.");
            return false;
        }
        return true;
    }
}
