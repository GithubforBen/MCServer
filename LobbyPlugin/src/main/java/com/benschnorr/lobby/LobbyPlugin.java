package com.benschnorr.lobby;

import com.benschnorr.paper.NetworkPlugin;
import com.benschnorr.paper.PluginCommands;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.paper.commands.CosmeticsCommand;
import com.benschnorr.paper.cosmetic.CosmeticEffects;
import com.benschnorr.paper.cosmetic.CosmeticService;
import com.benschnorr.paper.money.MoneyService;
import com.benschnorr.paper.hologram.Holograms;
import com.benschnorr.paper.event.RunService;
import com.benschnorr.paper.commands.VerifyCommand;
import com.benschnorr.paper.discord.AccountLinkService;
import com.benschnorr.paper.round.RoundService;
import com.benschnorr.lobby.bedwars.BedwarsDebugCommand;
import com.benschnorr.lobby.npc.LobbyNpcs;
import com.benschnorr.lobby.npc.NpcCommand;
import com.benschnorr.lobby.npc.NpcStore;
import com.benschnorr.lobby.parkour.CheckpointListener;
import com.benschnorr.lobby.parkour.ParkourCommand;
import com.benschnorr.lobby.parkour.ParkourService;
import com.benschnorr.lobby.parkour.ParkourStore;
import com.benschnorr.lobby.rounds.RoundCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LobbyPlugin extends JavaPlugin {

    private static LobbyPlugin instance;
    private ListenerAdapter listenerAdapter;
    private ParkourService parkour;
    private LobbyNpcs npcs;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // the hub is nothing without the network - every button in it leads somewhere else
        if (!NetworkPlugin.connect(this, ListenerAdapter.ServerName.LOBBY)) {
            throw new IllegalStateException("The lobby cannot run without the network.");
        }
        LobbyWorld.load(this);
        parkour = new ParkourService(new ParkourStore(new File(getDataFolder(), "parkour.yml"), getLogger()));
        new CheckpointListener(this, parkour);
        PluginCommands.register(this, "parkour", new ParkourCommand(parkour));
        // after the world is loaded, and once: the text hangs in the lobby world and is not persistent,
        // so every start has to put it back up
        parkour.getHolograms().refresh();
        // and the rings around the checkpoints, which are what makes a course visible from the ground
        parkour.getMarkers().start(this);
        RunService.init(this);
        // the hub is where the players are, so it is the hub that puts the server of an event up when its
        // time comes - a bedwars round, a casino, an arena - and brings the people standing here along
        com.benschnorr.paper.event.ServerEventStarter.init(this);
        // the ranking board of a poker night is read here while the hand it describes is still being
        // played on the casino server
        com.benschnorr.paper.poker.PokerStatsService.init(this);
        // rounds players put up themselves
        RoundService.init(this);
        PluginCommands.register(this, "runde", new RoundCommand());
        // who is who on discord: the link is confirmed here and looked up here
        AccountLinkService.init(this);
        PluginCommands.register(this, "verify", new VerifyCommand());
        // the hub is where people stand around, so it is where they put their cosmetics on. The bits are
        // needed with them: the shop shows what somebody can afford before they click
        MoneyService.init(this);
        CosmeticService.init(this);
        CosmeticEffects.init(this);
        // and the gadgets: the effects are the same everywhere, the answer to who may use one here is not
        new com.benschnorr.lobby.cosmetic.GadgetListener(this);
        // none of it on a course: a time only counts if everybody ran it with the same legs
        CosmeticEffects.setOff(player -> parkour.runOf(player) != null);
        PluginCommands.register(this, "cosmetics", new CosmeticsCommand());
        PluginCommands.register(this, "bwdebug", new BedwarsDebugCommand());
        // the lotto stand: the villager and its sign go up here, the slip itself is the same on every server
        com.benschnorr.lobby.lotto.LottoStand stand = new com.benschnorr.lobby.lotto.LottoStand(this);
        PluginCommands.register(this, "lotto", new com.benschnorr.paper.lotto.LottoCommand(java.util.Map.of(
                "stand", stand::place, "standweg", stand::remove)));
        // the people standing around the hub who take you to a server or show you the events
        npcs = new LobbyNpcs(this, new NpcStore(new File(getDataFolder(), "npcs.yml"), getLogger()));
        PluginCommands.register(this, "npc", new NpcCommand(npcs));
        com.benschnorr.paper.tablist.TabList.init(this, new com.benschnorr.lobby.tablist.LobbyTab());
        new LobbyJoinListener();
        new LobbyProtectionListener(this);
    }


    @Override
    public void onDisable() {
        // while the jar is still open: closing the cluster connection from a jvm shutdown hook is too
        // late, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
        if (npcs != null) npcs.despawnAll();
        Holograms.removeAll();
    }

    public static LobbyPlugin getInstance() {
        return instance;
    }

    /**
     * @return the parkour of the lobby: its courses, its runs and its times
     */
    public ParkourService getParkour() {
        return parkour;
    }
}
