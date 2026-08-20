package de.hems.utils.webconsole;

import de.hems.Main;
import de.hems.utils.Configuration;
import de.hems.utils.webconsole.auth.AuthService;
import de.hems.utils.webconsole.auth.Passwords;
import de.hems.utils.webconsole.auth.Session;
import de.hems.utils.webconsole.modules.AuthModule;
import de.hems.utils.webconsole.modules.ConsoleModule;
import de.hems.utils.webconsole.modules.CoreProtectModule;
import de.hems.utils.webconsole.modules.EventModule;
import de.hems.utils.webconsole.modules.PayingPlayerModule;
import de.hems.utils.webconsole.modules.PlayerModule;
import de.hems.utils.webconsole.modules.ServerModule;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The admin website of the network.
 * <p>
 * The server itself knows nothing about what is administrated - it only holds the login, the routes and the
 * list of {@link WebModule}s. Everything that can be managed is one of those modules, so the interface
 * grows by writing a module and adding it in {@link #loadModules()}; both the routes and the navigation of
 * the page follow from that automatically.
 * <p>
 * It runs on javalin, which brings the websockets the live console view needs.
 */
public class WebServer {

    /** The cookie the session token is kept in. */
    public static final String SESSION_COOKIE = "mcadmin_session";
    /** The header a changing request has to repeat the csrf token in. */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final Configuration configuration;
    private final AuthService authService;
    private final List<WebModule> modules = new ArrayList<>();
    private final Javalin app;

    /** Only set while the modules register, since javalin takes its routes at creation time. */
    private RoutesConfig routes;

    public WebServer() {
        this(Main.getInstance().getConfiguration());
    }

    /**
     * @param configuration the config the website reads its settings and accounts from
     * @param extraModules  modules registered on top of the built in ones, so the interface can be
     *                      extended without editing {@link #loadModules()}
     */
    public WebServer(Configuration configuration, WebModule... extraModules) {
        this.configuration = configuration;
        YamlConfiguration config = configuration.getConfig();
        int port = config.getInt("web.port", 8080);
        String bind = config.getString("web.bind", "0.0.0.0");
        this.authService = new AuthService(configuration);
        this.authService.ensureAccountExists();

        this.app = Javalin.create(cfg -> {
            cfg.startup.showJavalinBanner = false;
            cfg.startup.showOldJavalinVersionWarning = false;
            cfg.staticFiles.add("/web", Location.CLASSPATH);
            this.routes = cfg.routes;
            try {
                cfg.routes.after(WebServer::addSecurityHeaders);
                cfg.routes.exception(JSONException.class, (e, ctx) ->
                        fail(ctx, 400, "Der Request ist kein gültiges JSON."));
                cfg.routes.exception(IllegalArgumentException.class, (e, ctx) ->
                        fail(ctx, 400, String.valueOf(e.getMessage())));
                cfg.routes.exception(Exception.class, (e, ctx) -> {
                    e.printStackTrace();
                    fail(ctx, 500, "Unerwarteter Fehler: " + e.getMessage());
                });
                loadModules();
                for (WebModule module : extraModules) add(module);
                publicPost("/command", new CommandHandler()::handle);
            } finally {
                this.routes = null;
            }
        });
        app.start(bind, port);
        System.out.println("The admin website is available on http://" + bind + ":" + app.port() + "/");
    }

    /**
     * Registers the modules that make up the interface. This is the one place a new panel has to be added.
     */
    private void loadModules() {
        add(new AuthModule());
        add(new ServerModule());
        add(new EventModule());
        add(new PlayerModule());
        add(new CoreProtectModule());
        add(new PayingPlayerModule());
        add(new ConsoleModule());
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

    /* ------------------------------------------------------------------ routes */

    /** The methods the routes of the interface use. */
    private enum Method {
        GET, POST, DELETE
    }

    /** A route that needs a login. */
    public void get(String path, ApiHandler handler) {
        route(Method.GET, path, handler, true);
    }

    public void post(String path, ApiHandler handler) {
        route(Method.POST, path, handler, true);
    }

    public void delete(String path, ApiHandler handler) {
        route(Method.DELETE, path, handler, true);
    }

    /** A route that anybody may call, for the login itself. */
    public void publicGet(String path, ApiHandler handler) {
        route(Method.GET, path, handler, false);
    }

    public void publicPost(String path, ApiHandler handler) {
        route(Method.POST, path, handler, false);
    }

    private void route(Method method, String path, ApiHandler handler, boolean requiresAuthentication) {
        RoutesConfig target = requireRegistering();
        io.javalin.http.Handler wrapped = ctx -> {
            Session session = resolveSession(ctx.cookie(SESSION_COOKIE), ctx.header("Authorization"));
            if (requiresAuthentication && session == null) {
                fail(ctx, 401, "Nicht angemeldet.");
                return;
            }
            // a cookie alone must not be enough to change something, or another site could do it for you
            if (session != null && method != Method.GET
                    && !Passwords.tokensEqual(session.getCsrfToken(), ctx.header(CSRF_HEADER))) {
                fail(ctx, 403, "Der CSRF-Token fehlt oder stimmt nicht.");
                return;
            }
            handler.handle(new ApiContext(ctx, session, this));
        };
        switch (method) {
            case GET -> target.get(path, wrapped);
            case POST -> target.post(path, wrapped);
            case DELETE -> target.delete(path, wrapped);
        }
    }

    /**
     * Registers a websocket that only a logged in admin may open.
     * <p>
     * The login is checked on the upgrade request, before the socket exists at all. On top of that the
     * {@code Origin} has to match, because a websocket handshake is not stopped by the same origin policy
     * and the session cookie would otherwise be sent along by any page the admin happens to visit.
     *
     * @param path   where the socket lives
     * @param config what to do with it
     */
    public void authenticatedWs(String path, Consumer<WsConfig> config) {
        RoutesConfig target = requireRegistering();
        target.wsBeforeUpgrade(path, ctx -> {
            if (resolveSession(ctx.cookie(SESSION_COOKIE), ctx.header("Authorization")) == null) {
                throw new UnauthorizedResponse("Nicht angemeldet.");
            }
            if (!isSameOrigin(ctx)) {
                throw new UnauthorizedResponse("Die Herkunft der Verbindung stimmt nicht.");
            }
        });
        target.ws(path, config);
    }

    /**
     * @param ctx the upgrade request
     * @return whether it came from the page this server itself serves
     */
    private static boolean isSameOrigin(Context ctx) {
        String origin = ctx.header("Origin");
        // a browser always sends it for a websocket; anything else is a script talking to us directly
        if (origin == null || origin.isBlank()) return true;
        String host = ctx.header("Host");
        if (host == null) return false;
        try {
            URI uri = URI.create(origin);
            String originHost = uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
            return host.equalsIgnoreCase(originHost);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @param ws the socket to look up
     * @return the login it was opened with, or {@code null} if it has run out meanwhile
     */
    public Session sessionOf(WsContext ws) {
        return resolveSession(ws.cookie(SESSION_COOKIE), ws.header("Authorization"));
    }

    private Session resolveSession(String cookie, String authorization) {
        String token = cookie;
        if (token == null && authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7).trim();
        }
        return authService.getSession(token);
    }

    private RoutesConfig requireRegistering() {
        if (routes == null) {
            throw new IllegalStateException("Routes can only be added while the web server is being created");
        }
        return routes;
    }

    /* ------------------------------------------------------------------ plumbing */

    private static void fail(Context ctx, int code, String message) {
        ctx.status(code)
                .contentType("application/json; charset=utf-8")
                .result(new JSONObject().put("ok", false).put("error", message).toString());
    }

    /**
     * Headers that hold for every answer: the page never embeds anything from elsewhere and is never framed
     * by another site.
     *
     * @param ctx the request being answered
     */
    private static void addSecurityHeaders(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");
        ctx.header("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                        + "font-src 'self'; connect-src 'self'; frame-ancestors 'none'");
        ctx.header("Cache-Control", "no-store");
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
        return app.port();
    }

    public void stop() {
        app.stop();
    }
}
