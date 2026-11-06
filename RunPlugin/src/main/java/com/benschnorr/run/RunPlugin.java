package com.benschnorr.run;

import com.benschnorr.paper.NetworkPlugin;
import com.benschnorr.paper.PluginCommands;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.paper.event.RunService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin every run server carries.
 * <p>
 * A run server is created fresh for one attempt at a race, so this is what turns a bare Paper server into
 * one: it watches the bosses, enforces hardcore, reports the time back to the launcher and lets the team
 * start over with {@code /reset}, which ends the attempt and puts them straight onto the next one.
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
        com.benschnorr.paper.tablist.TabList.init(this, new com.benschnorr.paper.tablist.SimpleTab("Speedrun",
                net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE, false, "/events  ·  /lobby"));
        ResetCommand reset = new ResetCommand(tracker);
        PluginCommands.register(this, "reset", reset);
        PluginCommands.register(this, ResetCommand.ABORT_COMMAND, reset);
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
