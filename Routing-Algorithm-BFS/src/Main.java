// Import necessary JavaFX classes for building GUI
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

// Import Java utility classes for collections and algorithms
import java.util.*;

// Main class that extends JavaFX Application to create GUI application
public class Main extends Application {
    // Map to store each node's visual Circle representation
    private final Map<String, Circle> nodes = new HashMap<>();

    // Graph represented as an adjacency list: node -> list of connected nodes
    private final Map<String, List<String>> graph = new HashMap<>();

    // Coordinates for placing each node visually on the screen
    private final Map<String, double[]> coordinates = new HashMap<>();

    // Stores the parent of each node during BFS for path reconstruction
    private final Map<String, String> parent = new HashMap<>();

    // Default x,y positions for 6 pre-defined nodes (A to F)
    private final double[][] defaultPositions = {
            {200, 300}, {300, 200}, {300, 400}, {400, 150}, {400, 450}, {500, 300}
    };

    // Root group that contains all visual elements of the scene
    private Group root;

    // Dropdown menus for selecting nodes in various operations
    private ComboBox<String> startSelector, endSelector, connectToSelector, removeSelector;

    // Vertical box layout that contains control buttons and dropdowns
    private VBox controlPanel;

    // Counter to assign position to newly added nodes
    private int nodeCount = 0;

