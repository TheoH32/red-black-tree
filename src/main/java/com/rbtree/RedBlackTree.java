package com.rbtree;

import java.util.ArrayList;
import java.util.List;
// SOURCE: https://www.geeksforgeeks.org/dsa/introduction-to-red-black-tree/
// ALL COMMENTS ARE PASTED AND SOURCED FROM HERE ^^^^^^^^^^^^

public class RedBlackTree {
    Node root;

    // Insertion Steps
    // BST Insert: Insert the new node like in a standard BST.
    // Fix Violations:
    // If the parent of the new node is black, no properties are violated.
    // If the parent is red, the tree might violate the Red Property, requiring fixes.

    public void insert(int data) {
        Node newNode = new Node(data);
        root = bstInsert(root, newNode);
        fixViolations(newNode);

        // Update visuals after ops
        try {
            TreeSerializer.saveTreeToJson(root, "visualization/tree_data.json");
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save tree JSON", e);
        }
    }

    // BST Insert: Insert the new node like in a standard BST.
    private Node bstInsert(Node root, Node newNode) {
        // SOURCE: https://www.geeksforgeeks.org/dsa/insertion-in-binary-search-tree/

        // case: tree is empty so return newNode
        if (root == null) {
            return newNode;
        }

        // recurse down
        if (newNode.data < root.data) {
            root.left = bstInsert(root.left, newNode);
            root.left.parent = root;
        } else {
            root.right = bstInsert(root.right, newNode);
            root.right.parent = root;
        }
        
        // return unchanged root
        return root;
    }

    // Fixing Violations During Insertion
    // After inserting the new node as a red node, we might encounter several cases depending on the colors of the node's parent and uncle (the sibling of the parent):
    // Case 1: Uncle is Red: Recolor the parent and uncle to black, and the grandparent to red. Then move up the tree to check for further violations.
    // Case 2: Uncle is Black:
    // Sub-case 2.1: Node is a right child: Perform a left rotation on the parent.
    // Sub-case 2.2: Node is a left child: Perform a right rotation on the grandparent and recolor appropriately.
    private void fixViolations(Node newNode) {
        Node uncle;
    // While newNode is not root and its parent is RED (Violation!)
        while (newNode.parent != null && newNode.parent.isRed) {
            
            // --- SIDE A: Parent is a LEFT child of Grandparent ---
            if (newNode.parent == newNode.parent.parent.left) {
                uncle = newNode.parent.parent.right; // Uncle is right child

                // Case 1: Uncle is RED
                // Solution: Push BlacnewNodeness down from Grandparent to Parent and Uncle
                if (uncle != null && uncle.isRed) {
                    newNode.parent.isRed = false;
                    uncle.isRed = false;
                    newNode.parent.parent.isRed = true;
                    newNode = newNode.parent.parent; // Move check up the tree
                } 
                else {
                    // Case 2: Uncle is BLACK, and newNode is a RIGHT child (Triangle)
                    // Solution: Rotate Parent Left to turn it into a Line (Case 3)
                    if (newNode == newNode.parent.right) {
                        newNode = newNode.parent;
                        leftRotate(newNode);
                    }
                    
                    // Case 3: Uncle is BLACK, and newNode is a LEFT child (Line)
                    // Solution: Color Parent BLACK, Grandparent RED, Rotate Grandparent Right
                    newNode.parent.isRed = false;
                    newNode.parent.parent.isRed = true;
                    rightRotate(newNode.parent.parent);
                }
            } 
            
            // --- SIDE B: Parent is a RIGHT child of Grandparent ---
            // This is the exact mirror image of Side A
            else {
                uncle = newNode.parent.parent.left; // Uncle is left child

                // Case 1: Uncle is RED
                if (uncle != null && uncle.isRed) {
                    newNode.parent.isRed = false;
                    uncle.isRed = false;
                    newNode.parent.parent.isRed = true;
                    newNode = newNode.parent.parent;
                } 
                else {
                    // Case 2: Uncle is BLACK, and newNode is a LEFT child (Triangle)
                    if (newNode == newNode.parent.left) {
                        newNode = newNode.parent;
                        rightRotate(newNode);
                    }

                    // Case 3: Uncle is BLACK, and newNode is a RIGHT child (Line)
                    newNode.parent.isRed = false;
                    newNode.parent.parent.isRed = true;
                    leftRotate(newNode.parent.parent);
                }
            }
        }

        // FINAL STEP: The Root must always be BLACK
        root.isRed = false;

    }

