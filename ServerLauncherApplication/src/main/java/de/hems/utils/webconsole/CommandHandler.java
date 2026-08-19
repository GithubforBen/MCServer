package de.hems.utils.webconsole;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.utils.webconsole.auth.Passwords;
import org.json.JSONObject;

import java.util.List;

/**
 * The endpoint scripts use to run a command on a server, kept for {@code executeCommand.py} and anything
 * else that was built against it.
 * <p>
 * It used to accept the literal secret {@code "67"}, which meant anybody who could reach the port could run
 * commands as the console. The secret now comes from {@code web.command-secret} in the launcher config and
 * is generated on first start.
 */
public class CommandHandler {

    /** Where the secret lives in the launcher config. */
    private static final String SECRET_PATH = "web.command-secret";

    /**
     * @param ctx the request being answered
     */
    public void handle(ApiContext ctx) throws Exception {
        JSONObject json = ctx.body();
        for (String required : new String[]{"command", "server", "secret"}) {
            if (!json.has(required)) {
                ctx.error(400, required + " is missing");
                return;
            }
        }
        if (!Passwords.tokensEqual(secret(), json.getString("secret"))) {
            ctx.error(401, "wrong secret");
            return;
        }
        ListenerAdapter.ServerName name;
        try {
            name = ListenerAdapter.ServerName.valueOf(json.getString("server"));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "'" + json.getString("server") + "' is not a usable server name");
            return;
        }
        var instance = Main.getInstance().getServerHandler().getInstance(name);
        if (instance == null) {
            ctx.error(409, "the server '" + name + "' is not running");
            return;
        }
        instance.executeCommand(json.getString("command"));
        ctx.ok("successfully executed command");
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
                List.of("The secret scripts have to send to POST /command."));
        configuration.save();
        System.out.println("A secret for the /command endpoint was generated: " + generated);
        return generated;
    }
}
