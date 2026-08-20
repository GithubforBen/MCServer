package de.schnorrenbergers.bedwars;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.impl.BedTokenAddon;
import de.schnorrenbergers.bedwars.addon.impl.CustomItemsAddon;
import de.schnorrenbergers.bedwars.addon.impl.KillstreaksAddon;
import de.schnorrenbergers.bedwars.addon.impl.KitsAddon;
import de.schnorrenbergers.bedwars.addon.impl.RandomEventsAddon;
import de.schnorrenbergers.bedwars.commands.BedwarsCommand;
import de.schnorrenbergers.bedwars.config.GameSettings;
import de.schnorrenbergers.bedwars.config.GeneratorSettings;
import de.schnorrenbergers.bedwars.config.ModeSettings;
import de.schnorrenbergers.bedwars.config.ShopSettings;
import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.config.UpgradeSettings;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.listener.BedListener;
import de.schnorrenbergers.bedwars.listener.BuildListener;
import de.schnorrenbergers.bedwars.listener.ChatListener;
import de.schnorrenbergers.bedwars.listener.CombatListener;
import de.schnorrenbergers.bedwars.listener.DragonListener;
import de.schnorrenbergers.bedwars.listener.ShopListener;
import de.schnorrenbergers.bedwars.listener.SpecialItemListener;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.timeline.Dragons;
import de.schnorrenbergers.bedwars.game.timeline.Timeline;
import de.schnorrenbergers.bedwars.lobby.LobbyListener;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.MapLoader;
import de.schnorrenbergers.bedwars.map.MapRepository;
import de.schnorrenbergers.bedwars.map.setup.SetupSession;
import de.schnorrenbergers.bedwars.shop.ShopService;
import de.schnorrenbergers.bedwars.shop.trap.TrapService;
import de.schnorrenbergers.bedwars.shop.upgrade.UpgradeService;
import de.schnorrenbergers.bedwars.shop.villager.ShopKeepers;
import de.schnorrenbergers.bedwars.spectator.SpectatorListener;
import de.schnorrenbergers.bedwars.stats.FileStatsRepository;
import de.schnorrenbergers.bedwars.stats.StatsTracker;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

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
    private GeneratorSettings generatorSettings;
    private ShopSettings shopSettings;
    private UpgradeSettings upgradeSettings;
    private TimelineSettings timelineSettings;
    private ShopService shop;
    private AddonRegistry addons;
    private MapRepository maps;
    private MapLoader mapLoader;
    private SetupSession setup;
    private Game game;
    private StatsTracker stats;
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
        game.setGenerators(new GeneratorManager(generatorSettings));
        shop = new ShopService(shopSettings);
        game.setUpgrades(new UpgradeService(upgradeSettings));
        game.setTraps(new TrapService(upgradeSettings));
        game.setShopKeepers(new ShopKeepers());
        game.setTimeline(new Timeline(timelineSettings));
        game.setDragons(new Dragons(timelineSettings));
        loadArena();
        addons.apply(game);
        game.start(this);

        new LobbyListener(this);
        new BedListener(this);
        new BuildListener(this, game.getBlockTracker());
        new CombatListener(this);
        new ChatListener(this);
        new ShopListener(this);
        new SpecialItemListener(this);
        new DragonListener(this);
        new SpectatorListener(this);
        if (gameSettings.isStatsEnabled()) {
            stats = new StatsTracker(this, new FileStatsRepository(
                    new File(gameSettings.getStatsDirectory())));
        }
        register("bw", new BedwarsCommand());
        getLogger().info("Hosting " + game.getMode()
                + (game.getArena() == null ? " with no map yet" : " on " + game.getArena().getName())
                + (networked ? "" : ", without a network connection"));
    }

    @Override
    public void onDisable() {
        if (game != null && game.getShopKeepers() != null) game.getShopKeepers().remove();
        if (game != null && game.getDragons() != null) game.getDragons().remove();
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
        addons.reloadAll();
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
        if (generatorSettings == null) {
            generatorSettings = new GeneratorSettings();
        } else {
            generatorSettings.load();
        }
        if (shopSettings == null) {
            shopSettings = new ShopSettings();
        } else {
            shopSettings.load();
        }
        if (upgradeSettings == null) {
            upgradeSettings = new UpgradeSettings();
        } else {
            upgradeSettings.load();
        }
        if (timelineSettings == null) {
            timelineSettings = new TimelineSettings();
        } else {
            timelineSettings.load();
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
     * Registers every addon.
     * <p>
     * Registering is not enabling: what is actually switched on comes out of {@code addons.yml}, the event
     * that started this server and the lobby menu, in that order. Being listed here only means the round
     * knows that this addon exists.
     */
    private void registerAddons() {
        AddonSettings settings = addons.getSettings();
        addons.register(new BedTokenAddon(this, settings));
        addons.register(new KitsAddon(this, settings));
        addons.register(new CustomItemsAddon(this, settings));
        addons.register(new KillstreaksAddon(this, settings));
        addons.register(new RandomEventsAddon(this, settings));
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

    public GeneratorSettings getGeneratorSettings() {
        return generatorSettings;
    }

    public ShopSettings getShopSettings() {
        return shopSettings;
    }

    public UpgradeSettings getUpgradeSettings() {
        return upgradeSettings;
    }

    public TimelineSettings getTimelineSettings() {
        return timelineSettings;
    }

    /**
     * @return the shop of this round, which is what sells and what hands purchases back after a death
     */
    public ShopService getShop() {
        return shop;
    }

    /**
     * @return the team upgrades of this round
     */
    public UpgradeService getUpgrades() {
        return game == null ? null : game.getUpgrades();
    }

    /**
     * @return the traps of this round
     */
    public TrapService getTraps() {
        return game == null ? null : game.getTraps();
    }

    /**
     * @return whether this server reached the rest of the network
     */
    /**
     * @return what this round has come to so far, {@code null} when nothing is being kept
     */
    public @Nullable StatsTracker getStats() {
        return stats;
    }

    public boolean isNetworked() {
        return networked;
    }
}
