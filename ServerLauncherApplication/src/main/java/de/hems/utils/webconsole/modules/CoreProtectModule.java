package de.hems.utils.webconsole.modules;

import de.hems.communication.ListenerAdapter;
import de.hems.types.admin.CoreProtectEntry;
import de.hems.types.admin.LookupQuery;
import de.hems.utils.webconsole.AdminNetwork;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * CoreProtect from the browser.
 * <p>
 * Every lookup the {@code /co lookup} command offers is here: blocks, containers, items, inventories,
 * sessions, chat, commands, signs and name changes. Each paper server keeps its own CoreProtect database,
 * so a lookup is always addressed at one server rather than broadcast.
 */
public class CoreProtectModule implements WebModule {

    @Override
    public String getId() {
        return "coreprotect";
    }

    @Override
    public String getTitle() {
        return "CoreProtect";
    }

    @Override
    public String getDescription() {
        return "Durchsucht die Logs von CoreProtect nach Blöcken, Kisten, Chat und mehr.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/coreprotect", this::options);
        server.post("/api/coreprotect/lookup", this::lookup);
    }

    /**
     * Tells the browser which servers can be asked and which lookups exist, so the form builds itself.
     */
    private void options(ApiContext ctx) throws Exception {
        JSONArray servers = new JSONArray();
        // whoever answers on the bus is a server that can be asked - the proxy never answers, it has no
        // world to log, and a server that is still booting is correctly absent
        for (String name : AdminNetwork.respondingServers()) servers.put(name);
        JSONArray kinds = new JSONArray();
        for (LookupQuery.Kind kind : LookupQuery.Kind.values()) {
            kinds.put(new JSONObject().put("id", kind.name()).put("label", labelOf(kind)));
        }
        ctx.ok(new JSONObject().put("servers", servers).put("kinds", kinds));
    }

    /**
     * @param kind the lookup
     * @return what to call it in the interface
     */
    private static String labelOf(LookupQuery.Kind kind) {
        return switch (kind) {
            case BLOCK -> "Blöcke (an einer Position)";
            case CONTAINER -> "Kisten / Container";
            case ITEM -> "Items aufgehoben / fallen gelassen";
            case INVENTORY -> "Inventar-Änderungen";
            case SESSION -> "Logins / Logouts";
            case CHAT -> "Chat";
            case COMMAND -> "Befehle";
            case SIGN -> "Schilder";
            case USERNAME -> "Namensänderungen";
        };
    }

    private void lookup(ApiContext ctx) throws Exception {
        if (!AdminNetwork.isAvailable()) {
            ctx.error(503, "Das Netzwerk ist noch nicht verbunden.");
            return;
        }
        JSONObject body = ctx.body();
        String rawServer = ctx.string("server", "");
        if (rawServer.isEmpty()) {
            ctx.error(400, "Es fehlt der Server, auf dem gesucht werden soll.");
            return;
        }
        ListenerAdapter.ServerName server;
        try {
            server = ListenerAdapter.ServerName.valueOf(rawServer);
        } catch (IllegalArgumentException e) {
            ctx.error(400, "'" + rawServer + "' ist kein gültiger Servername.");
            return;
        }

        LookupQuery query = new LookupQuery();
        try {
            query.setKind(LookupQuery.Kind.valueOf(ctx.string("kind", "BLOCK").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Unbekannte Abfrage-Art.");
            return;
        }
        query.setUser(ctx.string("user", null));
        query.setTimeSeconds(body.optInt("timeSeconds", 3600));
        query.setLimit(body.optInt("limit", 100));
        query.setOffset(body.optInt("offset", 0));

        String world = ctx.string("world", "");
        if (!world.isEmpty()) {
            query.setHasLocation(true);
            query.setWorld(world);
            query.setX(body.optInt("x", 0));
            query.setY(body.optInt("y", 0));
            query.setZ(body.optInt("z", 0));
            query.setRadius(body.optInt("radius", 0));
        }

        List<CoreProtectEntry> entries;
        try {
            entries = AdminNetwork.lookup(server, query);
        } catch (IllegalStateException e) {
            ctx.error(409, String.valueOf(e.getMessage()));
            return;
        }

        JSONArray array = new JSONArray();
        for (CoreProtectEntry entry : entries) {
            array.put(new JSONObject()
                    .put("timestamp", entry.getTimestamp())
                    .put("player", entry.getPlayer() == null ? JSONObject.NULL : entry.getPlayer())
                    .put("action", entry.getAction() == null ? JSONObject.NULL : entry.getAction())
                    .put("target", entry.getTarget() == null ? JSONObject.NULL : entry.getTarget())
                    .put("world", entry.getWorld() == null ? JSONObject.NULL : entry.getWorld())
                    .put("x", entry.getX())
                    .put("y", entry.getY())
                    .put("z", entry.getZ())
                    .put("rolledBack", entry.isRolledBack())
                    .put("detail", entry.getDetail() == null ? JSONObject.NULL : entry.getDetail()));
        }
        ctx.ok("entries", array);
    }
}
