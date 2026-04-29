package com.tmes.encryption;

import com.tmes.graph.Edge;
import com.tmes.graph.Graph;
import com.tmes.graph.Node;

import java.util.*;

/**
 * Implementation of the Edmonds-Karp algorithm for calculating the Maximum Flow
 * and isolating the Minimum-Cut (Min-Cut) in the graph topology.
 * These metrics (P and Q) directly form the encryption keys for the image cipher.
 */
public class EdmondsKarp {

    /**
     * Computes the maximum flow and extracts the min-cut edges from the graph.
     * Builds a residual graph and iteratively finds augmenting paths using BFS.
     * @param graph The complete graph topology.
     * @param source The starting node (root of the largest SCC).
     * @param sink The target node (end of the largest SCC).
     * @return A FlowResult object containing the max flow (P) and the min-cut edges.
     * Complexity: O(V * E^2) where V is vertices and E is edges.
     */
    public static FlowResult compute(Graph graph, Node source, Node sink) {
        // We use a Map of Maps for the residual graph to allow O(1) lookups and mutations of edge capacities.
        Map<Node, Map<Node, Integer>> residualGraph = new HashMap<>();

        // Initialize the outer map for all nodes to prevent NullPointerExceptions during lookups.
        for (Node n : graph.getNodes()) {
            residualGraph.put(n, new HashMap<>());
        }

        // Populate the residual graph with initial capacities based on the physical graph.
        for (Node u : graph.getNodes()) {
            for (Edge e : u.getEdges()) {
                Node v = e.getDestination();

                // Forward edge gets the actual physical weight/capacity.
                residualGraph.get(u).put(v, e.getWeight());

                // Backward edge is initialized to 0, which gives the algorithm the ability to "undo" flow later.
                residualGraph.get(v).putIfAbsent(u, 0);
            }
        }

        int maxFlow = 0;
        // parentMap will store the exact augmenting path found by the BFS traversal.
        Map<Node, Node> parentMap = new HashMap<>();

        // The core loop: Continues as long as BFS finds a valid path from source to sink with available capacity.
        while (bfs(residualGraph, source, sink, parentMap)) {

            int pathFlow = Integer.MAX_VALUE;
            Node curr = sink;

            // 1st Pass: Trace backward from sink to source to find the bottleneck capacity (pathFlow).
            while (curr != source) {
                Node prev = parentMap.get(curr);
                Integer capacity = residualGraph.get(prev).get(curr);

                if (capacity == null) {
                    throw new RuntimeException("Logic Error: BFS path does not exist in Residual Graph.");
                }

                // The bottleneck is the minimum capacity edge along the entire path.
                pathFlow = Math.min(pathFlow, capacity);
                curr = prev;
            }

            curr = sink;

            // 2nd Pass: Trace backward again to update the residual capacities based on the bottleneck.
            while (curr != source) {
                Node prev = parentMap.get(curr);

                // Subtract the flow from the forward edge.
                int currentCap = residualGraph.get(prev).get(curr);
                residualGraph.get(prev).put(curr, currentCap - pathFlow);

                // Add the flow to the backward edge (enabling the "undo" mechanic for future BFS iterations).
                int reverseCap = residualGraph.get(curr).get(prev);
                residualGraph.get(curr).put(prev, reverseCap + pathFlow);

                curr = prev;
            }

            // Add this path's bottleneck flow to our total accumulated maximum flow.
            maxFlow += pathFlow;
        }

        // Phase 2: After max flow is reached, find all nodes still reachable from the source (the S-Set).
        Set<Node> sSet = getReachableNodes(residualGraph, source);
        List<Edge> minCutEdges = new ArrayList<>();

        // Extract the Min-Cut: Any physical edge going from a reachable node (S-Set) to an unreachable node.
        for (Node u : graph.getNodes()) {
            if (sSet.contains(u)) {
                for (Edge e : u.getEdges()) {
                    Node v = e.getDestination();
                    if (!sSet.contains(v)) {
                        minCutEdges.add(e);
                    }
                }
            }
        }

        return new FlowResult(maxFlow, minCutEdges);
    }

    /**
     * Breadth-First Search (BFS) to find the shortest augmenting path in the residual graph.
     * @param residualGraph The graph tracking current flow capacities.
     * @param s The source node.
     * @param t The sink node.
     * @param parentMap Map to reconstruct the path taken.
     * @return True if a path with available capacity was found, false otherwise.
     * Complexity: O(V + E)
     */
    private static boolean bfs(Map<Node, Map<Node, Integer>> residualGraph, Node s, Node t, Map<Node, Node> parentMap) {
        parentMap.clear();
        Queue<Node> queue = new LinkedList<>();
        Set<Node> visited = new HashSet<>();

        queue.add(s);
        visited.add(s);
        parentMap.put(s, null); // The source has no parent.

        while (!queue.isEmpty()) {
            Node u = queue.poll();

            // If we reached the sink, the path is complete.
            if (u.equals(t)) return true;

            Map<Node, Integer> neighbors = residualGraph.get(u);
            if (neighbors != null) {
                for (Map.Entry<Node, Integer> entry : neighbors.entrySet()) {
                    Node v = entry.getKey();
                    int capacity = entry.getValue();

                    // Only traverse edges that haven't been visited AND have remaining capacity to push flow.
                    if (!visited.contains(v) && capacity > 0) {
                        visited.add(v);
                        parentMap.put(v, u);
                        queue.add(v);
                    }
                }
            }
        }
        return false; // No more paths available; we have reached Maximum Flow.
    }

    /**
     * Traverses the final residual graph to find all nodes in the S-Set (reachable from source).
     * @param residualGraph The final state of the residual graph.
     * @param source The starting node.
     * @return A set of nodes that are part of the source component of the Min-Cut.
     * Complexity: O(V + E)
     */
    private static Set<Node> getReachableNodes(Map<Node, Map<Node, Integer>> residualGraph, Node source) {
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();

        queue.add(source);
        visited.add(source);

        while(!queue.isEmpty()) {
            Node u = queue.poll();
            Map<Node, Integer> neighbors = residualGraph.get(u);

            if (neighbors != null) {
                for (Map.Entry<Node, Integer> entry : neighbors.entrySet()) {
                    Node v = entry.getKey();
                    int capacity = entry.getValue();

                    // In the final residual graph, we can only step to nodes if the capacity > 0.
                    // If capacity is 0, it means the edge is fully saturated (part of the Min-Cut).
                    if (!visited.contains(v) && capacity > 0) {
                        visited.add(v);
                        queue.add(v);
                    }
                }
            }
        }
        return visited;
    }

    /**
     * Calculates the cryptographic 'Q' key based on the Min-Cut edges.
     * Iterates through the cut edges and performs an XOR sum on their string data.
     * @param minCutEdges The collection of edges forming the minimum cut.
     * @return The integer hash representing the Q parameter.
     * Complexity: O(C * L) where C is cut size and L is string length.
     */
    public static int calculateMinCutHash(Collection<Edge> minCutEdges) {
        int q = 0;
        for (Edge edge : minCutEdges) {
            String sourceData = edge.getSource().getData();
            String destData = edge.getDestination().getData();
            int edgeWeight = 0;

            // To prevent IndexOutOfBounds on varying string sizes, we only loop up to the shortest string length.
            int minLength = Math.min(sourceData.length(), destData.length());
            for (int i = 0; i < minLength; i++) {
                // Apply Bitwise XOR to the characters to generate the hash value.
                edgeWeight += sourceData.charAt(i) ^ destData.charAt(i);
            }
            q += edgeWeight;
        }
        return q;
    }
}