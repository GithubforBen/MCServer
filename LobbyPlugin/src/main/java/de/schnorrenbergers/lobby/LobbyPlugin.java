package de.schnorrenbergers.lobby;

import de.hems.paper.PluginCommands;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.CosmeticsCommand;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.cosmetic.CosmeticEffects;
import de.hems.paper.cosmetic.CosmeticService;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.money.MoneyService;
import de.hems.paper.event.EventService;
import de.hems.paper.hologram.Holograms;
import de.hems.paper.event.RunService;
import de.hems.paper.admin.NetworkOps;
import de.hems.paper.commands.VerifyCommand;
import de.hems.paper.discord.AccountLinkService;
import de.hems.paper.round.RoundService;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.lobby.bedwars.BedwarsDebugCommand;
import de.schnorrenbergers.lobby.parkour.CheckpointListener;
import de.schnorrenbergers.lobby.parkour.ParkourCommand;
import de.schnorrenbergers.lobby.parkour.ParkourService;
import de.schnorrenbergers.lobby.parkour.ParkourStore;
import de.schnorrenbergers.lobby.rounds.RoundCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LobbyPlugin extends JavaPlugin {

    private static LobbyPlugin instance;
    private ListenerAdapter listenerAdapter;
    private ParkourService parkour;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.LOBBY);
            new CustomInventoryListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LobbyWorld.load(this);
        ServerConnector.register(this);
        new PlayerAdminHandler(this);
        parkour = new ParkourService(new ParkourStore(new File(getDataFolder(), "parkour.yml"), getLogger()));
        new CheckpointListener(this, parkour);
        PluginCommands.register(this, "parkour", new ParkourCommand(parkour));
        // after the world is loaded, and once: the text hangs in the lobby world and is not persistent,
        // so every start has to put it back up
        parkour.getHolograms().refresh();
        // and the rings around the checkpoints, which are what makes a course visible from the ground
        parkour.getMarkers().start(this);
        PluginCommands.register(this, "servermanger", new ServerManagerCommand());
        PluginCommands.register(this, "warp", new WarpCommand());
        EventService.init(this);
        RunService.init(this);
        // the hub is where the players are, so it is the hub that puts the server of an event up when its
        // time comes - a bedwars round, a casino, an arena - and brings the people standing here along
        de.hems.paper.event.ServerEventStarter.init(this);
        // the ranking board of a poker night is read here while the hand it describes is still being
        // played on the casino server
        de.hems.paper.poker.PokerStatsService.init(this);
        PluginCommands.register(this, "events", new EventCommand());
        // rounds players put up themselves
        RoundService.init(this);
        PluginCommands.register(this, "runde", new RoundCommand());
        // who is who on discord: the link is confirmed here and looked up here
        AccountLinkService.init(this);
        NetworkOps.init(this);
        PluginCommands.register(this, "verify", new VerifyCommand());
        // the hub is where people stand around, so it is where they put their cosmetics on. The bits are
        // needed with them: the shop shows what somebody can afford before they click
        MoneyService.init(this);
        CosmeticService.init(this);
        CosmeticEffects.init(this);
        // and the gadgets: the effects are the same everywhere, the answer to who may use one here is not
        new de.schnorrenbergers.lobby.cosmetic.GadgetListener(this);
        PluginCommands.register(this, "cosmetics", new CosmeticsCommand());
        PluginCommands.register(this, "bwdebug", new BedwarsDebugCommand());
        new LobbyJoinListener();
        new LobbyProtectionListener(this);
    }


    @Override
    public void onDisable() {
        // while the jar is still open: closing the cluster connection from a jvm shutdown hook is too
        // late, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
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
