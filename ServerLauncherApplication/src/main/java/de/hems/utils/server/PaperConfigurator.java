package de.hems.utils.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.hems.FileHandler;
import de.hems.types.FileType;
import de.hems.api.UUIDFetcher;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class PaperConfigurator extends ServerConfigurator {

    private final String ip;
    private final int port;
    private final boolean isProxyed;
    private final List<UUID> ops;
    private final String[] whitelist;
    private final FileType.PLUGIN[] plugins;

    public PaperConfigurator(String ip, int port, boolean isProxyed, List<UUID> ops, String[] whitelist, String directory, FileType.PLUGIN[] plugins) {
        super(directory);
        this.ip = ip;
        this.port = port;
        this.isProxyed = isProxyed;
        this.ops = ops;
        this.whitelist = whitelist;
        this.plugins = plugins;
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
        writeToFile("server.properties", "server-ip=" + ip);
        writeToFile("server.properties", "server-port=" + port);
        if (isProxyed) writeToFile("server.properties", "online-mode=false");
        JsonArray jsonArray = new JsonArray();
        for (UUID op : ops) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", op.toString());
            jsonObject.addProperty("name", UUIDFetcher.findNameByUUID(op));
            jsonObject.addProperty("level", 4);
            jsonObject.addProperty("bypassesPlayerLimit", true);
            jsonArray.add(jsonObject);
        }
        writeToFile("ops.json", jsonArray.toString());
        jsonArray = new JsonArray();
        for (String whitelist : whitelist) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", UUIDFetcher.findUUIDByName(whitelist, true).toString());
            jsonObject.addProperty("name", whitelist);
            jsonArray.add(jsonObject);
        }
        writeToFile("whitelist.json", jsonArray.toString());
    }
}
