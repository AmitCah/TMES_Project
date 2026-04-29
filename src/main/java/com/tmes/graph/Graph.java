package com.tmes.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the topological structure of the encryption system.
 * Manages all the nodes in the system using a List for O(1) index-based access.
 * To maintain Data Consistency during topology optimization, edges are not stored
 * globally here, but rather managed directly within each Node.
 */
public class Graph {

    /** A sequential list containing all nodes. A node's index in this list corresponds to its unique ID. */
    private final List<Node> nodes;

    /**
     * Constructs a new, empty graph.
     * Initializes the underlying ArrayList for nodes.
     */
    public Graph() {
        this.nodes = new ArrayList<>();
    }

    /**
     * Creates a new node, assigns it an ID based on the current graph size,
     * and adds it to the graph.
     * Complexity: O(1)
     * @param data The K-mer string for the new node.
     */
    public void createNode(String data) {
        Node newNode = new Node(nodes.size(), data);
        nodes.add(newNode);
    }

    /**
     * Retrieves all nodes currently existing in the graph.
     * @return A List containing all nodes.
     * Complexity: O(1)
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Retrieves a specific node from the graph using its unique identifier.
     * @param id The unique integer ID of the requested node.
     * @return The Node object, or null if the ID is out of bounds.
     * Complexity: O(1)
     */
    public Node getNode(int id) {
        if (id >= 0 && id < nodes.size()) {
            return nodes.get(id);
        }
        return null;
    }
}