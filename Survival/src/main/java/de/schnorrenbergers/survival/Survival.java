package de.schnorrenbergers.survival;

import de.hems.paper.NetworkPlugin;
import de.hems.paper.PluginCommands;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.cosmetic.CosmeticService;
import de.hems.paper.discord.AccountLinkService;
import de.hems.paper.cosmetic.CosmeticEffects;
import de.hems.paper.money.MoneyService;
import de.hems.paper.admin.AdminStash;
import de.hems.paper.event.AwardService;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.hems.paper.event.RunService;
import de.hems.paper.team.TeamService;
import de.schnorrenbergers.survival.antiEnd.AntiEndListener;
import de.schnorrenbergers.survival.commands.*;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopChestListener;
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
import de.schnorrenbergers.survival.utils.events.RequestPlayerMoneyEventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class Survival extends JavaPlugin {
    private static Survival instance;
    private MoneyConfig moneyConfig;
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
        // survival owns the economy and the teams, and both live on the launcher
        if (!NetworkPlugin.connect(this, ListenerAdapter.ServerName.SURVIVAL)) {
            throw new IllegalStateException("Survival cannot run without the network.");
        }
        new RequestPlayerMoneyEventHandler();
        // the bits themselves live on the launcher now, this keeps the local copy current
        MoneyService.init(this);
        CosmeticService.init(this);
        // registered here as well, so the admin menu can say which effects actually exist
        CosmeticEffects.init(this);
        // the gadgets need one answer more than the other cosmetics: who counts as playing here
        new de.schnorrenbergers.survival.featrues.cosmetic.GadgetListener(this);
        AccountLinkService.init(this);
        AdminStash.init(this);
        // /admin join: the same stash, carried rather than opened, under a name nobody recognises
        de.schnorrenbergers.survival.featrues.adminjoin.AdminJoinService.init(this);
        TeamService.init(this);
        new TeamSyncListener();
        // claims are only worth having if somebody can see where they are: a title on crossing, the
        // owner over the hotbar while standing on it, and /cteam grenze for the line itself
        new de.schnorrenbergers.survival.featrues.team.ClaimDisplay(this);
        PluginCommands.register(this, "admin", new de.schnorrenbergers.survival.commands.AdminCommand());
        PluginCommands.register(this, "debug", new DebugCommand());
        PluginCommands.register(this, "cteam", new TeamCommand());
        PluginCommands.register(this, "rs", new RestartCommand());
        PluginCommands.register(this, "shopkeeper", new ShopkeeperCommand());
        PluginCommands.register(this, "shop", new de.schnorrenbergers.survival.commands.ShopCommand());
        PluginCommands.register(this, "banane", new BanCommand());
        PluginCommands.register(this, "legitimize", new LegitimizeCommand());
        PluginCommands.register(this, "verify", new de.hems.paper.commands.VerifyCommand());
        PluginCommands.register(this, "cosmetics", new de.hems.paper.commands.CosmeticsCommand());
        new Tablist();
        new ShopkeeperManager();
        new ShopkeeperListener();
        new ShopChestListener();
        new ATMListener();
        chunkLimiter = new ChunkLimiter(new ChunkLimiterSettings());
        chunkLimiter.start();
        new ChunkLimiterListener();
        new JoinListener();
        RunService.init(this);
        // the calendar is open here as well, and a poker ranking that is empty everywhere but the lobby
        // looks like a bug rather than like a server that was not asked
        de.hems.paper.poker.PokerStatsService.init(this);
        // this server owns the economy, so it is the one that can pay out the money side of a prize
        AwardService.setMoneyGiver((player, amount) ->
                MoneyHandler.addMoney(amount, player.getUniqueId()));
        new FlightListener();
        new CommandListener();
        new AntiEndListener();
        new EndListener();
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
