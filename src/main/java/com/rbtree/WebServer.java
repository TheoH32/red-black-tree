package com.rbtree;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {
    private final RedBlackTree tree;
    private final int port;

    public WebServer(RedBlackTree tree, int port) {
        this.tree = tree;
        this.port = port;
    }

    public void start() throws IOException {
        // Start the server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // These are the pages/endpoints the browser can talk to
        server.createContext("/", new StaticHandler());
        server.createContext("/insert", new InsertHandler());
        server.createContext("/delete", new DeleteHandler());
        server.createContext("/search", new SearchHandler());
        server.createContext("/tree.json", new TreeHandler());
        server.createContext("/nodes", new NodesHandler()); // Needed for "Delete All" button
        
        // This is the new one for the graph
        server.createContext("/benchmark", new BenchmarkHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server is up! Go to http://localhost:" + port + "/");
    }

    // --- HANDLERS ---

    // This handles the performance test.
    // It runs the operations on a fresh tree and times them.
    class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Get the parameters from the URL (like ?n=1000&type=random)
            String query = exchange.getRequestURI().getQuery();
            int n = 1000; 
            String type = "random"; 
            
            if (query != null) {
                for (String part : query.split("&")) {
                    String[] kv = part.split("=");
                    if (kv[0].equals("n")) n = Integer.parseInt(kv[1]);
                    if (kv[0].equals("type")) type = kv[1];
                }
            }

            // 1. Generate the test data
            int[] data = generateInput(n, type);
            
            // 2. Measure Insertion Time
            // We use a new tree so previous tests don't mess up the timing
            RedBlackTree benchTree = new RedBlackTree(); 
            long startInsert = System.nanoTime();
            for (int x : data) {
                benchTree.insert(x);
            }
            long endInsert = System.nanoTime();
            
            // Convert to milliseconds
            double insertMs = (endInsert - startInsert) / 1_000_000.0;

            // 3. Measure Search Time (Look for every item we just added)
            long startSearch = System.nanoTime();
            for (int x : data) {
                benchTree.search(x);
            }
            long endSearch = System.nanoTime();
            double searchMs = (endSearch - startSearch) / 1_000_000.0;

            // 4. Measure Delete Time (Remove everything)
            long startDelete = System.nanoTime();
            for (int x : data) {
                benchTree.delete(x);
            }
            long endDelete = System.nanoTime();
            double deleteMs = (endDelete - startDelete) / 1_000_000.0;

            // Send the results back to the browser as JSON
            String json = String.format(
                "{\"insert\": %.4f, \"search\": %.4f, \"delete\": %.4f}",
                insertMs, searchMs, deleteMs
            );
            sendJson(exchange, 200, json);
        }

        // Helper to make an array of numbers
        private int[] generateInput(int n, String type) {
            int[] arr = new int[n];
            if (type.equals("sorted")) {
                // Worst case for BST: 0, 1, 2, 3...
                for (int i = 0; i < n; i++) arr[i] = i;
            } else if (type.equals("reverse")) {
                // Also bad for BST: 10, 9, 8...
                for (int i = 0; i < n; i++) arr[i] = n - i;
            } else { 
                // Random (Average case)
                Random rand = new Random();
                for (int i = 0; i < n; i++) arr[i] = rand.nextInt(n * 10);
            }
            return arr;
        }
    }

    // Standard handlers for the visualizer buttons
    class InsertHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String q = t.getRequestURI().getQuery();
            int val = Integer.parseInt(q.split("=")[1]);
            tree.insert(val);
            // Save to file so the frontend can draw it
            TreeSerializer.saveTreeToJson(tree.root, "visualization/tree_data.json");
            sendJson(t, 200, "{\"status\": \"ok\"}");
        }
    }

    class DeleteHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String q = t.getRequestURI().getQuery();
            int val = Integer.parseInt(q.split("=")[1]);
            tree.delete(val);
            TreeSerializer.saveTreeToJson(tree.root, "visualization/tree_data.json");
            sendJson(t, 200, "{\"status\": \"ok\"}");
        }
    }

    class SearchHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String q = t.getRequestURI().getQuery();
            int val = Integer.parseInt(q.split("=")[1]);
            boolean found = (tree.search(val) != null);
            sendJson(t, 200, "{\"found\": " + found + "}");
        }
    }

    class TreeHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String json = "null";
            // Just read the file we saved earlier
            try {
                java.nio.file.Path path = java.nio.file.Paths.get("visualization/tree_data.json");
                if (java.nio.file.Files.exists(path)) {
                    json = java.nio.file.Files.readString(path);
                }
            } catch (Exception e) { e.printStackTrace(); }
            sendJson(t, 200, json);
        }
    }
    
    class NodesHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
             // Return a list of all nodes (for the Delete All button)
             java.util.List<Integer> nodes = tree.getAllValues();
             StringBuilder json = new StringBuilder("[");
             for (int i = 0; i < nodes.size(); i++) {
                 if (i > 0) json.append(",");
                 json.append(nodes.get(i));
             }
             json.append("]");
             sendJson(t, 200, json.toString());
        }
    }

    class StaticHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            java.io.File file = new java.io.File("visualization" + path);
            if (file.exists()) {
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                java.nio.file.Files.copy(file.toPath(), os);
                os.close();
            } else {
                sendText(t, 404, "File not found");
            }
        }
    }

    // Helpers to send data back easily
    private void sendText(HttpExchange t, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        t.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = t.getResponseBody()) { os.write(bytes); }
    }
    private void sendJson(HttpExchange t, int code, String json) throws IOException {
        byte[] bytes = json.getBytes();
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = t.getResponseBody()) { os.write(bytes); }
    }
}