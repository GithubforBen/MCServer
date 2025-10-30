package de.schnorrenbergers.survival;

import de.hems.communication.ListenerAdapter;
import de.schnorrenbergers.survival.commands.DebugCommand;
import de.schnorrenbergers.survival.featrues.tablist.Tablist;
import de.schnorrenbergers.survival.utils.configs.MoneyConfig;
import de.schnorrenbergers.survival.utils.configs.TeamConfig;
import de.schnorrenbergers.survival.utils.customInventory.CustomInventoryListener;
import de.schnorrenbergers.survival.utils.events.RequestPlayerMoneyEventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class Survival extends JavaPlugin {
    private static Survival instance;
    private MoneyConfig moneyConfig;
    private ListenerAdapter listenerAdapter;
    private TeamConfig teamConfig;

    @Override
    public void onLoad() {
        instance = this;
        moneyConfig = new MoneyConfig();
        teamConfig = new TeamConfig();
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
        new CustomInventoryListener();
    }

    @Override
    public void onDisable() {
        moneyConfig.save();
        teamConfig.save();
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

    public TeamConfig getTeamConfig() {
        return teamConfig;
    }
}
