package de.schnorrenbergers.bedwars;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.commands.BedwarsCommand;
import de.schnorrenbergers.bedwars.config.GameSettings;
import de.schnorrenbergers.bedwars.config.ModeSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.lobby.LobbyListener;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.MapLoader;
import de.schnorrenbergers.bedwars.map.MapRepository;
import de.schnorrenbergers.bedwars.map.setup.SetupSession;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * The bedwars server.
 * <p>
 * One server hosts one round, so the plugin builds that round while it starts and keeps it until the
 * server stops. Everything else - the map, the shop, the addons - hangs off that one {@link Game}.
 */
public final class Bedwars extends JavaPlugin {

    private static Bedwars instance;

    private ModeSettings modeSettings;
    private GameSettings gameSettings;
    private AddonRegistry addons;
    private MapRepository maps;
    private MapLoader mapLoader;
    private SetupSession setup;
    private Game game;
    private boolean networked;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        loadConfigs();
        connectToNetwork();

        addons = new AddonRegistry(new AddonSettings());
        registerAddons();

        maps = new MapRepository();
        mapLoader = new MapLoader(maps);

        game = new Game(modeSettings.get(gameSettings.getMode()), gameSettings);
        loadArena();
        addons.apply(game);
        game.start(this);

        new LobbyListener(this);
        register("bw", new BedwarsCommand());
        getLogger().info("Hosting " + game.getMode()
                + (game.getArena() == null ? " with no map yet" : " on " + game.getArena().getName())
                + (networked ? "" : ", without a network connection"));
    }

    @Override
    public void onDisable() {
        if (addons != null && game != null) addons.disableAll(game);
        if (game != null) game.shutdown();
    }

    /**
     * Reads every config again, for {@code /bw reload}.
     * <p>
     * The round itself is not rebuilt: a mode or a map that changed while people are standing in the lobby
     * would move them into a game they never joined. Those take effect on the next server.
     */
    public void reload() {
        loadConfigs();
        addons.getSettings().reload();
        addons.apply(game);
    }

    private void loadConfigs() {
        Messages.load();
        if (modeSettings == null) {
            modeSettings = new ModeSettings();
        } else {
            modeSettings.load();
        }
        if (gameSettings == null) {
            gameSettings = new GameSettings();
        } else {
            gameSettings.load();
        }
    }

    /**
     * Picks the map this server plays and loads a copy of it.
     * <p>
     * A server without a usable map still starts. That is the state you are in right before setting one
     * up, and a plugin that refuses to load there would make {@code /bw setup} impossible to reach.
     */
    private void loadArena() {
        ArenaMap arena = maps.pick(gameSettings.getMap(), game.getMode());
        if (arena == null) {
            getLogger().warning("No map to play. Put a world folder into " + maps.getDirectory().getPath()
                    + " and set it up with /bw setup <name>.");
            return;
        }
        World world = mapLoader.load(arena.getName());
        if (world == null) {
            getLogger().warning("The map " + arena.getName() + " could not be loaded.");
            return;
        }
        game.setArena(arena, world);
        getLogger().info("Arena " + arena.getName() + " is loaded as '" + world.getName()
                + "' in " + world.getWorldFolder().getPath());
    }

    /**
     * Registers every addon. Phase 6 fills this in; until then the registry is real but empty, which is
     * what the rest of the plugin needs it to be.
     */
    private void registerAddons() {
    }

    /**
     * Joins the network the launcher and the other servers talk on.
     * <p>
     * A failure is not fatal on purpose. Setting maps up is local work, and a plugin that refuses to start
     * without a launcher would make that impossible.
     */
    private void connectToNetwork() {
        new CustomInventoryListener(this);
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.valueOf(serverName()));
            ServerConnector.register(this);
            networked = true;
        } catch (Exception e) {
            getLogger().warning("No network connection (" + e.getMessage()
                    + "). The round runs, but it cannot be started by an event or send anybody home.");
        }
    }

    /**
     * @return the name this server is known by, which the launcher passes in as the directory it runs in
     */
    private String serverName() {
        File container = getServer().getWorldContainer();
        try {
            // canonical, not absolute: a world container of "." keeps its dot through getAbsoluteFile(),
            // and this server would register itself under the name "." for the rest of its life
            container = container.getCanonicalFile();
        } catch (IOException e) {
            container = container.getAbsoluteFile();
        }
        String directory = container.getName();
        return directory == null || directory.isBlank() || ".".equals(directory) ? "BEDWARS" : directory;
    }

    private void register(String name, Object command) {
        PluginCommand registered = getCommand(name);
        if (registered == null) {
            getLogger().warning("The command /" + name + " is missing from plugin.yml");
            return;
        }
        registered.setExecutor((CommandExecutor) command);
        if (command instanceof TabCompleter completer) registered.setTabCompleter(completer);
    }

    /**
     * Puts this server into setup mode, which holds the round while a map is being built.
     *
     * @param session the map that is being worked on
     */
    public void startSetup(SetupSession session) {
        setup = session;
        game.setSetupMode(true);
    }

    /**
     * Leaves setup mode. The map stays loaded - it is the world everybody is standing in.
     */
    public void stopSetup() {
        setup = null;
        game.setSetupMode(false);
    }

    public SetupSession getSetup() {
        return setup;
    }

    public MapRepository getMaps() {
        return maps;
    }

    public MapLoader getMapLoader() {
        return mapLoader;
    }

    public static Bedwars getInstance() {
        return instance;
    }

    public Game getGame() {
        return game;
    }

    public AddonRegistry getAddons() {
        return addons;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public ModeSettings getModeSettings() {
        return modeSettings;
    }

    /**
     * @return whether this server reached the rest of the network
     */
    public boolean isNetworked() {
        return networked;
    }
}
