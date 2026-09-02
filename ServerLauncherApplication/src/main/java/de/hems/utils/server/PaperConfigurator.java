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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PaperConfigurator extends ServerConfigurator {

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
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", op.toString());
            jsonObject.addProperty("name", UUIDFetcher.findNameByUUID(op));
            jsonObject.addProperty("level", 4);
            jsonObject.addProperty("bypassesPlayerLimit", true);
            jsonArray.add(jsonObject);
        }
        System.out.println(ops.size() + ":" + jsonArray);
        overwriteToFile("ops.json", jsonArray.toString(), true);
        jsonArray = new JsonArray();
        for (String whitelisted : whitelist) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", UUIDFetcher.findUUIDByName(whitelisted, true).toString());
            jsonObject.addProperty("name", whitelisted);
            jsonArray.add(jsonObject);
        }
        overwriteToFile("whitelist.json", jsonArray.toString(), false);
        System.out.println("Configured server " + name + " on port " + port);
    }
}
