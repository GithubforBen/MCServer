package de.hems.utils.webconsole;

import de.hems.utils.webconsole.auth.Session;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * One request of the admin website, together with the login it belongs to.
 * <p>
 * A thin layer over javalin's {@link Context}: it adds the session, json helpers built on {@code org.json}
 * - which the rest of the launcher already uses - and answers that all have the same shape.
 */
public class ApiContext {

    private final Context ctx;
    private final Session session;
    private final WebServer server;
    private JSONObject body;

    ApiContext(Context ctx, Session session, WebServer server) {
        this.ctx = ctx;
        this.session = session;
        this.server = server;
    }

    /**
     * @return the underlying javalin context, for the rare case something below is not enough
     */
    public Context raw() {
        return ctx;
    }

    /**
     * @return the login this request belongs to, or {@code null} on a route that does not need one
     */
    public Session session() {
        return session;
    }

    public WebServer server() {
        return server;
    }

    /**
     * @param name the placeholder in the route, for example {@code server} in {@code /api/servers/{server}}
     * @return what was in its place
     */
    public String pathParam(String name) {
        return ctx.pathParam(name);
    }

    public String queryParam(String name) {
        return ctx.queryParam(name);
    }

    /**
     * @return the body as json, empty if the request had none
     */
    public JSONObject body() {
        if (body == null) {
            String raw = ctx.body();
            body = raw == null || raw.isBlank() ? new JSONObject() : new JSONObject(raw);
        }
        return body;
    }

    /**
     * @param key      the field to read
     * @param fallback what to use if the field is missing
     * @return the field as a trimmed string
     */
    public String string(String key, String fallback) {
        JSONObject json = body();
        if (!json.has(key) || json.isNull(key)) return fallback;
        return json.get(key).toString().trim();
    }

    /**
     * @param key      the field to read
     * @param fallback what to use if the field is missing or is not a number
     * @return the field as an int
     */
    public int integer(String key, int fallback) {
        return body().optInt(key, fallback);
    }

    /**
     * @return where the request came from, used to keep login grace periods apart
     */
    public String clientKey() {
        String ip = ctx.ip();
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }

    public String header(String name) {
        return ctx.header(name);
    }

    public String cookie(String name) {
        return ctx.cookie(name);
    }

    /* ------------------------------------------------------------------ answers */

    public void ok(JSONObject json) {
        json(json, 200);
    }

    /**
     * Answers with a list, always wrapped in an object so every answer has the same shape.
     *
     * @param key   the field the array goes under
     * @param array what to send
     */
    public void ok(String key, JSONArray array) {
        json(new JSONObject().put(key, array), 200);
    }

    /**
     * Answers a request that worked but has nothing to return.
     *
     * @param message what to show the user
     */
    public void ok(String message) {
        json(new JSONObject().put("ok", true).put("message", message), 200);
    }

    public void error(int code, String message) {
        json(new JSONObject().put("ok", false).put("error", message), code);
    }

    public void json(JSONObject json, int code) {
        ctx.status(code)
                .contentType("application/json; charset=utf-8")
                .result(json.toString());
    }
}
