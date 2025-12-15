package de.hems.utils.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.hems.FileHandler;
import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.MissingConfigurationException;
import de.hems.types.SecureTokenGenerator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class PaperConfigurator extends ServerConfigurator {

    private final int port;
    private final boolean isProxyed;
    private final List<UUID> ops;
    private final String[] whitelist;
    private final FileType.PLUGIN[] plugins;
    private final ListenerAdapter.ServerName name;

    public PaperConfigurator(ListenerAdapter.ServerName name, boolean isProxyed, List<UUID> ops, String[] whitelist, String directory, FileType.PLUGIN[] plugins) {
        super(directory);
        this.port = name.getPort();
        this.isProxyed = isProxyed;
        this.ops = ops;
        this.whitelist = whitelist;
        this.plugins = plugins;
        this.name = name;
    }

    public void configure() throws Exception {
        File jar = new File(this.directory + "/" + FileType.SERVER.getFileName(FileType.SERVER.PAPER));
        File jarFile = new FileHandler().provideFile(FileType.SERVER.PAPER);
        Files.copy(jarFile.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        for (FileType.PLUGIN plugin : plugins) {
            File pluginF = new File(this.directory + "/plugins/" + FileType.PLUGIN.getFileName(plugin));
            pluginF.getParentFile().mkdirs();
            File pluginFile = new FileHandler().provideFile(plugin);
            Files.copy(pluginFile.toPath(), pluginF.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        writeToFile("eula.txt", "eula=true");
        writeToFile("server.properties", "server-ip=" + Main.getInstance().getIp());
        writeToFile("server.properties", "server-port=" + port);
        if (isProxyed) {
            writeToFile("server.properties", "online-mode=false");
            writeToYmlConfiguration("paper-global.yml", "proxies.velocity.enabled", true);
            writeToYmlConfiguration("paper-global.yml", "proxies.velocity.online-mode", true);

            if (Main.getInstance().getConfiguration().getConfig().contains("serversecret")) {
                writeToYmlConfiguration("paper-global.yml","proxies.velocity.secret",  Main.getInstance().getConfiguration().getConfig().getString("serversecret"));
            } else {
                Main.getInstance().getConfiguration().getConfig().setComments("serversecret", List.of("The velocity secret key."));
                Main.getInstance().getConfiguration().getConfig().set("serversecret", SecureTokenGenerator.generateSecureToken());
                Main.getInstance().getConfiguration().save();
                throw new MissingConfigurationException("serversecret is missing in config.yml. ou might want to change it to something else. It was auto set.");
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
        System.out.println(ops.size() + ":" + jsonArray.toString());
        writeToFileIfFileDoesntExist("ops.json", jsonArray.toString());
        jsonArray = new JsonArray();
        for (String whitelist : whitelist) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", UUIDFetcher.findUUIDByName(whitelist, true).toString());
            jsonObject.addProperty("name", whitelist);
            jsonArray.add(jsonObject);
        }
        writeToFileIfFileDoesntExist("whitelist.json", jsonArray.toString());
        System.out.println("Configured server " + name.toString());
    }
}