    private void leftRotate(Node pivot) {
        // The right child becomes the new parent of the subtree
        Node newParent = pivot.right;         

        // Move newParent's left subtree to pivot's right
        pivot.right = newParent.left;         
        if (newParent.left != null) {
            newParent.left.parent = pivot;    
        }

        // Link newParent to pivot's old parent
        newParent.parent = pivot.parent;      
        if (pivot.parent == null) {
            // Pivot was root — update root
            this.root = newParent;
        } else if (pivot == pivot.parent.left) {
            pivot.parent.left = newParent;    
        } else {
            pivot.parent.right = newParent;   
        }

        // Finish rotation: pivot becomes left child of newParent
        newParent.left = pivot;               
        pivot.parent = newParent;
    }


    private void rightRotate(Node pivot) {
        // The left child becomes the new parent of the subtree
        Node newParent = pivot.left;          

        // Move newParent's right subtree to pivot's left
        pivot.left = newParent.right;         
        if (newParent.right != null) {
            newParent.right.parent = pivot;   
        }

        // Link newParent to pivot's old parent
        newParent.parent = pivot.parent;      
        if (pivot.parent == null) {
            // Pivot was root — update root
            this.root = newParent;
        } else if (pivot == pivot.parent.right) {
            pivot.parent.right = newParent;   
        } else {
            pivot.parent.left = newParent;    
        }

        // Finish rotation: pivot becomes right child of newParent
        newParent.right = pivot;              
        pivot.parent = newParent;
    }


    public Node search(int key) {
        Node x = root;
        while (x != null && x != null) { // use NIL if you use a sentinel; otherwise check null
            int cmp = Integer.compare(key, x.data);
            if (cmp == 0) return x;
            x = (cmp < 0) ? x.left : x.right;
        }
        return null;
    }

    /** Convenience that returns the value or null. */
    public Integer get(int key) {
        Node n = search(key);
        return n == null ? null : n.data;
    }



    public void delete(int data) {
        Node node = search(data);
        if (node == null) {
            System.out.println("Node with data " + data + " not found.");
            return;
        }
        
        deleteNode(node);

        // Update visuals after ops
        try {
            TreeSerializer.saveTreeToJson(root, "visualization/tree_data.json");
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save tree JSON", e);
        }
    }

    // If the node to be deleted has no children, simply remove it and update the parent node.
    // If the node to be deleted has only one child, replace the node with its child.
    // If the node to be deleted has two children, then replace the node with its in-order successor, which is the leftmost node in the right subtree. Then delete the in-order successor node as if it has at most one child.
    // After the node is deleted, the red-black properties might be violated. To restore these properties, some color changes and rotations are performed on the nodes in the tree. The changes are similar to those performed during insertion, but with different conditions.
    // The deletion operation in a red-black tree takes O(log n) time on average, making it a good choice for searching and deleting elements in large data sets.
    // ...existing code...

