package com.tmes.graph;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private final List<Node> nodes;

    public Graph() {
        this.nodes = new ArrayList<>();
    }

    public Node createNode(String data) {
        // The ID is just the current size (0, 1, 2...)
        Node newNode = new Node(nodes.size(), data);
        nodes.add(newNode);
        return newNode;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public Node getNode(int id) {
        if (id >= 0 && id < nodes.size()) {
            return nodes.get(id);
        }
        return null; // Or throw exception
    }

    // Helper to print graph state
    public void printGraph() {
        for (Node node : nodes) {
            System.out.println("Node " + node.getId() + " [" + node.getData() + "]:");
            for (Edge edge : node.getEdges()) {
                System.out.println("   -> " + edge.getDestination().getData() + " (w:" + edge.getWeight() + ")");
            }
        }
    }
}