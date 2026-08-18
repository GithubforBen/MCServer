package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.utils.server.ServerInstance;
import de.hems.utils.webconsole.ApiHandler;
import de.hems.utils.webconsole.ApiRequest;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;

import java.io.IOException;

/**
 * Sends a command to the console of a running server.
 */
public class ConsoleModule implements WebModule {

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
        return "Schickt einen Befehl an die Konsole eines laufenden Servers.";
    }

    @Override
    public void register(WebServer server) {
        server.route("/api/console", new ConsoleRoute(server));
    }

    private static class ConsoleRoute extends ApiHandler {

        ConsoleRoute(WebServer server) {
            super(server, "/api/console", true);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            if (!request.isMethod("POST")) {
                wrongMethod(request);
                return;
            }
            String rawName = request.getString("server", "");
            String command = request.getString("command", "");
            if (rawName.isEmpty() || command.isEmpty()) {
                error(request, BAD_REQUEST, "Es fehlt der Server oder der Befehl.");
                return;
            }
            ListenerAdapter.ServerName name;
            try {
                name = ListenerAdapter.ServerName.valueOf(rawName);
            } catch (IllegalArgumentException e) {
                error(request, BAD_REQUEST, "'" + rawName + "' ist kein gültiger Servername.");
                return;
            }
            ServerInstance instance = Main.getInstance().getServerHandler().getInstance(name);
            if (instance == null) {
                error(request, CONFLICT, name + " läuft nicht.");
                return;
            }
            instance.executeCommand(command);
            ok(request, "'" + command + "' wurde an " + name + " geschickt.");
        }
    }
}
