package src.main.java.com.rbtree;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RedBlackTree rbt = new RedBlackTree();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Red-Black Tree Visualizer Backend Running...");
        System.out.println("Enter integers to insert (enter -1 to quit):");

        while (true) {
            System.out.print("> ");
            int val = scanner.nextInt();
            if (val == -1) break;
            rbt.insert(val);
            System.out.println("Inserted " + val + ". Check visualization/index.html");
        }
        scanner.close();
    }
}