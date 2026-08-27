package de.schnorrenbergers.run;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.ServerIdentity;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.LobbyCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.EventService;
import de.hems.paper.event.RunService;
import de.hems.paper.warp.ServerConnector;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin every run server carries.
 * <p>
 * A run server is created fresh for one attempt at a race, so this is what turns a bare Paper server into
 * one: it watches the bosses, enforces hardcore, reports the time back to the launcher and can wipe itself
 * with {@code /reset} so the same server can host the next attempt.
 */
public final class RunPlugin extends JavaPlugin {

    private static RunPlugin instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        new CustomInventoryListener(this);
        ServerConnector.register(this);
        // the way out has to exist even when nothing else does: a player stuck on an event server with no
        // network and no /warp has no way back to the lobby other than logging off
        registerCommand("warp", new WarpCommand());
        registerCommand("lobby", new LobbyCommand());
        registerCommand("servermanger", new ServerManagerCommand());
        try {
            new ListenerAdapter(ServerIdentity.of(this, "EVENT"));
        } catch (Exception e) {
            getLogger().warning("No network connection (" + e.getMessage()
                    + "). The run is not tracked and the server can not be left through the proxy.");
            return;
        }
        new PlayerAdminHandler(this);
        EventService.init(this);
        RunService.init(this);
        new RunTracker(this);
        registerCommand("reset", new ResetCommand());
        registerCommand("events", new EventCommand());
    }

    private void registerCommand(String commandName, Object command) {
        PluginCommand registered = getCommand(commandName);
        if (registered == null) {
            getLogger().warning("The command /" + commandName + " is not declared in plugin.yml.");
            return;
        }
        registered.setExecutor((CommandExecutor) command);
        if (command instanceof TabCompleter completer) registered.setTabCompleter(completer);
    }

    public static RunPlugin getInstance() {
        return instance;
    }
}
