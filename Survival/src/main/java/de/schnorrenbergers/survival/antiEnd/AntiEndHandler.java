package de.schnorrenbergers.survival.antiEnd;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

public class AntiEndHandler {

    public static boolean allowEnd() {
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        if (config.contains("allow-end")) {
            return config.getBoolean("allow-end");
        }
        return false;
    }

    public static void setAllowEnd(boolean allow) {
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        config.set("allow-end", allow);
        Survival.getInstance().getMoneyConfig().save();
    }
}
