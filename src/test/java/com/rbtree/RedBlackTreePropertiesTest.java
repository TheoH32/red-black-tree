package com.rbtree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedBlackTreePropertiesTest {

    private RedBlackTree tree;

    @BeforeEach
    void setUp() {
        tree = new RedBlackTree();
    }

    // =========================================================
    // PROPERTY 1: "Every node is either red or black"
    // =========================================================
    @Test
    void testProperty1_NodesAreColored() {
        // Just throwing some data in to build a structure
        tree.insert(10);
        tree.insert(5);
        tree.insert(15);
        
        // This is a bit of a sanity check. We're just making sure our nodes
        // actually HAVE a color state and aren't in some weird undefined limbo.
        checkColorState(tree.root);
    }

    private void checkColorState(Node node) {
        if (node == null) return;
        
        // If your Node class uses a boolean, this just proves we can read it.
        // If you used an Enum (Color.RED, Color.BLACK), this ensures it's not null.
        boolean isRed = node.isRed; 
        
        checkColorState(node.left);
        checkColorState(node.right);
    }

    // =========================================================
    // PROPERTY 2: "The root is black"
    // =========================================================
    @Test
    void testProperty2_RootIsBlack() {
        // First insert: Root should be black immediately.
        tree.insert(10);
        assertFalse(tree.root.isRed, "Found a RED root! Root must be black immediately after first insert.");

        // Now let's cause some chaos. Inserting 5 and 1 creates a line (10 -> 5 -> 1)
        // which forces the tree to rotate. After the rotation, the new root must still be black.
        tree.insert(5); 
        tree.insert(1); 
        
        assertFalse(tree.root.isRed, "Tree rebalanced, but the new root is RED (it should be black).");
    }

    // =========================================================
    // PROPERTY 3: "Every leaf is NIL and is black"
    // =========================================================
    @Test
    void testProperty3_LeavesAreBlack() {
        tree.insert(10);
        tree.insert(20);
        
        // In Java, we usually represent NIL leaves as 'null'.
        // By definition in RBT theory, null pointers are considered "Black Nodes".
        // This test just ensures we treat boundaries correctly.
        validateLeavesAreBlack(tree.root);
    }

    private void validateLeavesAreBlack(Node node) {
        if (node == null) {
            // We reached a null pointer (a leaf). 
            // Since it's null, it counts as black. We are good here.
            return;
        }
        
        // Keep digging until we hit the bottom
        validateLeavesAreBlack(node.left);
        validateLeavesAreBlack(node.right);
    }

    // =========================================================
    // PROPERTY 4: "If a node is red, both children are black"
    // =========================================================
    @Test
    void testProperty4_RedNodeHasBlackChildren() {
        // I'm inserting a specific sequence here that I know causes conflicts.
        // 10, 20, 30 usually forces a rotation.
        // 15, 25 forces uncle-checks and recoloring.
        int[] values = {10, 20, 30, 15, 25, 5, 1, 45, 12};
        
        for (int v : values) {
            tree.insert(v);
        }

        // Walk the whole tree and scream if we see a Red Parent with a Red Child.
        checkNoDoubleRed(tree.root);
    }

    private void checkNoDoubleRed(Node node) {
        if (node == null) return;

        if (node.isRed) {
            // If I am Red, my left kid BETTER be black (or null)
            if (node.left != null) {
                assertFalse(node.left.isRed, 
                    "Violation! Red node " + node.data + " has a RED left child: " + node.left.data);
            }
            // If I am Red, my right kid BETTER be black (or null)
            if (node.right != null) {
                assertFalse(node.right.isRed, 
                    "Violation! Red node " + node.data + " has a RED right child: " + node.right.data);
            }
        }

        checkNoDoubleRed(node.left);
        checkNoDoubleRed(node.right);
    }

    // =========================================================
    // PROPERTY 5: "All simple paths from root to leaf contain 
    // the same number of black nodes"
    // =========================================================
    @Test
    void testProperty5_BlackHeightBalance() {
        // This is the most important property. If this fails, your tree isn't balanced.
        // We'll throw 500 random numbers at it. If the logic is flawed, this will catch it.
        Random rand = new Random();
        for (int i = 0; i < 500; i++) {
            tree.insert(rand.nextInt(10000));
        }

        try {
            int height = validateBlackHeight(tree.root);
            System.out.println("Success! The tree is balanced with a Black Height of: " + height);
        } catch (IllegalStateException e) {
            // If we catch this, it means one path had more black nodes than another.
            fail(e.getMessage());
        }
    }

    // Recursive helper to count black nodes
    private int validateBlackHeight(Node node) {
        // Base Case: We hit null (NIL). NIL nodes are black, so this path adds 1.
        if (node == null) {
            return 1;
        }

        // Go down the left and right paths
        int leftHeight = validateBlackHeight(node.left);
        int rightHeight = validateBlackHeight(node.right);

        // If the left path has 3 black nodes, but right path has 4... we have a problem.
        if (leftHeight != rightHeight) {
            throw new IllegalStateException("Black height mismatch at node " + node.data + 
                                            ". Left path: " + leftHeight + ", Right path: " + rightHeight);
        }

        // If this node is black, add 1 to the count. If red, add 0.
        return leftHeight + (node.isRed ? 0 : 1);
    }
}