package de.schnorrenbergers.bedwars;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.ServerIdentity;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.LobbyCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.hologram.Holograms;
import de.hems.paper.event.EventService;
import de.hems.paper.cosmetic.CosmeticService;
import de.hems.paper.cosmetic.WinEffects;
import de.hems.paper.round.RoundService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.event.BedwarsEventSettings;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import de.hems.types.round.RoundData;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.impl.BedTokenAddon;
import de.schnorrenbergers.bedwars.addon.impl.CustomItemsAddon;
import de.schnorrenbergers.bedwars.addon.impl.KillstreaksAddon;
import de.schnorrenbergers.bedwars.addon.impl.KitsAddon;
import de.schnorrenbergers.bedwars.addon.impl.RandomEventsAddon;
import de.schnorrenbergers.bedwars.commands.BedwarsCommand;
import de.schnorrenbergers.bedwars.config.FeatureSettings;
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
import de.schnorrenbergers.bedwars.listener.RulesListener;
import de.schnorrenbergers.bedwars.listener.ShopListener;
import de.schnorrenbergers.bedwars.listener.SuddenDeathListener;
import de.schnorrenbergers.bedwars.listener.SpecialItemListener;
import de.schnorrenbergers.bedwars.listener.TeamChestListener;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.Rules;
import de.schnorrenbergers.bedwars.game.timeline.Dragons;
import de.schnorrenbergers.bedwars.game.timeline.Timeline;
import de.schnorrenbergers.bedwars.game.timeline.Withers;
import de.schnorrenbergers.bedwars.lobby.LobbyListener;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.MapLoader;
import de.schnorrenbergers.bedwars.map.MapRepository;
import de.schnorrenbergers.bedwars.map.setup.SetupSession;
import de.schnorrenbergers.bedwars.round.RoundContext;
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
    private FeatureSettings featureSettings;
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
    /** The event this round was ordered for, and when it begins. Both empty for a round nobody ordered. */
    private String eventName;
    private long eventStartsAt;

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

        game = new Game(modeSettings.get(modeToPlay()), gameSettings);
        game.setGenerators(new GeneratorManager(generatorSettings));
        shop = new ShopService(shopSettings);
        game.setUpgrades(new UpgradeService(upgradeSettings));
        game.setTraps(new TrapService(upgradeSettings));
        game.setShopKeepers(new ShopKeepers());
        game.setTimeline(new Timeline(timelineSettings));
        game.setDragons(new Dragons(timelineSettings));
        game.setWithers(new Withers(timelineSettings));
        loadArena();
        applyRoundAddons();
        addons.apply(game);
        game.start(this);

        new LobbyListener(this);
        new BedListener(this);
        new BuildListener(this, game.getBlockTracker());
        new TeamChestListener(this);
        new CombatListener(this);
        new ChatListener(this);
        new ShopListener(this);
        new SpecialItemListener(this);
        new SuddenDeathListener(this);
        new SpectatorListener(this);
        new RulesListener(this);
        new de.schnorrenbergers.bedwars.round.RoundStateListener(this);
        // what a round ends with, and what players carry into it
        WinEffects.init(this);
        new de.schnorrenbergers.bedwars.cosmetic.GadgetListener(this);
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
        ListenerAdapter.disconnect();
        // floating text is not persistent, so a clean stop loses it anyway - but a reload is not a stop,
        // and text left behind by the old instance is text nothing owns any more
        Holograms.removeAll();
        if (game != null && game.getShopKeepers() != null) game.getShopKeepers().remove();
        if (game != null && game.getDragons() != null) game.getDragons().remove();
        if (game != null && game.getWithers() != null) game.getWithers().remove();
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
        if (featureSettings == null) {
            featureSettings = new FeatureSettings();
        } else {
            featureSettings.load();
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
     * Works out which mode this round is: what the event that ordered it asked for, or what
     * {@code game.yml} says when nobody ordered it.
     * <p>
     * A server is created with a name and nothing else - no arguments, no config handed over - so the way
     * a round finds out what it was started for is to look itself up. The event carries the name of the
     * server it was started on, written before the server was even ordered, so exactly one event can be
     * the one that means this round.
     * <p>
     * The lookup blocks. That is deliberate and it is safe here: this runs while the server is starting,
     * the mode decides how many teams the round has, and a round that works that out a second later would
     * have to tear its teams down and build them again underneath whoever had already joined.
     *
     * @return the id of the mode to play
     */
    private String modeToPlay() {
        if (!networked) return gameSettings.getMode();
        String self = ServerIdentity.of(this, "BEDWARS").toString();
        EventService.refreshBlocking();
        for (EventData event : EventService.getEvents()) {
            if (event.getType() != EventType.BEDWARS) continue;
            BedwarsEventSettings settings = new BedwarsEventSettings(event);
            if (!self.equalsIgnoreCase(settings.getServer())) continue;
            eventName = event.getName();
            eventStartsAt = event.getStartsAt();
            getLogger().info("This round belongs to the event '" + event.getName() + "': "
                    + settings.getTeamSize() + " players per team, starting at "
                    + new java.util.Date(eventStartsAt) + ".");
            return settings.getMode();
        }
        // no event ordered this round, so somebody did: the same lookup, by the same name, in the list of
        // rounds players put up themselves
        RoundContext.load(self);
        RoundData round = RoundContext.get();
        if (round != null) {
            getLogger().info("This round was started by " + round.getOwnerName() + ": "
                    + round.getTeamSize() + " players per team on "
                    + (round.getMap() == null ? "whatever map is here" : round.getMap()) + ".");
            return round.getMode();
        }
        return gameSettings.getMode();
    }

    /**
     * Picks the map this server plays and loads a copy of it.
     * <p>
     * A server without a usable map still starts. That is the state you are in right before setting one
     * up, and a plugin that refuses to load there would make {@code /bw setup} impossible to reach.
     */
    private void loadArena() {
        RoundData round = RoundContext.get();
        String wanted = round != null && round.getMap() != null && !round.getMap().isBlank()
                ? round.getMap() : gameSettings.getMap();
        ArenaMap arena = maps.pick(wanted, game.getMode());
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
        // before anybody is let in: the locator bar and the time of day are what a player sees in their
        // first second on the server, and setting them afterwards is a flicker everybody notices
        Rules.applyTo(world, arena, featureSettings, true);
        getLogger().info("Arena " + arena.getName() + " is loaded as '" + world.getName()
                + "' in " + world.getWorldFolder().getPath());
    }

    /**
     * Switches the addons the way whoever started this round wanted them.
     * <p>
     * Their choice is the whole choice: an addon they did not tick is off, whatever {@code addons.yml}
     * says, because a menu that quietly leaves something on is a menu nobody trusts. An id the registry
     * does not know is ignored rather than fatal, so the lobby and this server can gain an addon in
     * either order.
     */
    private void applyRoundAddons() {
        RoundData round = RoundContext.get();
        if (round == null) return;
        java.util.Map<String, Boolean> wanted = new java.util.HashMap<>();
        for (de.schnorrenbergers.bedwars.addon.Addon addon : addons.all()) {
            wanted.put(addon.getId(), round.getAddons().contains(addon.getId()));
        }
        addons.applyEventOverrides(wanted);
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
        ServerConnector.register(this);
        // registered before the connection is attempted, so a round without a launcher still has a way out
        register("warp", new WarpCommand());
        register("lobby", new LobbyCommand());
        try {
            new ListenerAdapter(ServerIdentity.of(this, "BEDWARS"));
            new PlayerAdminHandler(this);
            EventService.init(this);
            RoundService.init(this);
            CosmeticService.init(this);
            networked = true;
        } catch (Exception e) {
            getLogger().warning("No network connection (" + e.getMessage()
                    + "). The round runs, but it cannot be started by an event or send anybody home.");
        }
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

    /**
     * How long the round still has to wait for the event it belongs to.
     * <p>
     * A round server for an event is put up before the event starts, so that people can gather in its
     * waiting lobby rather than in the hub. That is only worth anything if the round does not begin
     * underneath them in the meantime.
     *
     * @return the seconds until the event begins, 0 for a round that is not waiting for one
     */
    public long getSecondsUntilEvent() {
        if (eventStartsAt <= 0L) return 0L;
        long left = eventStartsAt - System.currentTimeMillis();
        return left <= 0L ? 0L : (left + 999L) / 1000L;
    }

    /**
     * @return what the event is called, or an empty string for a round nobody ordered
     */
    public String getEventName() {
        return eventName == null ? "" : eventName;
    }

    /**
     * @return the switches an admin flips in {@code /bw admin}
     */
    public FeatureSettings getFeatureSettings() {
        return featureSettings;
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
