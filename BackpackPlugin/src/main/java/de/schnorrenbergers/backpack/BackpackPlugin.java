package de.schnorrenbergers.backpack;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.PayingPlayers;
import de.hems.paper.team.TeamService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A backpack every member of a team shares.
 * <p>
 * The contents live on the launcher next to the teams themselves, so the same backpack is reachable from
 * every server of the network. How big it is depends on the team: once the members who pay for the server
 * are in the majority it grows from a chest to a double chest.
 * <p>
 * The plugin is selectable like any other, so a server can be created with or without it.
 */
public final class BackpackPlugin extends JavaPlugin {

    private static BackpackPlugin instance;
    private BackpackSettings settings;
    private BackpackManager manager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        PaperContext.setPlugin(this);
        if (!ListenerAdapter.isInitialized()) {
            getLogger().warning("The network is not connected yet - backpacks will work once it is.");
        }
        settings = new BackpackSettings(this);
        manager = new BackpackManager(this, settings);
        TeamService.init(this);
        PayingPlayers.refreshNow();

        new BackpackListener(this, manager);
        BackpackCommand command = new BackpackCommand(manager, settings);
        getCommand("backpack").setExecutor(command);
        getCommand("backpack").setTabCompleter(command);
        getLogger().info("Backpacks ready: " + settings.getFreeSize() + " slots, "
                + settings.getPayingSize() + " for teams with a paying majority.");
    }

    @Override
    public void onDisable() {
        // whatever is still open has to reach the launcher before the server goes away
        if (manager != null) manager.saveAllBlocking();
    }

    public static BackpackPlugin getInstance() {
        return instance;
    }

    public BackpackSettings getSettings() {
        return settings;
    }

    public BackpackManager getManager() {
        return manager;
    }
}
