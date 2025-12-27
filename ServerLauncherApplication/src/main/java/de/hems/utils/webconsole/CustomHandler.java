package de.hems.utils.webconsole;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomHandler {
    // Standard HTTP status codes
    protected static final int OK = 200;
    protected static final int BAD_REQUEST = 400;
    protected static final int NOT_FOUND = 404;
    protected static final int METHOD_NOT_ALLOWED = 405;
    protected static final int CONFLICT = 409;
    protected static final int JSON_ERROR = 410;
    protected static final int DATAT_NOT_FOUND = 501;

    protected void respond(HttpExchange exchange, String answer) throws IOException {
        respond(exchange, answer, OK);
    }

    public void respondDataNotFound(HttpExchange exchange, String data) throws IOException {
        respond(exchange, data, DATAT_NOT_FOUND);
    }

    protected void respondJson(HttpExchange exchange, JSONObject json, int code) throws IOException {
        respond(exchange, json.toString());
    }

    private void respond(HttpExchange exchange, String answer, int code) throws IOException {
        exchange.sendResponseHeaders(code, answer.getBytes().length);
        exchange.getResponseBody().write(answer.getBytes());
        exchange.getResponseBody().close();
    }

    protected JSONObject getJSON(HttpExchange exchange) throws IOException {
        try {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            String s = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(s);
            return new JSONObject(s);
        } catch (JSONException e) {
            jsonError(exchange);
            e.printStackTrace();
            throw e;
        }
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