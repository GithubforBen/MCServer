package de.schnorrenbergers.run;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.EventService;
import de.hems.paper.event.RunService;
import de.hems.paper.warp.ServerConnector;
import org.bukkit.command.CommandExecutor;
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
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.valueOf(serverName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new CustomInventoryListener(this);
        ServerConnector.register(this);
        EventService.init(this);
        RunService.init(this);
        new RunTracker(this);
        registerCommand("reset", new ResetCommand());
        registerCommand("events", new EventCommand());
    }

    /**
     * A run server is named after the run it hosts, and the launcher passes that name in the directory the
     * server was started in.
     *
     * @return the name this server is known by in the network
     */
    private String serverName() {
        String directory = getServer().getWorldContainer().getAbsoluteFile().getName();
        return directory == null || directory.isBlank() ? "EVENT" : directory;
    }

    private void registerCommand(String commandName, Object command) {
        getCommand(commandName).setExecutor((CommandExecutor) command);
        if (command instanceof TabCompleter completer) getCommand(commandName).setTabCompleter(completer);
    }

    public static RunPlugin getInstance() {
        return instance;
    }
}
