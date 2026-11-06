package com.benschnorr.survival;

import com.benschnorr.paper.NetworkPlugin;
import com.benschnorr.paper.PluginCommands;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.paper.cosmetic.CosmeticService;
import com.benschnorr.paper.discord.AccountLinkService;
import com.benschnorr.paper.cosmetic.CosmeticEffects;
import com.benschnorr.paper.money.MoneyService;
import com.benschnorr.paper.admin.AdminStash;
import com.benschnorr.paper.event.AwardService;
import com.benschnorr.survival.featrues.money.MoneyHandler;
import com.benschnorr.paper.event.RunService;
import com.benschnorr.paper.team.TeamService;
import com.benschnorr.survival.antiEnd.AntiEndListener;
import com.benschnorr.survival.commands.*;
import com.benschnorr.survival.featrues.Shopkeeper.ShopkeeperManager;
import com.benschnorr.survival.featrues.Shopkeeper.ShopkeeperStore;
import com.benschnorr.survival.featrues.adminabuse.CommandListener;
import com.benschnorr.survival.featrues.adminabuse.LegitimizeCommand;
import com.benschnorr.survival.featrues.chunklimiter.ChunkLimiter;
import com.benschnorr.survival.featrues.team.TeamRules;
import com.benschnorr.survival.featrues.team.TeamSyncListener;
import com.benschnorr.survival.featrues.chunklimiter.ChunkLimiterListener;
import com.benschnorr.survival.featrues.chunklimiter.ChunkLimiterSettings;
import com.benschnorr.survival.featrues.endfight.EndListener;
import com.benschnorr.survival.featrues.flight.FlightListener;
import com.benschnorr.survival.featrues.tablist.SurvivalTab;
import com.benschnorr.survival.listener.ATMListener;
import com.benschnorr.survival.listener.JoinListener;
import com.benschnorr.survival.utils.configs.MoneyConfig;
import com.benschnorr.survival.utils.configs.ShopConfig;
import com.benschnorr.survival.utils.configs.TeamConfig;
import com.benschnorr.survival.utils.events.RequestPlayerMoneyEventHandler;
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
        new com.benschnorr.survival.featrues.cosmetic.GadgetListener(this);
        AccountLinkService.init(this);
        AdminStash.init(this);
        // /admin join: the same stash, carried rather than opened, under a name nobody recognises
        com.benschnorr.survival.featrues.adminjoin.AdminJoinService.init(this);
        TeamService.init(this);
        new TeamSyncListener();
        // claims are only worth having if somebody can see where they are: a title on crossing, the
        // owner over the hotbar while standing on it, and /cteam grenze for the line itself
        new com.benschnorr.survival.featrues.team.ClaimDisplay(this);
        PluginCommands.register(this, "admin", new com.benschnorr.survival.commands.AdminCommand());
        PluginCommands.register(this, "debug", new DebugCommand());
        PluginCommands.register(this, "cteam", new TeamCommand());
        PluginCommands.register(this, "rs", new RestartCommand());
        // /shopkeeper <name> is the old spelling of /shop create <name>, and is the same command
        com.benschnorr.survival.commands.ShopCommand shop = new com.benschnorr.survival.commands.ShopCommand();
        PluginCommands.register(this, "shop", shop);
        PluginCommands.register(this, "shopkeeper", shop);
        PluginCommands.register(this, "banane", new BanCommand());
        PluginCommands.register(this, "legitimize", new LegitimizeCommand());
        PluginCommands.register(this, "verify", new com.benschnorr.paper.commands.VerifyCommand());
        PluginCommands.register(this, "cosmetics", new com.benschnorr.paper.commands.CosmeticsCommand());
        com.benschnorr.paper.tablist.TabList.init(this, new SurvivalTab());
        ShopkeeperManager.init(new ShopkeeperStore(shopConfig, getLogger()),
                new com.benschnorr.survival.featrues.Shopkeeper.ShopSettings(
                        new java.io.File("./configs/shop.yml"), getLogger()));
        new ATMListener();
        chunkLimiter = new ChunkLimiter(new ChunkLimiterSettings());
        chunkLimiter.start();
        new ChunkLimiterListener();
        PluginCommands.register(this, "sichtweite", new ViewDistanceCommand());
        new JoinListener();
        RunService.init(this);
        // the calendar is open here as well, and a poker ranking that is empty everywhere but the lobby
        // looks like a bug rather than like a server that was not asked
        com.benschnorr.paper.poker.PokerStatsService.init(this);
        // this server owns the economy, so it is the one that can pay out the money side of a prize
        AwardService.setMoneyGiver((player, amount) ->
                MoneyHandler.addMoney(amount, player.getUniqueId()));
        new FlightListener();
        new CommandListener();
        new AntiEndListener();
        new EndListener();
        // one LabSettings for the whole lab, so configs/lab.yml is read once
        com.benschnorr.survival.featrues.lab.LabSettings labSettings =
                new com.benschnorr.survival.featrues.lab.LabSettings(
                        new java.io.File("./configs/lab.yml"), getLogger());
        com.benschnorr.survival.featrues.lab.item.LabItemListeners.init(this);
        com.benschnorr.survival.featrues.lab.craft.LabCrafting.init(this);
        com.benschnorr.survival.featrues.lab.brew.LabBrewing.init(this, labSettings);
        com.benschnorr.survival.featrues.lab.consume.LabConsume.init(this, labSettings);
        PluginCommands.register(this, "labor",
                new com.benschnorr.survival.featrues.lab.LabCommand(labSettings));
        PluginCommands.register(this, "signitem",
                new com.benschnorr.survival.featrues.signing.SignItemCommand());
        com.benschnorr.survival.featrues.signing.SignItemCaseListener.init(this);
        com.benschnorr.survival.featrues.signing.guard.SignedCraftGuard.init(this);
        com.benschnorr.survival.featrues.signing.guard.SignedSmeltAnvilGuard.init(this);
        com.benschnorr.survival.featrues.signing.guard.SignedStationGuard.init(this);
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
