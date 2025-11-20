package de.schnorrenbergers.survival.featrues.money;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.UUID;

public class MoneyHandler {

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
}
