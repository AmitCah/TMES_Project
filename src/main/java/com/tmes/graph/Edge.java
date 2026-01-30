package com.tmes.graph;

public class Edge {
    private final Node source;
    private final Node destination;
    private int weight; // Dynamic weight (capacity) for Max-Flow later

    public Edge(Node source, Node destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public Node getDestination() {
        return destination;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return source.getData() + " -> " + destination.getData() + " (w:" + weight + ")";
    }
}
