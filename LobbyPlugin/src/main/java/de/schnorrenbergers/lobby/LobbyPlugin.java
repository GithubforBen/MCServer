package de.schnorrenbergers.lobby;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.lobby.parkour.CheckpointListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyPlugin extends JavaPlugin {

    private static LobbyPlugin instance;
    private ListenerAdapter listenerAdapter;

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
        ServerConnector.register(this);
        new PlayerAdminHandler(this);
        new CheckpointListener();
        registerCommand("servermanger", new ServerManagerCommand());
        registerCommand("warp", new WarpCommand());
    }


    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        getCommand(commandName).setTabCompleter((TabCompleter) command);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static LobbyPlugin getInstance() {
        return instance;
    }
}
