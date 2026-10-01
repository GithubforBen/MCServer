package de.hems.utils.whitelist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.types.FileType;
import de.hems.utils.server.ServerInstance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Carries the whitelist to the paper servers: all of it when a server is configured, and one player at a
 * time to the servers that are already running, so somebody who just accepted the rules can join right
 * away instead of after the next restart.
 * <p>
 * A running server is changed through its {@code whitelist.json} and {@code whitelist reload} rather than
 * {@code whitelist add}: the servers run behind the proxy in offline mode, and there the command would have
 * to work out a uuid on its own. Here the uuid is already known.
 */
public final class WhitelistSync {

    private static final String FILE = "whitelist.json";

    private WhitelistSync() {
    }

    /**
     * Everybody who may join a server: the names the admins put into the config, and the players who
     * accepted the rules.
     *
     * @param adminNames the names from {@code main-config.yml}
     * @return the content of {@code whitelist.json}
     */
    public static JsonArray entries(List<String> adminNames) {
        JsonArray array = new JsonArray();
        Set<UUID> seen = new HashSet<>();
        for (String name : adminNames) {
            UUID uuid = UUIDFetcher.findUUIDByName(name, true);
            // a typo or a renamed account used to stop the whole server from starting
            if (uuid == null) {
                System.out.println("Whitelist: mojang does not know '" + name + "' - left out.");
                continue;
            }
            if (seen.add(uuid)) array.add(entry(uuid, name));
        }
        WhitelistStore store = store();
        if (store != null) {
            for (WhitelistStore.Entry player : store.list()) {
                if (seen.add(player.uuid())) array.add(entry(player.uuid(), player.name()));
            }
        }
        return array;
    }

    /**
     * @return what a player who is not on the whitelist is told, with the way onto it
     */
    public static String kickMessage() {
        return "Du stehst nicht auf der Whitelist. Lies und akzeptiere die Regeln auf " + rulesUrl()
                + " - danach kannst du sofort spielen.";
    }

    /**
     * @return where players find the rules page
     */
    public static String rulesUrl() {
        WhitelistStore store = store();
        if (store != null && store.getPublicUrl() != null) return store.getPublicUrl();
        String host;
        try {
            host = Main.getInstance().getPublicAddress();
        } catch (IOException | RuntimeException e) {
            host = "localhost";
        }
        int port = Main.getInstance().getConfiguration().getConfig().getInt("web.port", 8080);
        return "http://" + host + ":" + port + "/regeln";
    }

    /**
     * Lets a player onto every server that is running now.
     */
    public static synchronized void add(UUID uuid, String name) {
        for (ServerInstance server : paperServers()) {
            patch(server, array -> {
                for (JsonElement element : array) {
                    if (uuid.toString().equals(element.getAsJsonObject().get("uuid").getAsString())) return false;
                }
                array.add(entry(uuid, name));
                return true;
            });
        }
    }

    /**
     * Takes a player off every server that is running now - unless an admin put the same name into the
     * config, which is a separate reason for them to be there.
     */
    public static synchronized void remove(UUID uuid, String name) {
        List<String> adminNames = Main.getInstance().getConfiguration().getConfig().getStringList("whitelist");
        for (String adminName : adminNames) {
            if (adminName.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) return;
        }
        for (ServerInstance server : paperServers()) {
            patch(server, array -> {
                for (int i = 0; i < array.size(); i++) {
                    if (uuid.toString().equals(array.get(i).getAsJsonObject().get("uuid").getAsString())) {
                        array.remove(i);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    /**
     * Switches the whitelist on or off on every server that is running now. The server writes it into its
     * {@code server.properties} itself; the next configure writes the same.
     */
    public static synchronized void setEnforced(boolean enforced) {
        for (ServerInstance server : paperServers()) {
            command(server, enforced ? "whitelist on" : "whitelist off");
        }
    }

    private interface Change {
        /** @return whether the list was changed and has to be written */
        boolean apply(JsonArray array);
    }

    private static void patch(ServerInstance server, Change change) {
        File file = new File(server.getDirectory(), FILE);
        try {
            JsonArray array = file.isFile()
                    ? JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonArray()
                    : new JsonArray();
            if (!change.apply(array)) return;
            Files.writeString(file.toPath(), array.toString(), StandardCharsets.UTF_8);
            command(server, "whitelist reload");
        } catch (IOException | RuntimeException e) {
            System.out.println("Whitelist: could not update " + server.getName() + ": " + e.getMessage());
        }
    }

    private static void command(ServerInstance server, String command) {
        try {
            server.executeCommand(command);
        } catch (IOException e) {
            System.out.println("Whitelist: could not send '" + command + "' to " + server.getName() + ": " + e.getMessage());
        }
    }

    private static List<ServerInstance> paperServers() {
        List<ServerInstance> servers = new ArrayList<>();
        for (ServerInstance instance : Main.getInstance().getServerHandler().getInstances()) {
            if (instance.getJarFile() == FileType.SERVER.PAPER) servers.add(instance);
        }
        return servers;
    }

    private static JsonObject entry(UUID uuid, String name) {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", uuid.toString());
        object.addProperty("name", name);
        return object;
    }

    private static WhitelistStore store() {
        Main main = Main.getInstance();
        return main == null ? null : main.getWhitelistStore();
    }
}
