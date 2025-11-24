package de.schnorrenbergers.survival.featrues.money;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.UUID;

public class MoneyHandler {
    public static final Material MONEY_ITEM = Material.DIAMOND;

    public static synchronized void addMoney(int amount, UUID uuid){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(uuid.toString() + ".money")) {
            config.set(uuid + ".money", config.getInt(uuid + ".money") + amount);
        } else {
            config.set(uuid + ".money", amount);
            config.setComments(uuid.toString() + ".money", List.of("This is the money of the player " +
                    UUIDFetcher.findNameByUUID(uuid)));
        }
        Survival.getInstance().getMoneyConfig().save();
    }

    public static synchronized boolean removeMoney(int amount, UUID uuid){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(uuid.toString() + ".money")) {
            if (config.getInt(uuid.toString() + ".money") < amount) return false;
            config.set(uuid.toString() + ".money", config.getInt(uuid + ".money") - amount);
        } else {
            return false;
        }
        Survival.getInstance().getMoneyConfig().save();
        return true;
    }

    public static synchronized int getMoney(UUID uuid){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(uuid.toString() + ".money")) {
            return config.getInt(uuid.toString() + ".money");
        }
        return 0;
    }
    public static synchronized void addMoney(int amount, Team team){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(team.getName() + ".money")) {
            config.set(team.getName() + ".money", config.getInt(team.getName() + ".money") + amount);
        } else {
            config.set(team.getName() + ".money", amount);
            config.setComments(team.getName() + ".money", List.of("This is the money of the team " + team.getName()));
        }
        Survival.getInstance().getMoneyConfig().save();
    }

    public static synchronized boolean removeMoney(int amount, Team team){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(team.getName() + ".money")) {
            if (config.getInt(team.getName() + ".money") < amount) return false;
            config.set(team.getName() + ".money", config.getInt(team.getName() + ".money") - amount);
        } else {
            return false;
        }
        Survival.getInstance().getMoneyConfig().save();
        return true;
    }

    public static synchronized int getMoney(Team team){
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains(team.getName() + ".money")) {
            return config.getInt(team.getName() + ".money");
        }
        return 0;
    }
}
