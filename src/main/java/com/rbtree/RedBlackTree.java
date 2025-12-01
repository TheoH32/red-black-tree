package src.main.java.com.rbtree;

// SOURCE: https://www.geeksforgeeks.org/dsa/introduction-to-red-black-tree/
// ALL COMMENTS ARE PASTED AND SOURCED FROM HERE ^^^^^^^^^^^^

public class RedBlackTree {
    private Node root;

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
        TreeSerializer.saveTreeToJson(root, "visualization/tree_data.json");
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
                u = newNode.parent.parent.left; // Uncle is left child

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

    
    public void delete(int data) {
        System.out.println("Delete not implemented yet!");
    }
 
}