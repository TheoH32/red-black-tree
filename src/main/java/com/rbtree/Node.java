package src.main.java.com.rbtree;

public class Node {

    // data 
    int data;

    // defines whether node is (red) or (not red --> black)
    boolean isRed;

    // left and right children
    Node left;
    Node right;

    // parent node of node
    Node parent;

    // CONSTRUCTOR
    public Node(int data) {
        this.data = data;
        this.isRed = true;
    }


}