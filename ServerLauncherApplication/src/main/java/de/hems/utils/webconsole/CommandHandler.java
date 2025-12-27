package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

public class CommandHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            methodNotAllowed(exchange);
            return;
        }
        JSONObject json = getJSON(exchange);
        if (json == null) {
            jsonError(exchange);
            return;
        }
        if (!json.has("command")) {
            respondDataNotFound(exchange, "command is missing");
            return;
        }
        if (!json.has("server")) {
            respondDataNotFound(exchange, "server is missing");
            return;
        }
        if (!json.has("secret")) {
            respondDataNotFound(exchange, "secret is missing");
            return;
        }
        if (!json.getString("secret").equals("67")) {
            respond(exchange, "wrong secret");
            return;
        }
        Main.getInstance().getServerHandler().getInstance(ListenerAdapter.ServerName.valueOf(json.getString("server"))).executeCommand(json.getString("command"));
        respond(exchange, "successfully executed command");
    }
}
