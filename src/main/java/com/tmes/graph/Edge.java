package com.tmes.graph;

/**
 * Represents a directional edge between two nodes in the TMES graph.
 * Acts as a "pipe" with a specific capacity (weight) used for Max-Flow calculations.
 */
public class Edge {
    /** The node from which this edge originates. */
    private final Node source;

    /** The node to which this edge points. */
    private final Node destination;

    /**
     * The dynamic capacity of the edge.
     * Not final because it is actively modified by the TopologyOptimizer to meet security thresholds.
     */
    private int weight;

    /**
     * Constructs a new directed edge.
     * @param source The starting node.
     * @param destination The ending node.
     * @param weight The initial capacity/weight of the edge.
     */
    public Edge(Node source, Node destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    /**
     * @return The starting node of this edge.
     * Complexity: O(1)
     */
    public Node getSource() {
        return source;
    }

    /**
     * @return The target node of this edge.
     * Complexity: O(1)
     */
    public Node getDestination() {
        return destination;
    }

    /**
     * @return The current weight (capacity) of the edge.
     * Complexity: O(1)
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Modifies the weight of the edge. Used mainly during Topology Optimization.
     * @param weight The new weight to set.
     * Complexity: O(1)
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }
}