    // JavaFX entry point for GUI applications
    @Override
    public void start(Stage stage) {
        // Create root group
        root = new Group();

        // Create initial graph and draw it on screen
        buildGraph();
        drawGraph();

        // Initialize dropdowns and set placeholder text
        startSelector = new ComboBox<>();
        endSelector = new ComboBox<>();
        connectToSelector = new ComboBox<>();
        removeSelector = new ComboBox<>();
        startSelector.setPromptText("Start Node");
        endSelector.setPromptText("End Node");
        connectToSelector.setPromptText("Connect To Node");
        removeSelector.setPromptText("Select Node to Remove");

        // Populate dropdowns with current node labels
        for (String label : graph.keySet()) {
            startSelector.getItems().add(label);
            endSelector.getItems().add(label);
            connectToSelector.getItems().add(label);
            removeSelector.getItems().add(label);
        }

        // Button to start BFS algorithm between selected nodes
        Button runBFS = createStyledButton("Run BFS");
        runBFS.setOnAction(e -> {
            resetColors(); // Reset node colors to default
            String start = startSelector.getValue();
            String end = endSelector.getValue();
            // Run BFS if both nodes are selected
            if (start != null && end != null) {
                new Thread(() -> bfs(start, end)).start(); // Run BFS in a background thread
            }
        });

        // Button to manually add edge between A and F
        Button addEdgeBtn = createStyledButton("Add Edge A-F");
        addEdgeBtn.setOnAction(e -> {
            if (graph.containsKey("A") && graph.containsKey("F") && !graph.get("A").contains("F")) {
                connect("A", "F"); // Connect A to F
                drawLineSafe("A", "F", Color.DARKGRAY); // Draw the edge
                log("Edge A-F added");
            }
        });

        // Input field to enter new node label
        TextField nodeNameField = new TextField();
        nodeNameField.setPromptText("Node Label");
        nodeNameField.setStyle("-fx-background-color: #222; -fx-text-fill: #eee; -fx-border-color: #00ffcc;");

        // Button to add new node
        Button addNodeBtn = createStyledButton("Add Node");
        addNodeBtn.setOnAction(e -> {
            String label = nodeNameField.getText().toUpperCase(); // Get label input
            String connectTo = connectToSelector.getValue(); // Get node to connect to

            // Proceed if label is not empty and node doesn't already exist
            if (!label.isEmpty() && !nodes.containsKey(label)) {
                // Assign default position
                double x = 150 + (nodeCount % 5) * 100;
                double y = 500;

                // Add node to graph and coordinates map
                coordinates.put(label, new double[]{x, y});
                graph.put(label, new ArrayList<>());

                // Create and store node visual (Circle)
                Circle circle = createNeonNode(x, y, label);
                nodes.put(label, circle);
                root.getChildren().add(circle);

                // Update all dropdowns
                startSelector.getItems().add(label);
                endSelector.getItems().add(label);
                connectToSelector.getItems().add(label);
                removeSelector.getItems().add(label);
                nodeCount++;

                log("Node " + label + " added.");

                // Connect to another node if selected
                if (connectTo != null && graph.containsKey(connectTo)) {
                    connect(label, connectTo);
                    drawLineSafe(label, connectTo, Color.DARKGRAY);
                    log("Connected " + label + " to " + connectTo);
                }
            }
        });

        // Button to remove selected node
        Button removeNodeBtn = createStyledButton("Remove Node");
        removeNodeBtn.setOnAction(e -> {
            String node = removeSelector.getValue();
            if (node != null && graph.containsKey(node)) {
                // Remove references in neighbors
                for (String neighbor : graph.get(node)) {
                    graph.get(neighbor).remove(node);
                }

                // Remove from all structures
                graph.remove(node);
                coordinates.remove(node);
                Circle circle = nodes.remove(node);
                if (circle != null) Platform.runLater(() -> root.getChildren().remove(circle));

                // Remove from dropdowns
                startSelector.getItems().remove(node);
                endSelector.getItems().remove(node);
                connectToSelector.getItems().remove(node);
                removeSelector.getItems().remove(node);

                log("Node " + node + " removed.");
            }
        });

        // Add all controls to the vertical control panel
        controlPanel = new VBox(10, startSelector, endSelector, runBFS, addEdgeBtn,
                nodeNameField, connectToSelector, addNodeBtn, removeSelector, removeNodeBtn);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setLayoutX(620);
        controlPanel.setLayoutY(20);

        root.getChildren().add(controlPanel);

        // Create the main scene and show the window
        Scene scene = new Scene(root, 900, 600, Color.web("#0d0d0d"));
        stage.setTitle("🌌 BFS Routing Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    // Build a sample graph with 6 nodes and default edges
    private void buildGraph() {
        String[] labels = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < labels.length; i++) {
            graph.put(labels[i], new ArrayList<>());
            coordinates.put(labels[i], defaultPositions[i]);
        }
        connect("A", "B");
        connect("A", "C");
        connect("B", "D");
        connect("C", "E");
        connect("D", "F");
    }

    // Draw all nodes and initial edges
    private void drawGraph() {
        for (String label : coordinates.keySet()) {
            double[] pos = coordinates.get(label);
            Circle circle = createNeonNode(pos[0], pos[1], label);
            nodes.put(label, circle);
            root.getChildren().add(circle);
        }
        drawLineSafe("A", "B", Color.DARKGRAY);
        drawLineSafe("A", "C", Color.DARKGRAY);
        drawLineSafe("B", "D", Color.DARKGRAY);
        drawLineSafe("C", "E", Color.DARKGRAY);
        drawLineSafe("D", "F", Color.DARKGRAY);
    }

    // Create a glowing circle with label
    private Circle createNeonNode(double x, double y, String label) {
        Circle circle = new Circle(x, y, 20);
        circle.setFill(Color.web("#3c9aff"));
        circle.setStroke(Color.web("#00ffe1"));
        circle.setStrokeWidth(2);
        circle.setEffect(new DropShadow(18, Color.web("#00ffe1")));

        Tooltip tip = new Tooltip("Node: " + label + "\nConnections: " + graph.get(label).size());
        Tooltip.install(circle, tip);

        Text text = new Text(x - 5, y + 5, label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", 14));
        Platform.runLater(() -> root.getChildren().add(text));
        return circle;
    }

    // Create a styled button with uniform appearance
    private Button createStyledButton(String label) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: #222; -fx-text-fill: #ffffff; -fx-border-color: #00ffcc; -fx-border-radius: 5; -fx-background-radius: 5;");
        return button;
    }

    // Connect two nodes bidirectionally (undirected graph)
    private void connect(String from, String to) {
        graph.get(from).add(to);
        graph.get(to).add(from);
    }

    // Draw a line between two nodes on screen (in a thread-safe way)
    private void drawLineSafe(String from, String to, Color color) {
        double[] fromPos = coordinates.get(from);
        double[] toPos = coordinates.get(to);
        Platform.runLater(() -> {
            Line line = new Line(fromPos[0], fromPos[1], toPos[0], toPos[1]);
            line.setStroke(color);
            line.setStrokeWidth(2);
            root.getChildren().add(line);
        });
    }

    // Breadth-First Search algorithm for finding path from start to end
    private void bfs(String start, String end) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        parent.clear();

        queue.add(start);
        visited.add(start);
        highlight(start, Color.LIME); // Highlight starting node
        log("Starting BFS from: " + start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            log("Visiting: " + current);
            if (current.equals(end)) {
                log("Destination " + end + " found.");
                drawPath(start, end); // Draw final path
                return;
            }

            // Visit neighbors
            for (String neighbor : graph.get(current)) {
                if (!visited.contains(neighbor)) {
                    parent.put(neighbor, current);
                    visited.add(neighbor);
                    queue.add(neighbor);
                    log("Queueing: " + neighbor + " from: " + current);
                    highlight(neighbor, Color.ORANGE);
                    sleep(500); // Delay for visualization
                }
            }
        }

        log("Destination " + end + " not reachable.");
    }

    // Draw the shortest path from start to end using parent map
    private void drawPath(String start, String end) {
        String current = end;
        while (!current.equals(start)) {
            String prev = parent.get(current);
            drawLineSafe(prev, current, Color.web("#00ffcc")); // Highlight path edge
            current = prev;
        }
        log("Path drawn in neon teal from " + start + " to " + end);
    }

    // Highlight a node with given color
    private void highlight(String nodeId, Color color) {
        Circle circle = nodes.get(nodeId);
        if (circle != null) {
            Platform.runLater(() -> circle.setFill(color));
        }
    }

    // Reset all node colors to original color
    private void resetColors() {
        for (Circle c : nodes.values()) {
            Platform.runLater(() -> c.setFill(Color.web("#3c9aff")));
        }
    }

    // Pause execution for given milliseconds
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // Print log message to console
    private void log(String message) {
        System.out.println("[LOG] " + message);
    }

    // Launch the application
    public static void main(String[] args) {
        launch();
    }
}