package com.tmes.graph;

import java.util.List;

/**
 * A static utility class responsible for constructing the De Bruijn-inspired graph topology.
 * It builds the foundational structure and applies complex shortcut edges to ensure
 * cryptographic confusion properties based on the user's password.
 */
public class GraphBuilder {
    /// APPLE, k=3 APP-PPL-PLE
    public static Graph buildBaseLayer(String password, int k) {
        // The sequential connection loop is removed. Connection now happens directly during generation.
        return getGraph(password, k);
    }

    private static Graph getGraph(String password, int k) {
        Graph graph = new Graph();
        if (k < 2 || password.length() < k) {
            throw new IllegalArgumentException("Invalid input: password too short or k is strictly less than 2.");
        }

        Node previousNode = null;
        for (int i = 0; i <= password.length() - k; i++) {
            String sub = password.substring(i, i + k);

            // This guarantees a true De Bruijn topology by enforcing uniqueness.
            Node currentNode = graph.getOrCreateNode(sub);

            // Connect the overlap: The previous K-mer's suffix is the current K-mer's prefix.
            if (previousNode != null) {
                previousNode.addEdge(currentNode, 1);
            }
            previousNode = currentNode;
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