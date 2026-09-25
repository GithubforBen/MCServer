package de.schnorrenbergers.run;

import de.hems.paper.NetworkPlugin;
import de.hems.paper.PluginCommands;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.event.RunService;
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
        // the way out has to exist even when nothing else does, which connect sets up first
        if (!NetworkPlugin.connect(this, "EVENT")) {
            getLogger().warning("The run is not tracked and the server can not be left through the proxy.");
            return;
        }
        RunService.init(this);
        new RunTracker(this);
        PluginCommands.register(this, "reset", new ResetCommand());
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
