package com.rbtree;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Very small embedded HTTP server to (1) serve files in visualization/,
 * (2) accept insert/clear requests and (3) expose the current tree JSON.
 *
 * No external dependencies; uses JDK HttpServer.
 */
public class WebServer {

    private final RedBlackTree tree;
    private final int port;

    public WebServer(RedBlackTree tree, int port) {
        this.tree = tree;
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/insert", new InsertHandler());
        server.createContext("/clear", new ClearHandler());
        server.createContext("/delete", new DeleteHandler());
        server.createContext("/tree.json", new TreeHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Web server started at http://localhost:" + port + "/");
        System.out.println("Use POST /insert?value=NUM  or POST /clear  or open / in browser.");
    }

    // Handler to insert a value: POST /insert?value=42
    class InsertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            URI uri = exchange.getRequestURI();
            String q = uri.getRawQuery();
            Integer value = null;
            if (q != null) {
                for (String part : q.split("&")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2 && "value".equals(kv[0])) {
                        try { value = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {}
                    }
                }
            }
            if (value == null) {
                // try reading body as plain number
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes()).trim();
                    if (!body.isEmpty()) {
                        value = Integer.parseInt(body);
                    }
                } catch (Exception ignored) {}
            }

            if (value == null) {
                sendText(exchange, 400, "Missing or invalid 'value' parameter");
                return;
            }

            // synchronize to avoid concurrent modifications
            synchronized (tree) {
                tree.insert(value);
                // try to persist visualization file too; ignore failures
                try {
                    TreeSerializer.saveTreeToJson(tree.root, "visualization/tree_data.json");
                } catch (IOException e) {
                    System.err.println("Warning: could not write visualization file: " + e.getMessage());
                }
            }

            String json = TreeSerializer.toJson(tree.root);
            sendJson(exchange, 200, json);
        }
    }

    // Handler to clear the tree file (POST /clear)
    class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            // write null to visualization file
            try {
                TreeSerializer.saveTreeToJson(null, "visualization/tree_data.json");
            } catch (IOException e) {
                System.err.println("Warning: could not write visualization file: " + e.getMessage());
            }
            sendText(exchange, 200, "Cleared");
        }
    }

    // Handler to return current tree JSON (GET /tree.json)
    class TreeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            String json;
            synchronized (tree) {
                json = TreeSerializer.toJson(tree.root);
            }
            sendJson(exchange, 200, json);
        }
    }

    // Very small static file handler for visualization/
    class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || "/".equals(path)) {
                path = "/index.html";
            }
            Path file = Path.of("visualization", path).normalize();
            if (!file.startsWith(Path.of("visualization")) || !Files.exists(file)) {
                sendText(exchange, 404, "Not Found");
                return;
            }
            byte[] bytes = Files.readAllBytes(file);
            String contentType = guessContentType(file.toString());
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }


    // Handler to delete a specific value: POST /delete?value=42
    class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            URI uri = exchange.getRequestURI();
            String q = uri.getRawQuery();
            Integer value = null;
            if (q != null) {
                for (String part : q.split("&")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2 && "value".equals(kv[0])) {
                        try { value = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {}
                    }
                }
            }
            if (value == null) {
                // try reading body as plain number
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes()).trim();
                    if (!body.isEmpty()) {
                        value = Integer.parseInt(body);
                    }
                } catch (Exception ignored) {}
            }

            if (value == null) {
                sendText(exchange, 400, "Missing or invalid 'value' parameter");
                return;
            }

            synchronized (tree) {
                tree.delete(value);
                try {
                    TreeSerializer.saveTreeToJson(tree.root, "visualization/tree_data.json");
                } catch (IOException e) {
                    System.err.println("Warning: could not write visualization file: " + e.getMessage());
                }
            }

            String json = TreeSerializer.toJson(tree.root);
            sendJson(exchange, 200, json);
        }
    }   

    private void sendText(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String guessContentType(String name) {
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}