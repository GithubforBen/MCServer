package de.schnorrenbergers.bedwars;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.ServerIdentity;
import de.hems.event.EventCalendar;
import de.hems.paper.commands.EventCalendarCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.commands.GameSettingsCommand;
import de.schnorrenbergers.bedwars.util.ConfigurationManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bedwars extends JavaPlugin {
    private static Bedwars instance;
    private ConfigurationManager configurationManager;

    @Override
    public void onLoad() {
        instance = this;
        configurationManager = new ConfigurationManager();
    }

    @Override
    public void onEnable() {
        try {
            // bedwars runs on event servers, so the name comes from the launcher
            new ListenerAdapter(ServerIdentity.resolve(ListenerAdapter.ServerName.EVENT));
            new CustomInventoryListener(this);
        } catch (Exception e) {
            getLogger().warning("Could not join the MCServer network: " + e.getMessage());
        }
        ServerConnector.register(this);
        EventCalendar.init();
        getCommand("gameSettings").setExecutor(new GameSettingsCommand());
        getCommand("gameSettings").setTabCompleter(new GameSettingsCommand());
        registerCommand("kalender", new EventCalendarCommand());
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

    public static Bedwars getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
