# HOW TO USE
1. **Build the Project**  
   Ensure you have Maven installed. From the project root directory, run:
   ```bash
   mvn -DskipTests=true package
   ```
2. **Run the Application**
    Start the Java application with:
    ```bash
    java -cp target/classes com.rbtree.Main
    ```
    This will start a local server on port 8080.
3. **Open Browser and Test**
    Open your web browser and navigate to `http://localhost:8080/` to view the visualization.
    

# HOW TO RUN TEST FIXTURE
To run the unit tests, execute:
```bash
mvn test
```
# Deliverables

- **Source code of the Red-Black Tree implementation**  
  **WHERE:** well here

- **Test suite demonstrating the correctness of the implementation**  
  **WHERE:** `src/test/java/com/rbtree/RedBlackTreePropertiesTest.java`  
  Run `mvn test` to execute

- **Performance analysis report, including time complexity measurements and comparisons with other data structures**  
  1. Run local server  
  2. Press "Run Performance Analysis" button

- **Documentation covering implementation details and optimizations**  
  Code is documented
