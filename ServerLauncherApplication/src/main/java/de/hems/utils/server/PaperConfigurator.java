package de.hems.utils.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.hems.FileHandler;
import de.hems.FileType;
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

    public PaperConfigurator(String ip, int port, boolean isProxyed, List<UUID> ops, String[] whitelist, String directory) {
        super(directory);
        this.ip = ip;
        this.port = port;
        this.isProxyed = isProxyed;
        this.ops = ops;
        this.whitelist = whitelist;
    }

    public void configure() throws Exception {
        File jar = new File(this.directory + "/" + FileType.getFileName(FileType.PAPER));
        File file = new FileHandler().provideFile(FileType.PAPER);
        Files.copy(file.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
