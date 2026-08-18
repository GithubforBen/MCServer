package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.hems.Main;
import de.hems.utils.Configuration;
import de.hems.utils.webconsole.auth.AuthService;
import de.hems.utils.webconsole.modules.AuthModule;
import de.hems.utils.webconsole.modules.ConsoleModule;
import de.hems.utils.webconsole.modules.PayingPlayerModule;
import de.hems.utils.webconsole.modules.ServerModule;
import de.hems.utils.webconsole.modules.StaticModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The admin website of the network.
 * <p>
 * The server itself knows nothing about what is administrated - it only holds the login, the routes and the
 * list of {@link WebModule}s. Everything that can be managed is one of those modules, so the interface
 * grows by writing a module and adding it in {@link #loadModules()}; both the routes and the navigation of
 * the page follow from that automatically.
 */
public class WebServer {

    /** How long a request may take before the server gives up on it while shutting down. */
    private static final int SHUTDOWN_DELAY_SECONDS = 2;

    private final Configuration configuration;
    private final HttpServer server;
    private final AuthService authService;
    private final List<WebModule> modules = new ArrayList<>();
    private final ExecutorService executor;
    private final int port;

    public WebServer() throws IOException {
        this(Main.getInstance().getConfiguration());
    }

    /**
     * @param configuration the config the website reads its settings and accounts from
     */
    public WebServer(Configuration configuration) throws IOException {
        this.configuration = configuration;
        YamlConfiguration config = configuration.getConfig();
        this.port = config.getInt("web.port", 8080);
        String bind = config.getString("web.bind", "0.0.0.0");
        this.authService = new AuthService(configuration);
        this.authService.ensureAccountExists();

        this.server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        // a login is deliberately slow because of its grace period, so requests must not share one thread
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "web-console");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);

        loadModules();
        server.createContext("/command", new CommandHandler());
        server.start();
        System.out.println("The admin website is available on http://" + bind + ":" + server.getAddress().getPort() + "/");
    }

    /**
     * Registers the modules that make up the interface. This is the one place a new panel has to be added.
     */
    private void loadModules() {
        add(new AuthModule());
        add(new ServerModule());
        add(new PayingPlayerModule());
        add(new ConsoleModule());
        // the static files answer everything that is not an api route, so they come last
        add(new StaticModule());
    }

    /**
     * Adds a module and lets it register its routes.
     *
     * @param module the module to add
     */
    public void add(WebModule module) {
        modules.add(module);
        module.register(this);
    }

    /**
     * Registers a route. The path is matched by prefix, so {@code /api/servers} also receives
     * {@code /api/servers/LOBBY/start}.
     *
     * @param path    where the route lives
     * @param handler what answers it
     */
    public void route(String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            addSecurityHeaders(exchange);
            handler.handle(exchange);
        });
    }

    /**
     * Headers that hold for every answer: the page never embeds anything from elsewhere and is never framed
     * by another site.
     *
     * @param exchange the request being answered
     */
    private static void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-ancestors 'none'");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }

    /**
     * @return the modules of the interface, in the order they were registered
     */
    public List<WebModule> getModules() {
        return List.copyOf(modules);
    }

    public AuthService getAuthService() {
        return authService;
    }

    /**
     * @return the config of the launcher, so modules can read their own settings from it
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return the port the website actually listens on
     */
    public int getBoundPort() {
        return server.getAddress().getPort();
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        server.stop(SHUTDOWN_DELAY_SECONDS);
        executor.shutdownNow();
    }
}
