package de.schnorrenbergers.lobby;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.BedwarsEventStarter;
import de.hems.paper.event.EventService;
import de.hems.paper.hologram.Holograms;
import de.hems.paper.event.RunService;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.lobby.bedwars.BedwarsDebugCommand;
import de.schnorrenbergers.lobby.parkour.CheckpointListener;
import de.schnorrenbergers.lobby.parkour.ParkourCommand;
import de.schnorrenbergers.lobby.parkour.ParkourService;
import de.schnorrenbergers.lobby.parkour.ParkourStore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
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
        }//TODO: parkour
        LobbyWorld.load(this);
        ServerConnector.register(this);
        new PlayerAdminHandler(this);
        parkour = new ParkourService(new ParkourStore(new File(getDataFolder(), "parkour.yml"), getLogger()));
        new CheckpointListener(this, parkour);
        registerCommand("parkour", new ParkourCommand(parkour));
        // after the world is loaded, and once: the text hangs in the lobby world and is not persistent,
        // so every start has to put it back up
        parkour.getHolograms().refresh();
        registerCommand("servermanger", new ServerManagerCommand());
        registerCommand("warp", new WarpCommand());
        EventService.init(this);
        RunService.init(this);
        // the hub is where the players are, so it is the hub that puts a bedwars round up when a bedwars
        // event's time comes and takes everybody standing here along
        BedwarsEventStarter.init(this);
        registerCommand("events", new EventCommand());
        registerCommand("bwdebug", new BedwarsDebugCommand());
        new LobbyJoinListener();
        new LobbyProtectionListener(this);
    }


    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        getCommand(commandName).setTabCompleter((TabCompleter) command);
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
