package com.tmes.graph;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

public class GraphBuilder {

    /**
     * Builds the initial De Bruijn graph from the password.
     * @param password The user's input string.
     * @param k The length of each K-mer (node content).
     * @return A constructed Graph object.
     */
    public static Graph buildBaseLayer(String password, int k) {
        Graph graph = getGraph(password, k);

        // 2. Connect sequential overlaps
        // In a basic password chain, Node i always connects to Node i+1
        List<Node> nodes = graph.getNodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node current = nodes.get(i);
            Node next = nodes.get(i + 1);

            // Default weight can be 1 or determined by ASCII.
            // For now, let's use 1 as a placeholder.
            current.addEdge(next, 1);
        }
        return graph;
    }

    private static Graph getGraph(String password, int k) {
        Graph graph = new Graph();

        // Validation: Password must be at least length k
        if (password.length() < k) {
            throw new IllegalArgumentException("Password too short for k=" + k);
        }

        // 1. Create all nodes (K-mers)
        // If password is "apple" (len 5) and k=3:
        // i=0 -> "app"
        // i=1 -> "ppl"
        // i=2 -> "ple"
        // Loop runs until i <= 5-3 (2)
        for (int i = 0; i <= password.length() - k; i++) {
            String sub = password.substring(i, i + k);
            graph.createNode(sub);
        }
        return graph;
    }
    /**
     * Layer 2: Adds deterministic "jump" edges to break linearity.
     * Formula: Target = (ASCII(C1) * ASCII(C2) + Index) % N
     */
    public static void addShortcutLayer(Graph graph) {
        List<Node> nodes = graph.getNodes();
        int N = nodes.size();

        for (Node current : nodes) {
            String kmer = current.getData();

            // Safety check: ensure kmer has at least 2 chars
            if (kmer.length() < 2) continue;

            char c1 = kmer.charAt(0);
            char c2 = kmer.charAt(1);
            int index = current.getId();

            // The Project Formula
            long targetIndexLong = ((long) c1 * c2 + index) % N;
            int targetIndex = (int) targetIndexLong;

            // Prevent self-loops if you want (optional, but good for flow networks)
            if (targetIndex != index) {
                Node targetNode = nodes.get(targetIndex);

                // We need a weight (capacity) for this edge.
                // Let's derive it from the chars so it's deterministic too.
                // Example: (c1 + c2) to keep it strictly data-dependent.
                int weight = (c1 + c2);

                current.addEdge(targetNode, weight);
                System.out.println("Shortcut added: " + current.getData() + " -> "
                        + targetNode.getData() + " (Target Idx: " + targetIndex + ")");
            }
        }
    }
    public static Graph createSubgraph(List<Node> validNodes) {
        Graph newGraph = new Graph();
        HashSet<Node> nodeSet = new HashSet<>(validNodes);
        for(Node node : validNodes)
        {
            ListIterator<Edge> iterator = node.getEdges().listIterator();
            while(iterator.hasNext())
            {
                Edge curr = iterator.next();
                if(!nodeSet.contains(curr.getDestination()))
                    iterator.remove();
            }
            newGraph.getNodes().add(node);
        }
        return newGraph;
    }
}