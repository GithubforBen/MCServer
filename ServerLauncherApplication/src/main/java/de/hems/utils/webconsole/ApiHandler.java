package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.hems.utils.webconsole.auth.Passwords;
import de.hems.utils.webconsole.auth.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * The base of every route of the admin website.
 * <p>
 * It takes care of the parts that are the same everywhere - checking the login, refusing requests that
 * would change something without a csrf token, turning exceptions into an answer instead of a dropped
 * connection - so a module only has to describe what it does.
 */
public abstract class ApiHandler extends CustomHandler implements HttpHandler {

    /** The cookie the session token is kept in. */
    public static final String SESSION_COOKIE = "mcadmin_session";
    /** The header a request has to repeat the csrf token in. */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final WebServer server;
    private final String route;
    private final boolean requiresAuthentication;

    protected ApiHandler(WebServer server, String route, boolean requiresAuthentication) {
        this.server = server;
        this.route = route;
        this.requiresAuthentication = requiresAuthentication;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            Session session = resolveSession(exchange);
            ApiRequest request = new ApiRequest(exchange, route, session);

            if (request.isMethod("OPTIONS")) {
                respond(exchange, "", NO_CONTENT);
                return;
            }
            if (requiresAuthentication && session == null) {
                error(exchange, UNAUTHORIZED, "Nicht angemeldet.");
                return;
            }
            // a cookie alone must not be enough to change something, or another site could do it for you
            if (session != null && !request.isMethod("GET", "HEAD")
                    && !Passwords.tokensEqual(session.getCsrfToken(), request.getHeader(CSRF_HEADER))) {
                error(exchange, FORBIDDEN, "Der CSRF-Token fehlt oder stimmt nicht.");
                return;
            }
            handleRequest(request);
        } catch (JSONException e) {
            error(exchange, BAD_REQUEST, "Der Request ist kein gültiges JSON.");
        } catch (IllegalArgumentException e) {
            error(exchange, BAD_REQUEST, String.valueOf(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            error(exchange, SERVER_ERROR, "Unerwarteter Fehler: " + e.getMessage());
        }
    }

    /**
     * Answers one request. Everything thrown out of here becomes a proper error answer.
     *
     * @param request the request, already unpacked
     */
    protected abstract void handleRequest(ApiRequest request) throws IOException;

    private Session resolveSession(HttpExchange exchange) {
        ApiRequest bare = new ApiRequest(exchange, route, null);
        String token = bare.getCookie(SESSION_COOKIE);
        if (token == null) {
            String header = bare.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = header.substring(7).trim();
            }
        }
        return server.getAuthService().getSession(token);
    }

    protected WebServer getServer() {
        return server;
    }

    /**
     * Answers with a json object.
     *
     * @param request the request being answered
     * @param json    what to send
     */
    protected void ok(ApiRequest request, JSONObject json) throws IOException {
        respondJson(request.getExchange(), json, OK);
    }

    /**
     * Answers with a json array wrapped in an object, so the shape stays the same everywhere.
     *
     * @param request the request being answered
     * @param key     the field the array is put under
     * @param array   what to send
     */
    protected void ok(ApiRequest request, String key, JSONArray array) throws IOException {
        respondJson(request.getExchange(), new JSONObject().put(key, array), OK);
    }

    /**
     * Answers a request that worked but has nothing to return.
     *
     * @param request the request being answered
     * @param message what to show the user
     */
    protected void ok(ApiRequest request, String message) throws IOException {
        respondJson(request.getExchange(), new JSONObject().put("ok", true).put("message", message), OK);
    }

    protected void error(ApiRequest request, int code, String message) throws IOException {
        error(request.getExchange(), code, message);
    }

    protected void error(HttpExchange exchange, int code, String message) throws IOException {
        respondJson(exchange, new JSONObject().put("ok", false).put("error", message), code);
    }

    /**
     * Refuses a request that used a method this route does not know.
     *
     * @param request the request being answered
     */
    protected void wrongMethod(ApiRequest request) throws IOException {
        error(request, METHOD_NOT_ALLOWED, request.getMethod() + " ist hier nicht erlaubt.");
    }
}
