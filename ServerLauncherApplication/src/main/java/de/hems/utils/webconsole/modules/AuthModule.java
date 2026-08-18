package de.hems.utils.webconsole.modules;

import de.hems.utils.webconsole.ApiHandler;
import de.hems.utils.webconsole.ApiRequest;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import de.hems.utils.webconsole.auth.LoginResult;
import de.hems.utils.webconsole.auth.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

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
        server.route("/api/login", new LoginHandler(server));
        server.route("/api/logout", new LogoutHandler(server));
        server.route("/api/session", new SessionHandler(server));
        server.route("/api/modules", new ModulesHandler(server));
    }

    /**
     * Takes a user name, a password and a code from the authenticator app.
     * <p>
     * The order matters and is enforced in {@link de.hems.utils.webconsole.auth.AuthService}: without a
     * valid code the password is never compared, and every attempt - successful or not - takes the full
     * grace period before it is answered.
     */
    private static class LoginHandler extends ApiHandler {

        LoginHandler(WebServer server) {
            super(server, "/api/login", false);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            if (!request.isMethod("POST")) {
                wrongMethod(request);
                return;
            }
            LoginResult result = getServer().getAuthService().login(
                    request.getString("username", ""),
                    request.getString("password", ""),
                    request.getString("token", ""),
                    request.getClientKey());

            if (!result.getStatus().isSuccess()) {
                JSONObject json = new JSONObject()
                        .put("ok", false)
                        .put("status", result.getStatus().name())
                        .put("error", result.getMessage());
                if (result.getRetryAfterSeconds() > 0L) {
                    json.put("retryAfter", result.getRetryAfterSeconds());
                    request.getExchange().getResponseHeaders()
                            .set("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
                }
                respondJson(request.getExchange(), json, result.getStatus().getHttpStatus());
                return;
            }

            Session session = result.getSession();
            setSessionCookie(request, session);
            ok(request, describe(session, getServer()));
        }

        private void setSessionCookie(ApiRequest request, Session session) {
            boolean secure = getServer().getConfiguration().getConfig()
                    .getBoolean("web.secure-cookie", false);
            long maxAge = getServer().getAuthService().getSessionTimeoutSeconds();
            request.getExchange().getResponseHeaders().add("Set-Cookie",
                    SESSION_COOKIE + "=" + session.getToken()
                            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAge
                            + (secure ? "; Secure" : ""));
        }
    }

    /**
     * Ends a session.
     */
    private static class LogoutHandler extends ApiHandler {

        LogoutHandler(WebServer server) {
            super(server, "/api/logout", true);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            if (!request.isMethod("POST")) {
                wrongMethod(request);
                return;
            }
            getServer().getAuthService().logout(request.getSession().getToken());
            request.getExchange().getResponseHeaders().add("Set-Cookie",
                    SESSION_COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
            ok(request, "Abgemeldet.");
        }
    }

    /**
     * Tells the page whether it is logged in, and hands out the csrf token that every changing request has
     * to repeat.
     */
    private static class SessionHandler extends ApiHandler {

        SessionHandler(WebServer server) {
            super(server, "/api/session", false);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            if (!request.isMethod("GET")) {
                wrongMethod(request);
                return;
            }
            if (request.getSession() == null) {
                JSONObject json = new JSONObject()
                        .put("authenticated", false)
                        .put("graceSeconds", getServer().getAuthService().getGraceSeconds());
                ok(request, json);
                return;
            }
            ok(request, describe(request.getSession(), getServer()));
        }
    }

    /**
     * The list the navigation of the website is built from. A newly registered {@link WebModule} shows up
     * here, and therefore in the browser, without the page needing a change.
     */
    private static class ModulesHandler extends ApiHandler {

        ModulesHandler(WebServer server) {
            super(server, "/api/modules", true);
        }

        @Override
        protected void handleRequest(ApiRequest request) throws IOException {
            if (!request.isMethod("GET")) {
                wrongMethod(request);
                return;
            }
            JSONArray array = new JSONArray();
            for (var module : getServer().getModules()) {
                if (!module.isVisible()) continue;
                array.put(new JSONObject()
                        .put("id", module.getId())
                        .put("title", module.getTitle())
                        .put("description", module.getDescription()));
            }
            ok(request, "modules", array);
        }
    }

    /**
     * @param session the session to describe
     * @param server  the server the session belongs to
     * @return what the frontend needs to know about a login
     */
    private static JSONObject describe(Session session, WebServer server) {
        return new JSONObject()
                .put("authenticated", true)
                .put("username", session.getUsername())
                .put("csrfToken", session.getCsrfToken())
                .put("expiresIn", (session.getExpiresAt() - System.currentTimeMillis()) / 1000L)
                .put("graceSeconds", server.getAuthService().getGraceSeconds());
    }
}
