package com.tmes.graph;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private final String data; // The K-mer string (e.g., "Pass")
    private final int id;      // Unique ID for algorithms (like Tarjan's arrays)
    private final List<Edge> outgoingEdges; // Adjacency list

    public Node(int id, String data) {
        this.id = id;
        this.data = data;
        this.outgoingEdges = new ArrayList<>();
    }

    public void addEdge(Node destination, int weight) {
        Edge newEdge = new Edge(this, destination, weight);
        this.outgoingEdges.add(newEdge);
    }

    public List<Edge> getEdges() {
        return outgoingEdges;
    }

    public String getData() {
        return data;
    }

    public int getId() {
        return id;
    }
}