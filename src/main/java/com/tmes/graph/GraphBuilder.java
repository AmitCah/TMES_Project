package com.tmes.graph;

import java.util.List;

/**
 * A static utility class responsible for constructing the De Bruijn-inspired graph topology.
 * It builds the foundational structure and applies complex shortcut edges to ensure
 * cryptographic confusion properties based on the user's password.
 */
public class GraphBuilder {

    /**
     * Builds the foundational linear layer of the graph by extracting K-mers from the password,
     * enforcing node uniqueness, and sequentially connecting the overlapping K-mers.
     * Example: For password "APPLE" and k=3, it extracts "APP", "PPL", "PLE".
     *
     * Complexity: O(L log L) where L is the length of the password string, as the K-mer window scales logarithmically.
     *
     * @param password The raw password string used as the topological seed.
     * @param k        The K-mer window size.
     * @return The instantiated Graph with connected base nodes.
     * @throws IllegalArgumentException If the password is too short or K is less than 2.
     */
    public static Graph buildBaseLayer(String password, int k) {
        Graph graph = new Graph();
        if (k < 2 || password.length() < k) {
            throw new IllegalArgumentException("Invalid input: password too short or k is strictly less than 2.");
        }
        System.out.println("K-mer length="+k);
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

    /**
     * Injects deterministic, non-linear bypass edges (shortcuts) into the graph topology.
     * This destroys the linearity of the base layer, creating the necessary chaos and
     * bottlenecks required for the Edmonds-Karp min-cut hash.
     *
     * Complexity: O(V) where V is the number of unique nodes in the graph.
     *
     * @param graph The initialized base layer graph to be mutated.
     */
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
            // A long is used to defend against overflow when dealing with high Unicode characters.
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