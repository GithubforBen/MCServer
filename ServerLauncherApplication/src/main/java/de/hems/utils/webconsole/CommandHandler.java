package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.utils.webconsole.auth.Passwords;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * The endpoint scripts use to run a command on a server, kept for {@code executeCommand.py} and anything
 * else that was built against it.
 * <p>
 * It used to accept the literal secret {@code "67"}, which meant anybody who could reach the port could run
 * commands as the console. The secret now comes from {@code web.command-secret} in the launcher config and
 * is generated on first start.
 */
public class CommandHandler extends CustomHandler implements HttpHandler {

    /** Where the secret lives in the launcher config. */
    private static final String SECRET_PATH = "web.command-secret";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                methodNotAllowed(exchange);
                return;
            }
            JSONObject json;
            try {
                json = getJSON(exchange);
            } catch (JSONException e) {
                jsonError(exchange);
                return;
            }
            for (String required : new String[]{"command", "server", "secret"}) {
                if (!json.has(required)) {
                    respondDataNotFound(exchange, required + " is missing");
                    return;
                }
            }
            if (!Passwords.tokensEqual(secret(), json.getString("secret"))) {
                respond(exchange, "wrong secret", UNAUTHORIZED);
                return;
            }
            ListenerAdapter.ServerName name;
            try {
                name = ListenerAdapter.ServerName.valueOf(json.getString("server"));
            } catch (IllegalArgumentException e) {
                respond(exchange, "'" + json.getString("server") + "' is not a usable server name", BAD_REQUEST);
                return;
            }
            var instance = Main.getInstance().getServerHandler().getInstance(name);
            if (instance == null) {
                respondDataNotFound(exchange, "the server '" + name + "' is not running");
                return;
            }
            instance.executeCommand(json.getString("command"));
            respond(exchange, "successfully executed command");
        } finally {
            exchange.close();
        }
    }

    /**
     * @return the secret callers have to send, generating and storing one if there is none yet
     */
    private static String secret() {
        var configuration = Main.getInstance().getConfiguration();
        String stored = configuration.getConfig().getString(SECRET_PATH);
        if (stored != null && !stored.isBlank()) return stored;
        String generated = Passwords.randomToken();
        configuration.getConfig().set(SECRET_PATH, generated);
        configuration.getConfig().setComments(SECRET_PATH,
                java.util.List.of("The secret scripts have to send to POST /command."));
        configuration.save();
        System.out.println("A secret for the /command endpoint was generated: " + generated);
        return generated;
    }
}
