package de.hems.paper;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.admin.NetworkOps;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.LobbyCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.EventService;
import de.hems.paper.warp.ServerConnector;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.function.Supplier;

/**
 * What every game server of the network does first: join the network and set up what every server has.
 * <p>
 * The lobby, survival, bedwars, the casino, the arena and the run servers each used to do this by hand, in a
 * slightly different order and with slightly different gaps - only two of them applied an op granted on
 * discord straight away, for one. Here it is once:
 * <ul>
 *     <li>the menus and the connection to the proxy, and the ways out ({@code /warp}, {@code /lobby},
 *     {@code /servermanger}) - set up <em>before</em> the network, so a server without one is not a trap</li>
 *     <li>the network itself</li>
 *     <li>then what needs it: the admin tools, ops, the event calendar and {@code /events}</li>
 * </ul>
 * A command is only hooked up when the plugin declares it, so the lobby, which has no {@code /lobby}, is not
 * warned about it.
 * <p>
 * What happens without a network is the plugin's own decision - a casino must not open, an arena can still
 * be played - so this answers whether it worked and leaves the rest to the caller.
 */
public final class NetworkPlugin {

    /** The commands every server offers, hooked up when the plugin declares them. */
    private static final Map<String, Supplier<CommandExecutor>> WAYS_OUT = Map.of(
            "warp", WarpCommand::new,
            "lobby", LobbyCommand::new,
            "servermanger", ServerManagerCommand::new);

    private NetworkPlugin() {
    }

    /**
     * @param plugin the plugin of this server
     * @param name   what this server is called on the network
     * @return whether the network is there; everything that needs it has been set up if so
     */
    public static boolean connect(JavaPlugin plugin, ListenerAdapter.ServerName name) {
        new CustomInventoryListener(plugin);
        ServerConnector.register(plugin);
        for (Map.Entry<String, Supplier<CommandExecutor>> command : WAYS_OUT.entrySet()) {
            if (plugin.getCommand(command.getKey()) != null) {
                PluginCommands.register(plugin, command.getKey(), command.getValue().get());
            }
        }
        try {
            new ListenerAdapter(name);
        } catch (Exception e) {
            plugin.getLogger().warning("No network connection: " + e.getMessage());
            return false;
        }
        new PlayerAdminHandler(plugin);
        NetworkOps.init(plugin);
        EventService.init(plugin);
        if (plugin.getCommand("events") != null) PluginCommands.register(plugin, "events", new EventCommand());
        return true;
    }

    /**
     * @param plugin   the plugin of this server
     * @param fallback the name to use when the server directory says nothing useful
     * @return whether the network is there
     */
    public static boolean connect(JavaPlugin plugin, String fallback) {
        return connect(plugin, ServerIdentity.of(plugin, fallback));
    }
}
