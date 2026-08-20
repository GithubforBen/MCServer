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
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
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

        game = new Game(modeSettings.get(gameSettings.getMode()), gameSettings);
        addons.apply(game);
        game.start(this);

        register("bw", new BedwarsCommand());
        getLogger().info("Hosting " + game.getMode() + (networked ? "" : " without a network connection"));
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
