package de.schnorrenbergers.survival;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.money.MoneyService;
import de.hems.paper.admin.AdminStash;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.event.AwardService;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.hems.paper.event.EventService;
import de.hems.paper.event.RunService;
import de.hems.paper.team.TeamService;
import de.schnorrenbergers.survival.antiEnd.AntiEndListener;
import de.schnorrenbergers.survival.commands.*;
import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperListener;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.adminabuse.CommandListener;
import de.schnorrenbergers.survival.featrues.adminabuse.LegitimizeCommand;
import de.schnorrenbergers.survival.featrues.chunklimiter.ChunkLimiter;
import de.schnorrenbergers.survival.featrues.team.TeamRules;
import de.schnorrenbergers.survival.featrues.team.TeamSyncListener;
import de.schnorrenbergers.survival.featrues.chunklimiter.ChunkLimiterListener;
import de.schnorrenbergers.survival.featrues.chunklimiter.ChunkLimiterSettings;
import de.schnorrenbergers.survival.featrues.endfight.EndListener;
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
    private ChunkLimiter chunkLimiter;
    private TeamRules teamRules;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        moneyConfig = new MoneyConfig();
        teamConfig = new TeamConfig();
        shopConfig = new ShopConfig();
        teamRules = new TeamRules();
        try {
            listenerAdapter = new ListenerAdapter(ListenerAdapter.ServerName.SURVIVAL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new RequestPlayerMoneyEventHandler();
        // the bits themselves live on the launcher now, this keeps the local copy current
        MoneyService.init(this);
        new PlayerAdminHandler(this);
        AdminStash.init(this);
        TeamService.init(this);
        new TeamSyncListener();
        registerCommand("admin", new de.schnorrenbergers.survival.commands.AdminCommand());
        registerCommand("debug", new DebugCommand());
        registerCommand("cteam", new TeamCommand());
        getCommand("rs").setExecutor(new RestartCommand());
        registerCommand("servermanger", new ServerManagerCommand());
        registerCommand("warp", new de.hems.paper.commands.WarpCommand());
        registerCommand("shopkeeper", new ShopkeeperCommand());
        registerCommand("shop", new de.schnorrenbergers.survival.commands.ShopCommand());
        registerCommand("banane", new BanCommand());
        registerCommand("legitimize", new LegitimizeCommand());
        new Tablist();
        new CustomInventoryListener(this);
        de.hems.paper.warp.ServerConnector.register(this);
        new ShopkeeperManager();
        new ShopkeeperListener();
        new ATMListener();
        chunkLimiter = new ChunkLimiter(new ChunkLimiterSettings());
        chunkLimiter.start();
        new ChunkLimiterListener();
        new JoinListener();
        EventService.init(this);
        RunService.init(this);
        // this server owns the economy, so it is the one that can pay out the money side of a prize
        AwardService.setMoneyGiver((player, amount) ->
                MoneyHandler.addMoney(amount, player.getUniqueId()));
        registerCommand("events", new de.hems.paper.commands.EventCommand());
        new FlightListener();
        new CommandListener();
        new AntiEndListener();
        new EndListener();
    }

    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        getCommand(commandName).setTabCompleter((TabCompleter) command);
    }

    @Override
    public void onDisable() {
        ListenerAdapter.disconnect();
        if (chunkLimiter != null) chunkLimiter.stop();
        if (AdminStash.getInstance() != null) AdminStash.getInstance().saveOnShutdown();
        ShopkeeperManager.shutdown();
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

    public ChunkLimiter getChunkLimiter() {
        return chunkLimiter;
    }

    public TeamRules getTeamRules() {
        return teamRules;
    }
}
