package de.schnorrenbergers.backpack;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.PayingPlayers;
import de.hems.paper.ServerIdentity;
import de.hems.paper.team.TeamService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A backpack every member of a team shares.
 * <p>
 * The contents live on the launcher next to the teams themselves, so the same backpack is reachable from
 * every server of the network. How big it is depends on the team: once the members who pay for the server
 * are in the majority it grows from a chest to a double chest.
 * <p>
 * The plugin is selectable like any other, so a server can be created with or without it - and it joins the
 * network on its own. That is not optional: every plugin carries its own copy of the common code inside its
 * jar, so the connection another plugin on the same server opened is one this plugin cannot see. Without a
 * connection of its own the teams never arrive, and every {@code /backpack} answers "die Teams sind noch
 * nicht geladen" for as long as the server runs.
 */
public final class BackpackPlugin extends JavaPlugin {

    /** The name to run under when the directory the server was started in does not give one. */
    private static final String FALLBACK_SERVER = "SURVIVAL";

    private static BackpackPlugin instance;
    private BackpackSettings settings;
    private BackpackManager manager;
    private boolean connected;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        PaperContext.setPlugin(this);
        connect();
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
        // whatever is still open has to reach the launcher before the server goes away, so the backpacks
        // are written back while the connection is still there and the channel is closed afterwards
        if (manager != null) manager.saveAllBlocking();
        if (connected) ListenerAdapter.disconnect();
    }

    /**
     * Joins the network, under the name of the server this plugin is running on.
     * <p>
     * A server that is already in the cluster through another plugin ends up in it twice, which costs one
     * more member and nothing else: events are broadcast, and every connection answers only with the
     * handlers registered on its own side.
     */
    private void connect() {
        if (ListenerAdapter.isInitialized()) {
            connected = true;
            return;
        }
        try {
            new ListenerAdapter(ServerIdentity.of(this, FALLBACK_SERVER));
            connected = true;
        } catch (Exception e) {
            // without the launcher there are no teams and therefore no backpacks, but a server that comes
            // up without them is still better than one that refuses to start
            getLogger().warning("Could not join the network - backpacks stay unavailable: " + e.getMessage());
        }
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
