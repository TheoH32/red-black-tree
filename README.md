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
