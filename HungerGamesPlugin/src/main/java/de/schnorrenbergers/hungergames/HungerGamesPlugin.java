package de.schnorrenbergers.hungergames;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.ServerIdentity;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.LobbyCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.EventService;
import de.hems.paper.warp.ServerConnector;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin an arena carries.
 * <p>
 * An arena exists for one hunger games event. The lobby puts it up a few minutes before the event and
 * writes its name onto the event; the arena reads its rules from there, fills the map, waits for its players
 * and plays one game. The launcher switches it off and throws it away when the event is settled.
 * <p>
 * The order in {@link #onEnable} matters: the network first, because the rules come from the event; the
 * map second, because the game needs its middle.
 */
public final class HungerGamesPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        new CustomInventoryListener(this);
        ServerConnector.register(this);
        // the way out has to work even when nothing else does
        registerCommand("warp", new WarpCommand());
        registerCommand("lobby", new LobbyCommand());
        registerCommand("servermanger", new ServerManagerCommand());

        String self;
        try {
            self = ServerIdentity.of(this, "HUNGER_GAMES").toString();
            new ListenerAdapter(ServerIdentity.of(this, "HUNGER_GAMES"));
            new PlayerAdminHandler(this);
            EventService.init(this);
            registerCommand("events", new EventCommand());
        } catch (Exception e) {
            // without the network the game can still be played, it just counts for nothing
            getLogger().warning("No network connection (" + e.getMessage()
                    + "). The game can be played, but nothing is reported.");
            self = "HUNGER_GAMES";
        }

        ArenaContext.load(self);
        Loot.load(this);
        ArenaMap map = ArenaMap.load(this);

        game = new Game(this, map, ArenaContext.getSettings());
        getServer().getPluginManager().registerEvents(new GameListener(this, game), this);
        registerCommand("hg", new HgCommand(game));
        game.start();
    }

    @Override
    public void onDisable() {
        if (game != null) game.shutdown();
        // while the jar is still open, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
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
}
