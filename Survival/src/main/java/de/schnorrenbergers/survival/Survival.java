package de.schnorrenbergers.survival;

import de.hems.communication.ListenerAdapter;
import de.schnorrenbergers.survival.commands.DebugCommand;
import de.schnorrenbergers.survival.featrues.tablist.Tablist;
import de.schnorrenbergers.survival.utils.configs.MoneyConfig;
import de.schnorrenbergers.survival.utils.events.RequestPlayerMoneyEventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class Survival extends JavaPlugin {
    private static Survival instance;
    private MoneyConfig moneyConfig;
    private ListenerAdapter listenerAdapter;

    @Override
    public void onLoad() {
        instance = this;
        moneyConfig = new MoneyConfig();
    }

    @Override
    public void onEnable() {
        try {
            listenerAdapter = new ListenerAdapter("Survival");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new RequestPlayerMoneyEventHandler();
        getCommand("admin").setExecutor(new de.schnorrenbergers.survival.commands.AdminCommand());
        getCommand("admin").setTabCompleter(new de.schnorrenbergers.survival.commands.AdminCommand());
        getCommand("debug").setExecutor(new DebugCommand());
        getCommand("debug").setTabCompleter(new DebugCommand());
        new Tablist();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Survival getInstance() {
        return instance;
    }

    public MoneyConfig getMoneyConfig() {
        return moneyConfig;
    }

    public ListenerAdapter getListenerAdapter() {
        return listenerAdapter;
    }
}
