package de.hems.utils.webconsole.modules;

import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import de.hems.utils.webconsole.auth.LoginResult;
import de.hems.utils.webconsole.auth.Session;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Logging in and out, and telling the frontend which modules exist.
 */
public class AuthModule implements WebModule {

    @Override
    public String getId() {
        return "auth";
    }

    @Override
    public String getTitle() {
        return "Anmeldung";
    }

    @Override
    public boolean isVisible() {
        // the login is not a panel you navigate to
        return false;
    }

    @Override
    public void register(WebServer server) {
        server.publicPost("/api/login", this::login);
        server.publicGet("/api/session", this::session);
        server.post("/api/logout", this::logout);
        server.get("/api/modules", this::modules);
    }

    /**
     * Takes a user name, a password and a code from the authenticator app.
     * <p>
     * The order matters and is enforced in {@link de.hems.utils.webconsole.auth.AuthService}: without a
     * valid code the password is never compared, and every attempt - successful or not - takes the full
     * grace period before it is answered.
     *
     * @param ctx the request being answered
     */
    private void login(ApiContext ctx) {
        LoginResult result = ctx.server().getAuthService().login(
                ctx.string("username", ""),
                ctx.string("password", ""),
                ctx.string("token", ""),
                ctx.clientKey());

        if (!result.getStatus().isSuccess()) {
            JSONObject json = new JSONObject()
                    .put("ok", false)
                    .put("status", result.getStatus().name())
                    .put("error", result.getMessage());
            if (result.getRetryAfterSeconds() > 0L) {
                json.put("retryAfter", result.getRetryAfterSeconds());
                ctx.raw().header("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            }
            ctx.json(json, result.getStatus().getHttpStatus());
            return;
        }

        Session session = result.getSession();
        boolean secure = ctx.server().getConfiguration().getConfig().getBoolean("web.secure-cookie", false);
        Cookie cookie = new Cookie(WebServer.SESSION_COOKIE, session.getToken(), "/",
                (int) ctx.server().getAuthService().getSessionTimeoutSeconds(),
                secure, true, "", SameSite.STRICT);
        ctx.raw().cookie(cookie);
        ctx.ok(describe(session, ctx.server()));
    }

    /**
     * Ends a session.
     *
     * @param ctx the request being answered
     */
    private void logout(ApiContext ctx) {
        ctx.server().getAuthService().logout(ctx.session().getToken());
        ctx.raw().removeCookie(WebServer.SESSION_COOKIE, "/");
        ctx.ok("Abgemeldet.");
    }

    /**
     * Tells the page whether it is logged in, and hands out the csrf token that every changing request has
     * to repeat.
     *
     * @param ctx the request being answered
     */
    private void session(ApiContext ctx) {
        if (ctx.session() == null) {
            ctx.ok(new JSONObject()
                    .put("authenticated", false)
                    .put("brand", brandOf(ctx.server()))
                    .put("graceSeconds", ctx.server().getAuthService().getGraceSeconds()));
            return;
        }
        ctx.ok(describe(ctx.session(), ctx.server()));
    }

    /**
     * The list the navigation of the website is built from. A newly registered {@link WebModule} shows up
     * here, and therefore in the browser, without the page needing a change.
     *
     * @param ctx the request being answered
     */
    private void modules(ApiContext ctx) {
        JSONArray array = new JSONArray();
        for (WebModule module : ctx.server().getModules()) {
            if (!module.isVisible()) continue;
            array.put(new JSONObject()
                    .put("id", module.getId())
                    .put("title", module.getTitle())
                    .put("description", module.getDescription()));
        }
        ctx.ok("modules", array);
    }

    /**
     * The name shown at the head of the navigation. Configurable, because a network usually has a name of
     * its own and a placeholder is the one thing on the page nobody else can fix.
     *
     * @param server the server holding the config
     * @return the name to show
     */
    private static String brandOf(WebServer server) {
        return server.getConfiguration().getConfig().getString("web.brand", "MCServer");
    }

    /**
     * @param session the session to describe
     * @param server  the server the session belongs to
     * @return what the frontend needs to know about a login
     */
    private static JSONObject describe(Session session, WebServer server) {
        return new JSONObject()
                .put("authenticated", true)
                .put("brand", brandOf(server))
                .put("username", session.getUsername())
                .put("csrfToken", session.getCsrfToken())
                .put("expiresIn", (session.getExpiresAt() - System.currentTimeMillis()) / 1000L)
                .put("graceSeconds", server.getAuthService().getGraceSeconds());
    }
}
