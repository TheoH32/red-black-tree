package com.rbtree;

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
        return searchRecursive(root, key);
    }

    private Node searchRecursive(Node current, int key) {
        if (current == null || current.data == key) {
            return current;
        }
        if (key < current.data) {
            return searchRecursive(current.left, key);
        }
        return searchRecursive(current.right, key);
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
    private void deleteNode(Node node) {
        Node x, y; // x is the node that moves up, y is the node being removed/moved

        // 1. Determine which node 'y' to physically remove or move
        if (node.left == null || node.right == null) {
            y = node; // Node has 0 or 1 child
        } else {
            y = getSuccessor(node.right); // Node has 2 children, find successor
        }

        // 2. Determine 'x', the child of 'y' (can be null)
        if (y.left != null) {
            x = y.left;
        } else {
            x = y.right;
        }

        // 3. Link x to y's parent
        if (x != null) {
            x.parent = y.parent;
        }

        if (y.parent == null) {
            this.root = x; // y was root
        } else if (y == y.parent.left) {
            y.parent.left = x;
        } else {
            y.parent.right = x;
        }

        // 4. If we moved a successor (case with 2 children), copy data to original node
        if (y != node) {
            node.data = y.data;
        }

        // 5. If the removed/moved node 'y' was BLACK, we lost a black height. Fix it.
        if (!y.isRed) {
            // Handle the tricky case where x is null (leaf removal)
            // We create a temporary dummy node if x is null to perform the fix
            if (x == null) {
                // If tree became empty, just return
                if (root == null) return;
                // Temporarily pretend a null node is double black? 
                // Actually, standard algorithms pass 'x' as null, but we need the parent.
                // Easier approach for this specific structure: Fix logic handles null x usually, 
                // but we need to know x's parent. 
                // Let's call fixDelete with the parent and explicit instructions, 
                // OR use a sentinel. Since we don't use sentinels:
                fixDelete(x, y.parent); 
            } else {
                fixDelete(x, x.parent);
            }
        }
    }

    // Helper to find the smallest node in the right subtree
    private Node getSuccessor(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

// x is the node that moved up (or null), parent is its parent
private void fixDelete(Node x, Node parent) {
    Node sibling;

    // While x is not root and x is BLACK (Double Black situation)
    while (x != root && (x == null || !x.isRed)) {
        
        // --- SIDE A: x is a LEFT child ---
        if (x == parent.left) {
            sibling = parent.right;

            // Case 1: Sibling is RED
            // Strategy: Rotate parent left to make sibling black, then continue
            if (sibling != null && sibling.isRed) {
                sibling.isRed = false;
                parent.isRed = true;
                leftRotate(parent);
                sibling = parent.right; // New sibling after rotation
            }

            // Case 2: Sibling's children are both BLACK (or null)
            // Strategy: Remove black from sibling (make it Red) and push issue up to parent
            if (sibling == null || 
               ((sibling.left == null || !sibling.left.isRed) && 
                (sibling.right == null || !sibling.right.isRed))) {
                
                if (sibling != null) sibling.isRed = true;
                x = parent;          // Move up
                parent = x.parent;   // Update parent ref
            } 
            else {
                // Case 3: Sibling's Left Child is RED (Right Child is Black)
                // Strategy: Rotate sibling right to become Case 4
                if (sibling.right == null || !sibling.right.isRed) {
                    if (sibling.left != null) sibling.left.isRed = false;
                    sibling.isRed = true;
                    rightRotate(sibling);
                    sibling = parent.right;
                }

                // Case 4: Sibling's Right Child is RED
                // Strategy: Rotate parent left, swap colors, done.
                sibling.isRed = parent.isRed;
                parent.isRed = false;
                if (sibling.right != null) sibling.right.isRed = false;
                leftRotate(parent);
                x = root; // Terminate loop
            }
        } 
        
        // --- SIDE B: x is a RIGHT child --- (Mirror of Side A)
        else {
            sibling = parent.left;

            // Case 1: Sibling is RED
            if (sibling != null && sibling.isRed) {
                sibling.isRed = false;
                parent.isRed = true;
                rightRotate(parent);
                sibling = parent.left;
            }

            // Case 2: Sibling's children are both BLACK
            if (sibling == null || 
               ((sibling.left == null || !sibling.left.isRed) && 
                (sibling.right == null || !sibling.right.isRed))) {
                
                if (sibling != null) sibling.isRed = true;
                x = parent;
                parent = x.parent;
            } 
            else {
                // Case 3: Sibling's Right Child is RED
                if (sibling.left == null || !sibling.left.isRed) {
                    if (sibling.right != null) sibling.right.isRed = false;
                    sibling.isRed = true;
                    leftRotate(sibling);
                    sibling = parent.left;
                }

                // Case 4: Sibling's Left Child is RED
                sibling.isRed = parent.isRed;
                parent.isRed = false;
                if (sibling.left != null) sibling.left.isRed = false;
                rightRotate(parent);
                x = root; // Terminate loop
            }
        }
    }

    if (x != null) x.isRed = false;
}
}