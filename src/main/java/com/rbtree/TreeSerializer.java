package com.rbtree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TreeSerializer {

    public static void saveTreeToJson(Node root, String filePath) throws IOException {
        String json = nodeToJson(root);
        Files.writeString(Path.of(filePath), json);
    }

    // Public helper to return JSON string without writing the file.
    public static String toJson(Node root) {
        return nodeToJson(root);
    }

    private static String nodeToJson(Node node) {
        if (node == null) {
            return "null";
        }
        String color = node.isRed ? "RED" : "BLACK";
        String left = nodeToJson(node.left);
        String right = nodeToJson(node.right);

        // Very small, predictable JSON string for tests (no external dependency)
        return "{"
            + "\"data\": " + node.data + ", "
            + "\"color\": \"" + color + "\", "
            + "\"left\": " + left + ", "
            + "\"right\": " + right
            + "}";
    }
}