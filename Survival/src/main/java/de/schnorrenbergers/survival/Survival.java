package de.schnorrenbergers.survival;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.commands.ServerManagerCommand;
import de.schnorrenbergers.survival.commands.DebugCommand;
import de.schnorrenbergers.survival.commands.RestartCommand;
import de.schnorrenbergers.survival.commands.TeamCommand;
import de.schnorrenbergers.survival.featrues.tablist.Tablist;
import de.schnorrenbergers.survival.utils.configs.MoneyConfig;
import de.schnorrenbergers.survival.utils.configs.TeamConfig;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.schnorrenbergers.survival.utils.events.RequestPlayerMoneyEventHandler;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
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
            listenerAdapter = new ListenerAdapter(ListenerAdapter.ServerName.SURVIVAL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new RequestPlayerMoneyEventHandler();
        registerCommand("admin", new de.schnorrenbergers.survival.commands.AdminCommand());
        registerCommand("debug", new DebugCommand());
        registerCommand("cteam", new TeamCommand());
        getCommand("rs").setExecutor(new RestartCommand());
        registerCommand("servermanger", new ServerManagerCommand());
        new Tablist();
        new CustomInventoryListener(this);
    }

    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        getCommand(commandName).setTabCompleter((TabCompleter) command);
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
