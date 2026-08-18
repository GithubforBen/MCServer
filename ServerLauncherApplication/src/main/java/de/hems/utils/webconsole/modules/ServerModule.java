package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
import de.hems.utils.server.ServerHandler;
import de.hems.utils.server.ServerInstance;
import de.hems.utils.webconsole.ApiHandler;
import de.hems.utils.webconsole.ApiRequest;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows which servers of the network are running and lets them be switched on and off.
 */
public class ServerModule implements WebModule {

    /** Where the servers that were created in earlier runs are remembered. */
    private static final String CONFIG_ROOT = "servers";
    /** How long a restart waits for the old process to release its port. */
    private static final long SHUTDOWN_TIMEOUT_MS = 120_000L;

    @Override
    public String getId() {
        return "servers";
    }

    @Override
    public String getTitle() {
        return "Server";
    }

    @Override
    public String getDescription() {
        return "Zeigt an welche Server laufen und schaltet sie an und aus.";
    }

    @Override
    public void register(WebServer server) {
        server.route("/api/servers", new ServerHandlerRoute(server));
    }

    private static class ServerHandlerRoute extends ApiHandler {

        ServerHandlerRoute(WebServer server) {
            super(server, "/api/servers", true);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            String first = request.pathAt(0);
            if (request.isMethod("GET")) {
                if ("templates".equals(first)) {
                    ok(request, "templates", listTemplates());
                    return;
                }
                ok(request, "servers", listServers());
                return;
            }
            if (!request.isMethod("POST")) {
                wrongMethod(request);
                return;
            }
            if (first == null) {
                create(request);
                return;
            }
            String action = request.pathAt(1);
            if (action == null) {
                error(request, BAD_REQUEST, "Es fehlt die Aktion, z.B. /api/servers/" + first + "/start.");
                return;
            }
            switch (action.toLowerCase()) {
                case "start" -> start(request, first);
                case "stop" -> stop(request, first);
                case "restart" -> restart(request, first);
                default -> error(request, BAD_REQUEST, "Unbekannte Aktion '" + action + "'.");
            }
        }

        /**
         * Every server the interface knows about: the ones that run right now and the ones that ran before
         * and can be switched back on.
         *
         * @return the servers, sorted by name
         */
        private JSONArray listServers() {
            ServerHandler handler = Main.getInstance().getServerHandler();
            handler.updateInstances();
            Map<String, JSONObject> byName = new LinkedHashMap<>();

            // everything that was ever started is remembered in the config, so it can be switched on again
            YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
            ConfigurationSection section = config.getConfigurationSection(CONFIG_ROOT);
            if (section != null) {
                for (String name : section.getKeys(false)) {
                    byName.put(name, new JSONObject()
                            .put("name", name)
                            .put("port", section.getInt(name + ".port", ListenerAdapter.ServerName.NO_PORT))
                            .put("memory", section.getInt(name + ".memory", 0))
                            .put("template", section.getString(name + ".template", "EVENT"))
                            .put("software", section.getString(name + ".software", "PAPER"))
                            .put("online", false)
                            .put("starting", false)
                            .put("known", true));
                }
            }

            for (ServerInstance instance : handler.getInstances()) {
                if (instance.getName().isReserved()) continue;
                boolean online;
                try {
                    online = instance.isAlive();
                } catch (IOException e) {
                    online = false;
                }
                byName.put(instance.getName().toString(), new JSONObject()
                        .put("name", instance.getName().toString())
                        .put("port", instance.getName().getPort())
                        .put("memory", instance.getAllocatedMemoryMB())
                        .put("template", instance.getTemplate().name())
                        .put("software", instance.getJarFile().name())
                        .put("online", online)
                        .put("starting", !online && instance.isStarting())
                        .put("known", true));
            }

            List<String> names = new ArrayList<>(byName.keySet());
            names.sort(String::compareTo);
            JSONArray array = new JSONArray();
            for (String name : names) array.put(byName.get(name));
            return array;
        }

        /**
         * @return the blueprints a new server can be created from
         */
        private JSONArray listTemplates() {
            JSONArray array = new JSONArray();
            for (ServerTemplate template : ServerTemplate.values()) {
                if (template == ServerTemplate.PROXY) continue;
                array.put(new JSONObject()
                        .put("name", template.name())
                        .put("software", template.getSoftware().name())
                        .put("defaultMemory", template.getDefaultMemoryMB()));
            }
            return array;
        }

        /**
         * Switches a server on. A server that was created before comes back with the settings it had.
         *
         * @param request the request being answered
         * @param rawName the server to start
         */
        private void start(ApiRequest request, String rawName) throws IOException {
            ListenerAdapter.ServerName name = resolve(rawName);
            if (name == null) {
                error(request, BAD_REQUEST, "'" + rawName + "' ist kein gültiger Servername.");
                return;
            }
            ServerHandler handler = Main.getInstance().getServerHandler();
            if (handler.doesInstanceExist(name)) {
                error(request, CONFLICT, name + " läuft bereits.");
                return;
            }
            YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
            ServerTemplate template = templateOf(config, name);
            int memory = config.getInt(CONFIG_ROOT + "." + name + ".memory", template.getDefaultMemoryMB());
            try {
                handler.startNewInstance(name, template, memory, new FileType.PLUGIN[0]);
            } catch (Exception e) {
                error(request, SERVER_ERROR, "Konnte " + name + " nicht starten: " + e.getMessage());
                return;
            }
            ok(request, name + " wird gestartet.");
        }

