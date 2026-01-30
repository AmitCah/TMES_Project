package com.tmes.graph;

import java.util.*;

public class EdmondsKarp {

    public static FlowResult compute(Graph graph, Node source, Node sink) {
        // --- Setup Residual Graph ---
        Map<Node, Map<Node, Integer>> residualGraph = new HashMap<>();

        // Step 1: Initialize empty maps for ALL nodes first
        // This prevents overwriting existing data later
        for (Node n : graph.getNodes()) {
            residualGraph.put(n, new HashMap<>());
        }

        // Step 2: Populate edges safely
        for (Node u : graph.getNodes()) {
            for (Edge e : u.getEdges()) {
                Node v = e.getDestination();

                // Add Forward Edge (Capacity = Weight)
                residualGraph.get(u).put(v, e.getWeight());

                // Add Backward Edge (Capacity = 0) if it doesn't exist
                // (If a real edge v->u exists, we will overwrite this with real weight later, which is fine)
                residualGraph.get(v).putIfAbsent(u, 0);
            }
        }

        int maxFlow = 0;
        Map<Node, Node> parentMap = new HashMap<>();

        // --- Phase 1: Max Flow Calculation ---
        while (bfs(residualGraph, source, sink, parentMap)) {
            // 1. Find bottleneck
            int pathFlow = Integer.MAX_VALUE;
            Node curr = sink;
            while (curr != source) {
                Node prev = parentMap.get(curr);
                Integer capacity = residualGraph.get(prev).get(curr);
                if (capacity == null) {
                    throw new RuntimeException("Logic Error: BFS path does not exist in Residual Graph. "
                            + "Prev: " + prev.getData() + " -> Curr: " + curr.getData());
                }
                pathFlow = Math.min(pathFlow, capacity);
                curr = prev;
            }

            // 2. Update residual capacities
            curr = sink;
            while (curr != source) {
                Node prev = parentMap.get(curr);

                // Subtract from forward
                int currentCap = residualGraph.get(prev).get(curr);
                residualGraph.get(prev).put(curr, currentCap - pathFlow);

                // Add to backward
                int reverseCap = residualGraph.get(curr).get(prev);
                residualGraph.get(curr).put(prev, reverseCap + pathFlow);

                curr = prev;
            }
            maxFlow += pathFlow;
        }

        // --- Phase 2: Min-Cut Extraction ---
        // Run BFS one last time to find all nodes reachable from Source in the Residual Graph
        Set<Node> sSet = getReachableNodes(residualGraph, source);
        List<Edge> minCutEdges = new ArrayList<>();

        // Iterate over ALL ORIGINAL EDGES
        // If an edge goes from a Node in S-Set to a Node NOT in S-Set, it is part of the cut.
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

    // Standard BFS for Augmenting Paths
    private static boolean bfs(Map<Node, Map<Node, Integer>> residualGraph, Node s, Node t, Map<Node, Node> parentMap) {
        parentMap.clear();
        Queue<Node> queue = new LinkedList<>();
        Set<Node> visited = new HashSet<>();

        queue.add(s);
        visited.add(s);
        parentMap.put(s, null);

        while (!queue.isEmpty()) {
            Node u = queue.poll();
            if (u.equals(t)) return true;

            Map<Node, Integer> neighbors = residualGraph.get(u);
            if (neighbors != null) {
                for (Map.Entry<Node, Integer> entry : neighbors.entrySet()) {
                    Node v = entry.getKey();
                    int capacity = entry.getValue();

                    if (!visited.contains(v) && capacity > 0) {
                        visited.add(v);
                        parentMap.put(v, u);
                        queue.add(v);
                    }
                }
            }
        }
        return false;
    }

    // Helper BFS to find S-Set
    private static Set<Node> getReachableNodes(Map<Node, Map<Node, Integer>> residualGraph, Node source) {
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();

        queue.add(source);
        visited.add(source);

        while(!queue.isEmpty()) {
            Node u = queue.poll();
            Map<Node, Integer> neighbors = residualGraph.get(u);
            if (neighbors == null) continue;

            for (Map.Entry<Node, Integer> entry : neighbors.entrySet()) {
                Node v = entry.getKey();
                int capacity = entry.getValue();

                // In the final residual graph, we can only move if capacity > 0
                if (!visited.contains(v) && capacity > 0) {
                    visited.add(v);
                    queue.add(v);
                }
            }
        }
        return visited;
    }
}