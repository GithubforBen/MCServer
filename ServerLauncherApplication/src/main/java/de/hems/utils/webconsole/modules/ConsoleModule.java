package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.utils.server.ServerInstance;
import de.hems.utils.server.console.ConsoleBuffer;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import io.javalin.websocket.WsContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The console of a server: the recent output, a live stream of everything that follows, and a way to send
 * a command.
 * <p>
 * The stream is a websocket. The servers themselves run inside tmux, so their output is picked up by
 * {@link de.hems.utils.server.console.ConsoleTailer} and kept in a {@link ConsoleBuffer} per server; this
 * module only forwards what lands there.
 */
public class ConsoleModule implements WebModule {

    /** The open viewers, so their subscription can be dropped again when they disconnect. */
    private final Map<WsContext, ConsoleBuffer.Subscription> viewers = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "console";
    }

    @Override
    public String getTitle() {
        return "Konsole";
    }

    @Override
    public String getDescription() {
        return "Zeigt die Ausgabe eines Servers live an und schickt Befehle an ihn.";
    }

    @Override
    public void register(WebServer server) {
        server.post("/api/console", this::send);
        server.get("/api/console/{server}", this::history);
        server.authenticatedWs("/api/console/stream", ws -> {
            ws.onConnect(this::onConnect);
            ws.onClose(ctx -> unsubscribe(ctx));
            ws.onError(ctx -> unsubscribe(ctx));
        });
    }

    /* ------------------------------------------------------------------ live stream */

    /**
     * Hooks a new viewer up to the console of the server it asked for.
     *
     * @param ws the socket that was just opened
     */
    private void onConnect(WsContext ws) {
        ServerInstance instance = instanceOf(ws.queryParam("server"));
        if (instance == null) {
            ws.send(new JSONObject()
                    .put("type", "error")
                    .put("message", "Dieser Server läuft nicht.")
                    .toString());
            ws.closeSession(1008, "server not running");
            return;
        }
        ws.enableAutomaticPings();

        // the subscription carries the history as of the moment it was made, so no line is lost or doubled
        ConsoleBuffer.Subscription subscription = instance.getConsole().subscribe(line -> sendLine(ws, line));
        viewers.put(ws, subscription);

        JSONArray history = new JSONArray();
        subscription.getHistory().forEach(history::put);
        ws.send(new JSONObject()
                .put("type", "history")
                .put("server", instance.getName().toString())
                .put("lines", history)
                .toString());
    }

    /**
     * @param ws   the viewer to send to
     * @param line the line that was just written
     */
    private void sendLine(WsContext ws, String line) {
        try {
            ws.send(new JSONObject().put("type", "line").put("line", line).toString());
        } catch (Exception e) {
            // the socket died without telling us - drop it so the buffer stops holding on to it
            unsubscribe(ws);
        }
    }

    /**
     * @param ws the viewer that went away
     */
    private void unsubscribe(WsContext ws) {
        ConsoleBuffer.Subscription subscription = viewers.remove(ws);
        if (subscription != null) subscription.close();
    }

    /* ------------------------------------------------------------------ http */

    /**
     * The recent output, for a browser that can not use the websocket.
     *
     * @param ctx the request being answered
     */
    private void history(ApiContext ctx) {
        ServerInstance instance = instanceOf(ctx.pathParam("server"));
        if (instance == null) {
            ctx.error(409, "Dieser Server läuft nicht.");
            return;
        }
        JSONArray lines = new JSONArray();
        instance.getConsole().history().forEach(lines::put);
        ctx.ok(new JSONObject()
                .put("ok", true)
                .put("server", instance.getName().toString())
                .put("lines", lines));
    }

    /**
     * Sends a command to the console of a running server.
     *
     * @param ctx the request being answered
     */
    private void send(ApiContext ctx) throws Exception {
        String rawName = ctx.string("server", "");
        String command = ctx.string("command", "");
        if (rawName.isEmpty() || command.isEmpty()) {
            ctx.error(400, "Es fehlt der Server oder der Befehl.");
            return;
        }
        ServerInstance instance = instanceOf(rawName);
        if (instance == null) {
            ctx.error(409, "Dieser Server läuft nicht.");
            return;
        }
        instance.executeCommand(command);
        ctx.ok("'" + command + "' wurde an " + instance.getName() + " geschickt.");
    }

    /**
     * @param rawName the server name as it arrived from the browser
     * @return the running server with that name, or {@code null} if there is none
     */
    private static ServerInstance instanceOf(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;
        Main main = Main.getInstance();
        // the website can be up before the launcher finished wiring itself together
        if (main == null || main.getServerHandler() == null) return null;
        ListenerAdapter.ServerName name;
        try {
            name = ListenerAdapter.ServerName.valueOf(rawName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return main.getServerHandler().getInstance(name);
    }
}
