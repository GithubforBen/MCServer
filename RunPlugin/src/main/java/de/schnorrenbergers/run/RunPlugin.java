package de.schnorrenbergers.run;

import de.hems.paper.PluginCommands;
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
        PluginCommands.register(this, "warp", new WarpCommand());
        PluginCommands.register(this, "lobby", new LobbyCommand());
        PluginCommands.register(this, "servermanger", new ServerManagerCommand());
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
        PluginCommands.register(this, "reset", new ResetCommand());
        PluginCommands.register(this, "events", new EventCommand());
    }

    @Override
    public void onDisable() {
        // while the jar is still open: closing the cluster connection from a jvm shutdown hook is too
        // late, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
    }

    public static RunPlugin getInstance() {
        return instance;
    }
}