        /**
         * Switches a server off.
         *
         * @param request the request being answered
         * @param rawName the server to stop
         */
        private void stop(ApiRequest request, String rawName) throws IOException {
            ListenerAdapter.ServerName name = resolve(rawName);
            if (name == null) {
                error(request, BAD_REQUEST, "'" + rawName + "' ist kein gültiger Servername.");
                return;
            }
            ServerHandler handler = Main.getInstance().getServerHandler();
            if (!handler.doesInstanceExist(name)) {
                error(request, CONFLICT, name + " läuft nicht.");
                return;
            }
            try {
                handler.stop(name);
            } catch (RuntimeException e) {
                error(request, SERVER_ERROR, "Konnte " + name + " nicht stoppen: " + e.getMessage());
                return;
            }
            ok(request, name + " wird gestoppt.");
        }

        /**
         * Stops a server and starts it again once its port is free. The waiting happens in the background,
         * so the browser gets an answer right away.
         *
         * @param request the request being answered
         * @param rawName the server to restart
         */
        private void restart(ApiRequest request, String rawName) throws IOException {
            ListenerAdapter.ServerName name = resolve(rawName);
            if (name == null) {
                error(request, BAD_REQUEST, "'" + rawName + "' ist kein gültiger Servername.");
                return;
            }
            ServerHandler handler = Main.getInstance().getServerHandler();
            if (!handler.doesInstanceExist(name)) {
                error(request, CONFLICT, name + " läuft nicht.");
                return;
            }
            ServerInstance stopped;
            try {
                stopped = handler.stop(name);
            } catch (RuntimeException e) {
                error(request, SERVER_ERROR, "Konnte " + name + " nicht stoppen: " + e.getMessage());
                return;
            }
            Thread restarter = new Thread(() -> {
                long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;
                while (handler.doesInstanceExist(name)) {
                    if (System.currentTimeMillis() > deadline) {
                        System.out.println(name + " did not stop in time - not restarting it.");
                        return;
                    }
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                try {
                    handler.startNewInstance(stopped.getName(), stopped.getAllocatedMemoryMB(),
                            stopped.getJarFile(), stopped.getPlugins(), stopped.getTemplate());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "restart-" + name);
            restarter.setDaemon(true);
            restarter.start();
            ok(request, name + " wird neu gestartet.");
        }

        /**
         * Creates a server that did not exist before.
         *
         * @param request the request being answered
         */
        private void create(ApiRequest request) throws IOException {
            String rawName = request.getString("name", "");
            if (rawName.isEmpty()) {
                error(request, BAD_REQUEST, "Es fehlt der Name des neuen Servers.");
                return;
            }
            ListenerAdapter.ServerName name = resolve(rawName);
            if (name == null) {
                error(request, BAD_REQUEST, "'" + rawName + "' ist kein gültiger Servername.");
                return;
            }
            if (name.isReserved()) {
                error(request, BAD_REQUEST, "'" + name + "' ist für das Netzwerk reserviert.");
                return;
            }
            ServerHandler handler = Main.getInstance().getServerHandler();
            if (handler.doesInstanceExist(name)) {
                error(request, CONFLICT, name + " läuft bereits.");
                return;
            }
            ServerTemplate template;
            try {
                template = ServerTemplate.valueOf(request.getString("template", "EVENT").toUpperCase());
            } catch (IllegalArgumentException e) {
                error(request, BAD_REQUEST, "Unbekanntes Template.");
                return;
            }
            int memory = request.getBody().optInt("memory", template.getDefaultMemoryMB());
            try {
                handler.startNewInstance(name, template, memory, new FileType.PLUGIN[0]);
            } catch (Exception e) {
                error(request, SERVER_ERROR, "Konnte " + name + " nicht erstellen: " + e.getMessage());
                return;
            }
            ok(request, name + " wurde erstellt und wird gestartet.");
        }

        /**
         * @param config the launcher config
         * @param name   the server to look up
         * @return the blueprint that server was created from
         */
        private static ServerTemplate templateOf(YamlConfiguration config, ListenerAdapter.ServerName name) {
            String stored = config.getString(CONFIG_ROOT + "." + name + ".template");
            if (stored != null) {
                try {
                    return ServerTemplate.valueOf(stored);
                } catch (IllegalArgumentException ignored) {
                    // the config holds a template that no longer exists - fall back to the name
                }
            }
            return ServerTemplate.forServerName(name.toString());
        }

        /**
         * @param rawName the name as it arrived from the browser
         * @return the canonical name, or {@code null} if it can not be used as a server name
         */
        private static ListenerAdapter.ServerName resolve(String rawName) {
            try {
                return ListenerAdapter.ServerName.valueOf(rawName);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
