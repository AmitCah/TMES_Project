package com.tmes.graph;

import java.util.List;

/**
 * A static utility class responsible for constructing the De Bruijn-inspired graph topology.
 * It builds the foundational structure and applies complex shortcut edges to ensure
 * cryptographic confusion properties based on the user's password.
 */
public class GraphBuilder {

    public static Graph buildBaseLayer(String password, int k) {
        Graph graph = getGraph(password, k);
        List<Node> nodes = graph.getNodes();

        // Connect nodes sequentially to ensure a continuous baseline flow before optimization.
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node current = nodes.get(i);
            Node next = nodes.get(i + 1);
            current.addEdge(next, 1);
        }
        return graph;
    }

    private static Graph getGraph(String password, int k) {
        Graph graph = new Graph();

        // Strict validation: Prevent StringIndexOutOfBoundsException during K-mer extraction or math logic later.
        if (k < 2 || password.length() < k) {
            throw new IllegalArgumentException("Invalid input: password too short or k is strictly less than 2.");
        }

        for (int i = 0; i <= password.length() - k; i++) {
            String sub = password.substring(i, i + k);
            graph.createNode(sub);
        }
        return graph;
    }

    public static void addShortcutLayer(Graph graph) {
        List<Node> nodes = graph.getNodes();
        int N = nodes.size();

        for (Node current : nodes) {
            String kmer = current.getData();
            char c1 = kmer.charAt(0);
            char c2 = kmer.charAt(1);
            int index = current.getId();

            // The Project Formula: Creates a deterministic but chaotic jump.
            // We use modulo N to ensure the target index safely wraps around the graph's bounds.
            long targetIndexLong = ((long) c1 * c2 + index) % N;
            int targetIndex = (int) targetIndexLong;

            // Prevent self-loops to keep the flow network clean and meaningful.
            if (targetIndex != index) {
                Node targetNode = nodes.get(targetIndex);

                // Capacity is derived deterministically from the ASCII values.
                int weight = (c1 + c2);
                current.addEdge(targetNode, weight);
                System.out.println("Shortcut added: " + current.getData() + " -> "
                        + targetNode.getData() + " (Target Idx: " + targetIndex + ")");
            }
        }
    }
}