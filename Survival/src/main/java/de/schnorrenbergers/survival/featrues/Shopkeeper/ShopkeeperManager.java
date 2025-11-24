package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.utils.Inventorys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopkeeperManager {
    private static List<Shopkeeper> shopkeepers;

    public ShopkeeperManager() {
        if (shopkeepers == null) {
            shopkeepers = new ArrayList<>();
            load();
        }
    }

    public static void save() {
        shopkeepers.forEach(Shopkeeper::save);
    }

    public static Shopkeeper createShopkeeper(Player player, String name) {
        int money = MoneyHandler.getMoney(player.getUniqueId());
        if (money < 20 * 100) {
            player.sendMessage("You dont have enough money! You need 2000!");
            return null;
        }
        Location chest = new Location(player.getWorld(), player.getX(), player.getY() , player.getZ());
        if (chest.getBlock().getType() != Material.CHEST) {
            player.sendMessage("You need to be standing on a chest!");
            return null;
        }
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        if (playerTeam == null) {
            player.sendMessage("You dont have a team!");
            return null;
        }
        if (ClaimManager.getTeamOfChunk(player.getChunk()) == null) {
            player.sendMessage("You need to claim this chunk first!");
            return null;
        }
        if (!ClaimManager.getTeamOfChunk(player.getChunk()).equals(playerTeam.getName())) {
            player.sendMessage("You dont own this chunk!");
            return null;
        }
        Shopkeeper shopkeeper = new Shopkeeper(UUID.randomUUID(),
                name,
                player.getLocation(),
                chest,
                playerTeam.getName(),
                new ArrayList<>());
        shopkeepers.add(shopkeeper);
        return shopkeeper;
    }

    public static void openManagerInventory(Player player, UUID uuid) {
        Shopkeeper shopkeeper = getShopkeeper(uuid);
        if (shopkeeper == null) {
            player.sendMessage("Shopkeeper not found! Report this to the server owner!");
            return;
        }
        player.openInventory(Inventorys.ADMIN_SHOPKEEPER_INVENTORY(
                shopkeeper
        ).getInventory());
    }

    public static Shopkeeper getShopkeeper(UUID uuid) {
        for (Shopkeeper shopkeeper : shopkeepers) {
            if (shopkeeper.getUuid().equals(uuid)) {
                return shopkeeper;
            }
        }
        return null;
    }

    public static void openShopInventory(Player player, UUID uuid) {
        Shopkeeper shopkeeper = getShopkeeper(uuid);
        if (shopkeeper == null) return;
        player.openInventory(shopkeeper.getInventory(1).getInventory());
    }

    private void load() {
        YamlConfiguration config = Survival.getInstance().getShopConfig().getConfig();
        if (config.contains("shopkeepers.ids")) {
            config.getStringList("shopkeepers.ids").forEach(id -> {
                if (config.contains("shopkeepers." + id)) {
                    shopkeepers.add(new Shopkeeper(UUID.fromString(id)));
                }
            });
        }
    }
}
