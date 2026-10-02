package de.hems.utils.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.hems.FileHandler;
import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.MissingConfigurationException;
import de.hems.types.ServerTemplate;
import de.hems.utils.whitelist.WhitelistSync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PaperConfigurator extends ServerConfigurator {
    /**
     * The version paper writes into its config files. A file written before the first start carries it, so
     * paper takes it as current and only fills in what is missing - without it, paper would take the file
     * for one from before its first migration and run every one of them over it.
     */
    private static final int PAPER_CONFIG_VERSION = 31;

    /** What anti-xray hides, overworld and nether - one list, since engine mode 1 picks the cover per world. */
    private static final List<String> HIDDEN_ORES = List.of(
            "coal_ore", "deepslate_coal_ore", "copper_ore", "deepslate_copper_ore", "raw_copper_block",
            "iron_ore", "deepslate_iron_ore", "raw_iron_block", "gold_ore", "deepslate_gold_ore",
            "redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore",
            "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore",
            "nether_gold_ore", "nether_quartz_ore", "ancient_debris",
            "chest", "trapped_chest", "barrel", "spawner", "amethyst_cluster");

    /** The seed the survival world is generated from. */
    private static final String SURVIVAL_SEED = "8750345191364376078";


    /** Where the hunger games map lies next to the launcher. */
    public static final String HUNGER_GAMES_MAP = "./hungergames-world";

    private final int port;
    private final boolean isProxyed;
    private final List<UUID> ops;
    private final String[] whitelist;
    private final FileType.PLUGIN[] plugins;
    private final ListenerAdapter.ServerName name;
    private final ServerTemplate template;

    public PaperConfigurator(ListenerAdapter.ServerName name, boolean isProxyed, List<UUID> ops, String[] whitelist, String directory, FileType.PLUGIN[] plugins) throws IOException {
        this(name, isProxyed, ops, whitelist, directory, plugins, ServerTemplate.forServerName(name.toString()));
    }

    public PaperConfigurator(ListenerAdapter.ServerName name, boolean isProxyed, List<UUID> ops, String[] whitelist,
                             String directory, FileType.PLUGIN[] plugins, ServerTemplate template) throws IOException {
        super(directory);
        this.port = name.getPort();
        this.isProxyed = isProxyed;
        this.ops = ops;
        this.whitelist = whitelist;
        this.plugins = plugins;
        this.name = name;
        this.template = template == null ? ServerTemplate.forServerName(name.toString()) : template;
    }

    public void configure() throws Exception {
        String jarName = FileType.SERVER.getFileName(FileType.SERVER.PAPER);
        File jar = new File(this.directory + "/" + jarName);
        // a new Minecraft version converts the worlds for good, so they are copied away first - before the
        // old jar is removed, because that jar is how the old version is recognised
        WorldBackup.beforeUpgrade(new File(this.directory), jarName);
        File jarFile = new FileHandler().provideFile(FileType.SERVER.PAPER);
        Files.copy(jarFile.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        removeStaleServerJars(jarName);

        new File(this.directory + "/plugins/").mkdirs();
        removeStalePlugins(Arrays.asList(plugins));
        for (FileType.PLUGIN plugin : plugins) {
            File pluginF = new File(this.directory + "/plugins/" + FileType.PLUGIN.getFileName(plugin));
            pluginF.getParentFile().mkdirs();
            File pluginFile = new FileHandler().provideFile(plugin);
            Files.copy(pluginFile.toPath(), pluginF.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        // the worlds and the configuration that belongs to them, written once and then left to the admins
        new AssetInstaller(new File(this.directory)).install(template.getAssets());
        // the lobby is the built spawn again on every start - here, because a running server cannot swap its
        // main world
        if (template == ServerTemplate.LOBBY) {
            LobbyMap.installInto(new File(this.directory));
        }
        // and the maps somebody dropped into ./bedwars-maps themselves, which no release knows about
        if (template == ServerTemplate.BEDWARS) {
            new CustomMaps().installInto(new File(this.directory));
        }
        // the casino is built once and then belongs to whoever builds on it, so it travels from night to
        // night in ./poker-world rather than being generated again on every fresh server
        if (template == ServerTemplate.POKER) {
            new CasinoMap().installInto(new File(this.directory));
        }
        // hunger games is one world and its border: the prepared map becomes the main world, and the
        // other two dimensions are switched off so nobody can leave the arena through a portal
        if (template == ServerTemplate.HUNGER_GAMES) {
            new PreparedMap(HUNGER_GAMES_MAP, "world").installInto(new File(this.directory));
            setProperty("server.properties", "allow-nether", false);
            // the cornucopia stands at spawn, and vanilla spawn protection would make it unbreakable and its
            // chests unopenable for everybody who is not an operator
            setProperty("server.properties", "spawn-protection", 0);
            setProperty("server.properties", "difficulty", "normal");
            writeToYmlConfiguration("bukkit.yml", "settings.allow-end", false, true);
        }

        // survival is generated from one fixed seed, so a fresh installation gets the same world; only read
        // when the world is created, an existing one stays as it is
        if (template == ServerTemplate.SURVIVAL) {
            setProperty("server.properties", "level-seed", SURVIVAL_SEED);
            survivalRules();
        }

        overwriteToFile("eula.txt", "eula=true", true);
        // written every time so that a server keeps working after it was given another port
        setProperty("server.properties", "server-ip", "localhost");
        setProperty("server.properties", "server-port", port);
        setProperty("server.properties", "motd", name.toString());
        if (isProxyed) {
            setProperty("server.properties", "online-mode", false);
            writeToYmlConfiguration("config/paper-global.yml", "proxies.velocity.enabled", true, true);
            writeToYmlConfiguration("config/paper-global.yml", "proxies.velocity.online-mode", true, true);

            if (Main.getInstance().getConfiguration().getConfig().contains("serversecret")) {
                writeToYmlConfiguration("config/paper-global.yml","proxies.velocity.secret",  Main.getInstance().getConfiguration().getConfig().getString("serversecret"), true);
            } else {
                throw new MissingConfigurationException("serversecret is missing in config.yml. You need to start the velocity server first!");
            }
        }
        JsonArray jsonArray = new JsonArray();
        for (UUID op : ops) {
            // a name the account service could not resolve right now - it is down, or it answers too many
            // lookups with nothing. One missing operator must not keep the whole server from starting
            if (op == null) {
                System.out.println("An operator of " + name + " could not be looked up and is left out of ops.json.");
                continue;
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", op.toString());
            jsonObject.addProperty("name", UUIDFetcher.findNameByUUID(op));
            jsonObject.addProperty("level", 4);
            jsonObject.addProperty("bypassesPlayerLimit", true);
            jsonArray.add(jsonObject);
        }
        System.out.println(ops.size() + ":" + jsonArray);
        overwriteToFile("ops.json", jsonArray.toString(), true);
        // written every time, so a player who accepted the rules while this server was off is on it too
        overwriteToFile("whitelist.json", WhitelistSync.entries(Arrays.asList(whitelist)).toString(), true);
        boolean enforced = Main.getInstance().getWhitelistStore().isEnforced();
        setProperty("server.properties", "white-list", enforced);
        // with this, "whitelist reload" also sends off whoever was taken off the list
        setProperty("server.properties", "enforce-whitelist", enforced);
        writeToYmlConfiguration("spigot.yml", "messages.whitelist", WhitelistSync.kickMessage(), true);
        System.out.println("Configured server " + name + " on port " + port);
    }

    /**
     * What survival plays by, written at every start - so it holds on a server that was just created as much
     * as on one that has been running for months:
     * <ul>
     *     <li>anti-xray on. Engine mode 1 sends the ores nobody can see as stone (netherrack in the nether),
     *     which leaves an x-ray texture pack nothing to show; §2.3 of the rules forbids it, this enforces it.</li>
     *     <li>the dupes the rules allow (§2.2): TNT, carpet and rail dupers (piston duplication), sand and
     *     gravel through the end portal, and string through tripwire hooks. Paper switches all three off by
     *     default.</li>
     *     <li>attribute swapping, as vanilla has it: vanilla only picks up the attributes of the hand a moment
     *     after the hotbar slot changes, and a hit in that moment combines two items - a spear with a sword,
     *     an axe or a mace. Paper refreshes the equipment on every player action and so takes that away by
     *     default.</li>
     * </ul>
     */
    private void survivalRules() throws Exception {
        String world = "config/paper-world-defaults.yml";
        String global = "config/paper-global.yml";
        stamp(world);
        stamp(global);
        writeToYmlConfiguration(world, "anticheat.anti-xray.enabled", true, true);
        writeToYmlConfiguration(world, "anticheat.anti-xray.engine-mode", 1, true);
        // the nether's ancient debris goes up to 119; the overworld's ores above this are on mountain faces
        writeToYmlConfiguration(world, "anticheat.anti-xray.max-block-height", 128, true);
        writeToYmlConfiguration(world, "anticheat.anti-xray.hidden-blocks", HIDDEN_ORES, true);
        writeToYmlConfiguration(global, "unsupported-settings.allow-piston-duplication", true, true);
        writeToYmlConfiguration(global, "unsupported-settings.allow-unsafe-end-portal-teleportation", true, true);
        writeToYmlConfiguration(global, "unsupported-settings.skip-tripwire-hook-placement-validation", true, true);
        writeToYmlConfiguration(global, "unsupported-settings.update-equipment-on-player-actions", false, true);
    }

    /**
     * Gives a config file that paper has not written yet the version paper would, see
     * {@link #PAPER_CONFIG_VERSION}.
     */
    private void stamp(String file) throws Exception {
        if (new File(this.directory, file).exists()) return;
        writeToYmlConfiguration(file, "_version", PAPER_CONFIG_VERSION, true);
    }
}
