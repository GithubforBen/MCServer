package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.schnorrenbergers.survival.utils.Inventorys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

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

    public static Shopkeeper createShopkeeper(Player player) {
        int money = MoneyHandler.getMoney(player.getUniqueId());
        if (money < 20 * 100) {
            player.sendMessage("You dont have enough money! You need 2000!");
            return null;
        }
        Location chest = new Location(player.getWorld(), player.getX(), player.getY() - 1, player.getZ());
        player.getWorld().setType(
                chest,
                Material.CHEST
        );
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        if (playerTeam == null) {
            player.sendMessage("You dont have a team!");
            return null;
        }
        Shopkeeper shopkeeper = new Shopkeeper(UUID.randomUUID(),
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
