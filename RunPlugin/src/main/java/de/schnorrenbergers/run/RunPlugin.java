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
 * one: it watches the bosses, enforces hardcore, reports the time back to the launcher and lets the team
 * call the attempt off with {@code /reset}, so they can start the next one on a fresh server.
 */
public final class RunPlugin extends JavaPlugin {

    private static RunPlugin instance;
    /** The run of this server, or {@code null} when the network could not be reached. */
    private RunTracker tracker;

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
        tracker = new RunTracker(this);
        de.hems.paper.tablist.TabList.init(this, new de.hems.paper.tablist.SimpleTab("Speedrun",
                net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE, false, "/events  ·  /lobby"));
        PluginCommands.register(this, "reset", new ResetCommand(tracker));
    }

    @Override
    public void onDisable() {
        if (tracker != null) tracker.close();
        // while the jar is still open: closing the cluster connection from a jvm shutdown hook is too
        // late, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
    }

    public static RunPlugin getInstance() {
        return instance;
    }
}
