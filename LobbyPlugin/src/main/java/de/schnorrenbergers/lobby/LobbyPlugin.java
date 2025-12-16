package de.schnorrenbergers.lobby;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
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
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.LOBBY);
            new CustomInventoryListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }//TODO: parkour
        new CheckpointListener();
    }

    @Override
    public void onEnable() {
        registerCommand("servermanger", new ServerManagerCommand());
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
