package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The bits every handler of the web console needs: writing an answer and reading a body.
 */
public class CustomHandler {
    // Standard HTTP status codes
    protected static final int OK = 200;
    protected static final int NO_CONTENT = 204;
    protected static final int BAD_REQUEST = 400;
    protected static final int UNAUTHORIZED = 401;
    protected static final int FORBIDDEN = 403;
    protected static final int NOT_FOUND = 404;
    protected static final int METHOD_NOT_ALLOWED = 405;
    protected static final int CONFLICT = 409;
    protected static final int JSON_ERROR = 410;
    protected static final int TOO_MANY_REQUESTS = 429;
    protected static final int SERVER_ERROR = 500;
    protected static final int DATAT_NOT_FOUND = 501;

    protected void respond(HttpExchange exchange, String answer) throws IOException {
        respond(exchange, answer, OK);
    }

    public void respondDataNotFound(HttpExchange exchange, String data) throws IOException {
        respond(exchange, data, DATAT_NOT_FOUND);
    }

    protected void respondJson(HttpExchange exchange, JSONObject json, int code) throws IOException {
        respond(exchange, json.toString(), code, "application/json; charset=utf-8");
    }

    protected void respond(HttpExchange exchange, String answer, int code) throws IOException {
        respond(exchange, answer, code, "text/plain; charset=utf-8");
    }

    /**
     * Writes the answer and closes the exchange.
     *
     * @param exchange    the request being answered
     * @param answer      the body
     * @param code        the status code
     * @param contentType what the body is
     */
    protected void respond(HttpExchange exchange, String answer, int code, String contentType) throws IOException {
        byte[] bytes = answer == null ? new byte[0] : answer.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(bytes);
            }
        }
        exchange.close();
    }

    /**
     * @param exchange the request to read
     * @return the body as json
     * @throws JSONException if the body is not json
     */
    protected JSONObject getJSON(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bytes, StandardCharsets.UTF_8);
        if (body.isBlank()) return new JSONObject();
        return new JSONObject(body);
    }

    protected void methodNotAllowed(HttpExchange exchange) throws IOException {
        respond(exchange, "Method not allowed", METHOD_NOT_ALLOWED);
    }

    protected void badRequest(HttpExchange exchange) throws IOException {
        respond(exchange, "BAD REQUEST: " + BAD_REQUEST, BAD_REQUEST);
    }

    protected void notFound(HttpExchange exchange, String message) throws IOException {
        respond(exchange, message, NOT_FOUND);
    }

    protected void conflict(HttpExchange exchange, String message) throws IOException {
        respond(exchange, message, CONFLICT);
    }

    protected void jsonError(HttpExchange exchange) throws IOException {
        respond(exchange, "Can't parse JSON", JSON_ERROR);
    }
}
