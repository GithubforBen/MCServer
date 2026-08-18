package de.hems.utils.webconsole.modules;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.hems.utils.webconsole.CustomHandler;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Serves the page itself - the html, the stylesheet and the script - out of the jar.
 */
public class StaticModule implements WebModule {

    /** Where the files live inside the jar. */
    private static final String ROOT = "/web";

    @Override
    public String getId() {
        return "static";
    }

    @Override
    public String getTitle() {
        return "Oberfläche";
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void register(WebServer server) {
        server.route("/", new StaticHandler());
    }

    private static class StaticHandler extends CustomHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")
                    && !exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                methodNotAllowed(exchange);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.isEmpty() || path.equals("/")) path = "/index.html";
            String resource = ROOT + path;
            if (!isSafe(path)) {
                notFound(exchange, "Not found");
                return;
            }
            try (InputStream stream = StaticModule.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    notFound(exchange, "Not found");
                    return;
                }
                byte[] bytes = stream.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentTypeOf(path));
                exchange.sendResponseHeaders(OK, bytes.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(bytes);
                }
            } finally {
                exchange.close();
            }
        }

        /**
         * Keeps a request from reaching outside the web folder of the jar.
         *
         * @param path the path that was asked for
         * @return whether it may be served
         */
        private static boolean isSafe(String path) {
            return !path.contains("..") && !path.contains("//") && path.matches("/[A-Za-z0-9._/-]*");
        }

        private static String contentTypeOf(String path) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".html")) return "text/html; charset=utf-8";
            if (lower.endsWith(".css")) return "text/css; charset=utf-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