    /** Helper: replace subtree rooted at u with subtree rooted at v */
    private void rbTransplant(Node u, Node v) {
        if (u.parent == null) {
            this.root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        if (v != null) v.parent = u.parent;
    }

    /** Helper: return minimum node in subtree rooted at x */
    private Node treeMinimum(Node x) {
        while (x.left != null) x = x.left;
        return x;
    }

    /** CLRS-style RB-DELETE adapted for nullable children (no NIL sentinel) */
    private void deleteNode(Node z) {
        if (z == null) return;

        Node y = z;
        boolean yOriginalIsRed = y.isRed;
        Node x;          // node that moved into y's original position (may be null)
        Node xParent = null; // parent of x when x is null

        if (z.left == null) {
            x = z.right;
            xParent = z.parent;
            rbTransplant(z, z.right);
        } else if (z.right == null) {
            x = z.left;
            xParent = z.parent;
            rbTransplant(z, z.left);
        } else {
            // z has two children: replace z with its successor y = min(z.right)
            y = treeMinimum(z.right);
            yOriginalIsRed = y.isRed;
            x = y.right;
            if (y.parent == z) {
                // x's parent becomes y (even if x is null, we remember parent)
                xParent = y;
                if (x != null) x.parent = y;
            } else {
                // move y's right child up
                rbTransplant(y, y.right);
                xParent = y.parent;
                y.right = z.right;
                if (y.right != null) y.right.parent = y;
            }
            rbTransplant(z, y);
            y.left = z.left;
            if (y.left != null) y.left.parent = y;
            y.isRed = z.isRed; // preserve original color of z
        }

        // If a black node was removed, fix double-black property
        if (!yOriginalIsRed) {
            rbDeleteFixup(x, xParent);
        }
    }


    private void rbDeleteFixup(Node x, Node parent) {
        // While x is not root and x is black (x==null treated as black)
        while ((x != root) && (x == null || !x.isRed)) {
            // Determine whether x is a left child of parent
            if (parent != null && x == parent.left) {
                Node w = parent.right; // sibling
                // Case 1: sibling is red
                if (w != null && w.isRed) {
                    w.isRed = false;
                    parent.isRed = true;
                    leftRotate(parent);
                    w = parent.right;
                }

                // Case 2: sibling is black and both children black
                if (w == null ||
                ((w.left == null || !w.left.isRed) && (w.right == null || !w.right.isRed))) {
                    if (w != null) w.isRed = true;
                    x = parent;
                    parent = x.parent;
                } else {
                    // Case 3: sibling black, left red, right black
                    if (w.right == null || !w.right.isRed) {
                        if (w.left != null) w.left.isRed = false;
                        w.isRed = true;
                        rightRotate(w);
                        w = parent.right;
                    }
                    // Case 4: sibling black, right red
                    if (w != null) {
                        w.isRed = parent.isRed;
                        parent.isRed = false;
                        if (w.right != null) w.right.isRed = false;
                    }
                    leftRotate(parent);
                    x = root;
                    parent = null;
                }
            } else {
                // x is right child or parent is null — mirror image
                if (parent == null) {
                    // No parent (shouldn't usually happen) — break defensively
                    break;
                }
                Node w = parent.left;
                // Case 1 mirror: sibling is red
                if (w != null && w.isRed) {
                    w.isRed = false;
                    parent.isRed = true;
                    rightRotate(parent);
                    w = parent.left;
                }

                // Case 2 mirror: sibling's children both black
                if (w == null ||
                ((w.left == null || !w.left.isRed) && (w.right == null || !w.right.isRed))) {
                    if (w != null) w.isRed = true;
                    x = parent;
                    parent = x.parent;
                } else {
                    // Case 3 mirror: sibling left black, right red -> rotate sibling
                    if (w.left == null || !w.left.isRed) {
                        if (w.right != null) w.right.isRed = false;
                        w.isRed = true;
                        leftRotate(w);
                        w = parent.left;
                    }
                    // Case 4 mirror: sibling left red
                    if (w != null) {
                        w.isRed = parent.isRed;
                        parent.isRed = false;
                        if (w.left != null) w.left.isRed = false;
                    }
                    rightRotate(parent);
                    x = root;
                    parent = null;
                }
            }
        }

        if (x != null) x.isRed = false;
    }

    // Returns a list of all node values (preorder). Synchronized to be safe with WebServer access.
    public synchronized List<Integer> getAllValues() {
        List<Integer> out = new ArrayList<>();
        collectValues(root, out);
        return out;
    }

    // preorder traversal to collect values
    private void collectValues(Node node, List<Integer> out) {
        if (node == null) return;
        out.add(node.data);
        collectValues(node.left, out);
        collectValues(node.right, out);
    }

    // Returns the number of nodes in the tree
    public synchronized int getNodeCount() {
        return getAllValues().size();
    }
}