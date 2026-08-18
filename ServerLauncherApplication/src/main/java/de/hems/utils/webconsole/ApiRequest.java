package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import de.hems.utils.webconsole.auth.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a handler needs about one incoming request, already unpacked.
 */
public class ApiRequest {

    private final HttpExchange exchange;
    private final String method;
    /** The parts of the path that come after the route the handler was registered on. */
    private final List<String> path;
    private final Map<String, String> query;
    private final Session session;
    private JSONObject body;

    ApiRequest(HttpExchange exchange, String routePrefix, Session session) {
        this.exchange = exchange;
        this.method = exchange.getRequestMethod().toUpperCase();
        this.session = session;
        this.path = splitPath(exchange.getRequestURI().getPath(), routePrefix);
        this.query = parseQuery(exchange.getRequestURI().getRawQuery());
    }

    private static List<String> splitPath(String fullPath, String routePrefix) {
        String rest = fullPath.startsWith(routePrefix) ? fullPath.substring(routePrefix.length()) : fullPath;
        List<String> parts = new ArrayList<>();
        for (String part : rest.split("/")) {
            if (part.isEmpty()) continue;
            parts.add(URLDecoder.decode(part, StandardCharsets.UTF_8));
        }
        return List.copyOf(parts);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> parsed = new HashMap<>();
        if (raw == null || raw.isEmpty()) return parsed;
        for (String pair : raw.split("&")) {
            int index = pair.indexOf('=');
            if (index < 0) {
                parsed.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            } else {
                parsed.put(URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8));
            }
        }
        return parsed;
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public String getMethod() {
        return method;
    }

    public boolean isMethod(String... methods) {
        for (String candidate : methods) {
            if (method.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }

    /**
     * @return the path parts below the route, so {@code /api/servers/LOBBY/start} on route
     * {@code /api/servers} gives {@code ["LOBBY", "start"]}
     */
    public List<String> getPath() {
        return path;
    }

    /**
     * @param index which part to read
     * @return that part of the path, or {@code null} if the path is shorter
     */
    public String pathAt(int index) {
        return index < path.size() ? path.get(index) : null;
    }

    public String getQuery(String key) {
        return query.get(key);
    }

    /**
     * @return the login this request belongs to, or {@code null} if there is none
     */
    public Session getSession() {
        return session;
    }

    /**
     * Reads the body as json. The body is read once and remembered, so a handler may ask for it repeatedly.
     *
     * @return the body, empty if there was none
     */
    public JSONObject getBody() throws IOException {
        if (body == null) {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            String raw = new String(bytes, StandardCharsets.UTF_8);
            body = raw.isBlank() ? new JSONObject() : new JSONObject(raw);
        }
        return body;
    }

    /**
     * @param key      the field to read
     * @param fallback what to use if the field is missing
     * @return the field as a trimmed string
     */
    public String getString(String key, String fallback) throws IOException {
        JSONObject json = getBody();
        if (!json.has(key) || json.isNull(key)) return fallback;
        return json.get(key).toString().trim();
    }

    /**
     * @return where the request came from, used to keep login grace periods apart
     */
    public String getClientKey() {
        if (exchange.getRemoteAddress() == null || exchange.getRemoteAddress().getAddress() == null) {
            return "unknown";
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    /**
     * @param name the cookie to read
     * @return its value, or {@code null} if it was not sent
     */
    public String getCookie(String name) {
        List<String> headers = exchange.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String header : headers) {
            for (String cookie : header.split(";")) {
                String trimmed = cookie.trim();
                int index = trimmed.indexOf('=');
                if (index > 0 && trimmed.substring(0, index).equals(name)) {
                    return trimmed.substring(index + 1);
                }
            }
        }
        return null;
    }

    public String getHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }
}
