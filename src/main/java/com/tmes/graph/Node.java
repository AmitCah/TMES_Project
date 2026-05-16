package com.tmes.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the graph, holding a specific K-mer string.
 * Each node is uniquely identified by an ID and maintains a list of its outgoing edges.
 */
public class Node {
    /** The K-mer string represented by this node. */
    private final String data;

    /** Unique ID for graph algorithms such as Tarjan's SCC. */
    private final int id;

    /** Adjacency list representing edges originating from this node. */
    private final List<Edge> outgoingEdges;

    /**
     * The constructor for a new Node.
     * Initializes the node with an ID and data.
     * Sets up an empty list for outgoing edges.
     * @param id The unique identifier for this node.
     * @param data The 4-character K-mer string.
     */
    public Node(int id, String data) {
        this.id = id;
        this.data = data;
        this.outgoingEdges = new ArrayList<>();
    }

    /**
     * Creates a new directional edge from this node to a destination node.
     * Prevents duplicate edges by overwriting the weight if the connection already exists.
     * @param destination The target node of the edge.
     * @param weight The capacity or weight of the edge.
     * Complexity: O(M) where M is the number of outgoing edges from this node.
     */
    public void addEdge(Node destination, int weight) {
        // 1. Check for an existing edge to the same destination
        for (Edge existingEdge : this.outgoingEdges) {
            if (existingEdge.getDestination().equals(destination)) {
                // 2. Collision detected. Overwrite the weight.
                // This ensures the Shortcut Layer can still overwrite the Base Layer.
                existingEdge.setWeight(weight);
                return; // 3. Exit early to prevent duplication
            }
        }

        // 4. No duplicate found. Safely add the new edge.
        Edge newEdge = new Edge(this, destination, weight);
        this.outgoingEdges.add(newEdge);
    }

    /**
     * Checks if a given object is a Node and has the same ID as this node.
     * @param o The object to compare with.
     * @return true if the IDs match, false otherwise.
     * Complexity: O(1)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id == node.id;
    }

    /**
     * @return The list of outgoing edges from this node.
     * Complexity: O(1)
     */
    public List<Edge> getEdges() {
        return outgoingEdges;
    }

    /**
     * @return The K-mer string content of this node.
     * Complexity: O(1)
     */
    public String getData() {
        return data;
    }

    /**
     * @return The unique ID of this node.
     * Complexity: O(1)
     */
    public int getId() {
        return id;
    }

    /**
     * Computes a hash code based on the unique node ID.
     * Required for O(1) lookups in HashMaps and HashSets.
     * Complexity: O(1)
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}