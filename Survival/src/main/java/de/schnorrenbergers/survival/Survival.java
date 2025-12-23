package de.schnorrenbergers.survival;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.commands.ServerManagerCommand;
import de.schnorrenbergers.survival.commands.*;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperListener;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.adminabuse.CommandListener;
import de.schnorrenbergers.survival.featrues.chunklimiter.PlayerLoadChunkListener;
import de.schnorrenbergers.survival.featrues.flight.FlightListener;
import de.schnorrenbergers.survival.featrues.tablist.Tablist;
import de.schnorrenbergers.survival.listener.ATMListener;
import de.schnorrenbergers.survival.listener.JoinListener;
import de.schnorrenbergers.survival.utils.configs.MoneyConfig;
import de.schnorrenbergers.survival.utils.configs.ShopConfig;
import de.schnorrenbergers.survival.utils.configs.TeamConfig;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.schnorrenbergers.survival.utils.events.RequestPlayerMoneyEventHandler;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Survival extends JavaPlugin {
    private static Survival instance;
    private MoneyConfig moneyConfig;
    private ListenerAdapter listenerAdapter;
    private TeamConfig teamConfig;
    private ShopConfig shopConfig;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        moneyConfig = new MoneyConfig();
        teamConfig = new TeamConfig();
        shopConfig = new ShopConfig();
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
        registerCommand("shopkeeper", new ShopkeeperCommand());
        registerCommand("banane", new BanCommand());
        new Tablist();
        new CustomInventoryListener(this);
        new ShopkeeperManager();
        new ShopkeeperListener();
        new ATMListener();
        new PlayerLoadChunkListener();
        new JoinListener();
        new FlightListener();
        new CommandListener();
    }

    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        getCommand(commandName).setTabCompleter((TabCompleter) command);
    }

    @Override
    public void onDisable() {
        ShopkeeperManager.save();
        moneyConfig.save();
        teamConfig.save();
        shopConfig.save();
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

    public ShopConfig getShopConfig() {
        return shopConfig;
    }
}
