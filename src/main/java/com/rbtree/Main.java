package com.rbtree;

public class Main {
    public static void main(String[] args) {
        RedBlackTree rbt = new RedBlackTree();

        // Clear any old visualization file at startup
        try {
            TreeSerializer.saveTreeToJson(null, "visualization/tree_data.json");
        } catch (Exception e) {
            System.err.println("Warning: couldn't clear previous visualization: " + e.getMessage());
        }

        // Start web server
        WebServer server = new WebServer(rbt, 8080);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // The server runs in background threads; keep main thread alive.
        System.out.println("Press Ctrl+C to stop.");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {}
    }
}