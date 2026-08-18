package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the players that pay for the server.
 * <p>
 * The list lives in the launcher config under {@code paying-players} and is the same one the discord
 * command writes and the survival server reads, so a change here reaches the game without a restart.
 */
public class PayingPlayerModule implements WebModule {

    /** The config key the list is stored under. */
    private static final String CONFIG_KEY = "paying-players";
    /** Names looked up at mojang, so the list does not hit their api on every page load. */
    private static final Map<UUID, String> NAME_CACHE = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "paying-players";
    }

    @Override
    public String getTitle() {
        return "Paying Player";
    }

    @Override
    public String getDescription() {
        return "Legt fest wer für den Server zahlt und dadurch die volle Sichtweite behält.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/paying-players", this::list);
        server.post("/api/paying-players", this::add);
        server.delete("/api/paying-players/{uuid}", this::remove);
    }

    private void list(ApiContext ctx) {
        JSONArray array = new JSONArray();
        for (UUID uuid : read()) {
            array.put(new JSONObject().put("uuid", uuid.toString()).put("name", nameOf(uuid)));
        }
        ctx.ok("players", array);
    }

    /**
     * Adds a player, either by minecraft name or by uuid.
     *
     * @param ctx the request being answered
     */
    private void add(ApiContext ctx) {
        String uuidInput = ctx.string("uuid", "");
        String nameInput = ctx.string("name", "");
        if (uuidInput.isEmpty() && nameInput.isEmpty()) {
            ctx.error(400, "Es fehlt ein Minecraft-Name oder eine UUID.");
            return;
        }

        UUID uuid;
        if (!uuidInput.isEmpty()) {
            try {
                uuid = UUID.fromString(uuidInput);
            } catch (IllegalArgumentException e) {
                ctx.error(400, "'" + uuidInput + "' ist keine gültige UUID.");
                return;
            }
        } else {
            uuid = UUIDFetcher.findUUIDByName(nameInput, true);
            if (uuid == null) {
                ctx.error(404, "Es gibt keinen Spieler namens '" + nameInput + "'.");
                return;
            }
            NAME_CACHE.put(uuid, nameInput);
        }

        Set<UUID> players = read();
        if (!players.add(uuid)) {
            ctx.error(409, nameOf(uuid) + " zahlt bereits.");
            return;
        }
        write(players);
        ctx.ok(new JSONObject()
                .put("ok", true)
                .put("message", nameOf(uuid) + " wurde hinzugefügt.")
                .put("uuid", uuid.toString())
                .put("name", nameOf(uuid)));
    }

    /**
     * Removes a player, addressed as {@code /api/paying-players/<uuid>}.
     *
     * @param ctx the request being answered
     */
    private void remove(ApiContext ctx) {
        UUID uuid;
        try {
            uuid = UUID.fromString(ctx.pathParam("uuid"));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Das ist keine gültige UUID.");
            return;
        }
        Set<UUID> players = read();
        if (!players.remove(uuid)) {
            ctx.error(404, "Dieser Spieler steht nicht auf der Liste.");
            return;
        }
        write(players);
        ctx.ok(nameOf(uuid) + " wurde entfernt.");
    }

    /**
     * @return the players that currently pay, ignoring entries that are not uuids
     */
    private static Set<UUID> read() {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        Set<UUID> players = new LinkedHashSet<>();
        for (String entry : config.getStringList(CONFIG_KEY)) {
            try {
                players.add(UUID.fromString(entry.trim()));
            } catch (IllegalArgumentException ignored) {
                // an entry that is not a uuid would be useless to the survival plugin - drop it
            }
        }
        return players;
    }

    private static void write(Set<UUID> players) {
        List<String> stored = new ArrayList<>(players.size());
        for (UUID uuid : players) stored.add(uuid.toString());
        Main.getInstance().getConfiguration().getConfig().set(CONFIG_KEY, stored);
        Main.getInstance().getConfiguration().save();
    }

    /**
     * @param uuid the player to name
     * @return the minecraft name, or the uuid if mojang does not know it
     */
    private static String nameOf(UUID uuid) {
        return NAME_CACHE.computeIfAbsent(uuid, key -> {
            String name = UUIDFetcher.findNameByUUID(key);
            return name == null ? key.toString() : name;
        });
    }
}
