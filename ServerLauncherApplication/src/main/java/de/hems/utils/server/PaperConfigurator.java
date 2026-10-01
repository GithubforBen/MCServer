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
}
