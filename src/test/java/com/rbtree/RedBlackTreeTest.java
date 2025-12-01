package com.rbtree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedBlackTreeTest {

    private RedBlackTree tree;

    @BeforeEach
    void setUp() {
        tree = new RedBlackTree();
    }

    // ==========================================
    // 1. ROOT PROPERTY TEST
    // "The root of the tree is always black."
    // ==========================================
    @Test
    void testRootProperty() {
        tree.insert(10);
        assertFalse(tree.root.isRed, "Root should be black after first insert");

        tree.insert(5); // Should trigger recolor/rotate logic
        tree.insert(20);
        
        assertFalse(tree.root.isRed, "Root should remain black after multiple inserts");
    }

    // ==========================================
    // 2. RED PROPERTY TEST
    // "Red nodes cannot have red children."
    // ==========================================
    @Test
    void testRedProperty() {
        // Insert enough nodes to force splits and recoloring
        int[] input = {10, 20, 30, 15, 25, 5, 1}; 
        
        for (int val : input) {
            tree.insert(val);
        }

        assertTrue(checkNoDoubleRed(tree.root), "Tree contains a Red node with a Red child!");
    }

    // Helper recursive method to check for Red-Red conflicts
    private boolean checkNoDoubleRed(Node node) {
        if (node == null) return true;

        if (node.isRed) {
            if (node.left != null && node.left.isRed) return false;
            if (node.right != null && node.right.isRed) return false;
        }

        return checkNoDoubleRed(node.left) && checkNoDoubleRed(node.right);
    }

    // ==========================================
    // 3. BLACK PROPERTY TEST
    // "Every path from a node to its descendant null nodes has the same number of black nodes."
    // ==========================================
    @Test
    void testBlackHeightProperty() {
        // Insert random values to create a complex tree
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            tree.insert(rand.nextInt(1000));
        }

        assertNotEquals(-1, checkBlackHeight(tree.root), "Black height mismatch detected in tree paths.");
    }

    // Helper: Returns black height if consistent, otherwise returns -1 (error)
    private int checkBlackHeight(Node node) {
        if (node == null) return 1; // Null nodes count as 1 black node

        int leftHeight = checkBlackHeight(node.left);
        int rightHeight = checkBlackHeight(node.right);

        // If subtrees are invalid or unbalanced regarding black height
        if (leftHeight == -1 || rightHeight == -1 || leftHeight != rightHeight) {
            return -1;
        }

        // Add 1 if current node is black, else add 0
        return leftHeight + (node.isRed ? 0 : 1);
    }

    // ==========================================
    // 4. LEAF PROPERTY TEST
    // "All leaves (NIL nodes) are black."
    // ==========================================
    @Test
    void testLeafProperty() {
        // In our Java implementation, null represents the black NIL nodes.
        // We verify that we never accidentally attached a "Red" null placeholder.
        // This test essentially checks that boundaries are clean.
        
        tree.insert(10);
        tree.insert(20);

        assertNull(tree.root.left.left, "Leaf children should be null (implicitly black)");
        assertNull(tree.root.left.right, "Leaf children should be null (implicitly black)");
    }

    // ==========================================
    // 5. SEARCH & CORRECTNESS TESTS
    // ==========================================
    @Test
    void testSearchFunctionality() {
        tree.insert(50);
        tree.insert(25);
        tree.insert(75);

        // We assume you have a basic search method implemented:
        // public Node search(int key) { ... }
        
        // Assertions might change depending on your search return type (Node vs boolean)
        assertNotNull(tree.search(50));
        assertNotNull(tree.search(25));
        assertNull(tree.search(99), "Searching for non-existent value should return null");
    }

    // ==========================================
    // 6. COMPLEXITY PROOF (Indirect O(log n))
    // ==========================================
    @Test
    void testTreeHeightLimit() {
        // A Red-Black Tree with N nodes must have a height <= 2 * log2(N+1).
        // If height exceeds this, it is degenerating into a linked list (O(n)), failing the requirement.

        int n = 1000;
        for (int i = 0; i < n; i++) {
            tree.insert(i); // Inserting sorted data is the worst case for BST, but RBT should handle it.
        }

        int height = getHeight(tree.root);
        double maxHeightAllowed = 2 * (Math.log(n + 1) / Math.log(2)); 

        System.out.println("Tree Height: " + height + ", Max Allowed: " + maxHeightAllowed);
        assertTrue(height <= maxHeightAllowed, "Tree height exceeds O(log n) bounds!");
    }

    private int getHeight(Node node) {
        if (node == null) return 0;
        return 1 + Math.max(getHeight(node.left), getHeight(node.right));
    }

    
    // ==========================================
    // 7. DELETE TEST (Placeholder)
    // ==========================================
    @Test
    void testDeleteAndMaintainProperties() {
        // Only run this if you have implemented delete
        /*
        tree.insert(10);
        tree.insert(20);
        tree.insert(5);
        
        tree.delete(20);
        
        assertNull(tree.search(20));
        assertTrue(checkNoDoubleRed(tree.root));
        assertNotEquals(-1, checkBlackHeight(tree.root));
        */
    }
}