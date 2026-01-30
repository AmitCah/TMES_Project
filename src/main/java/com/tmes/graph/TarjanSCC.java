package com.tmes.graph;

import java.util.*;

public class TarjanSCC {
    private int idCounter;
    private int sccCount;

    // Maps to hold algorithm state per Node ID
    // We use arrays for O(1) access since node IDs are 0..N-1
    private int[] ids;
    private int[] low;
    private boolean[] onStack;
    private Deque<Integer> stack; // The recursion stack

    private final Graph graph;

    public TarjanSCC(Graph graph) {
        this.graph = graph;
        int n = graph.getNodes().size();

        this.ids = new int[n];
        this.low = new int[n];
        this.onStack = new boolean[n];
        this.stack = new ArrayDeque<>();

        // Initialize arrays with -1 (unvisited)
        Arrays.fill(ids, -1);
        this.idCounter = 0;
        this.sccCount = 0;
    }

    public List<List<Node>> run() {
        List<List<Node>> sccs = new ArrayList<>();
        List<Node> nodes = graph.getNodes();

        // Run DFS on every unvisited node (handles disconnected graphs)
        for (int i = 0; i < nodes.size(); i++) {
            if (ids[i] == -1) {
                dfs(i, sccs);
            }
        }
        return sccs;
    }

    private void dfs(int at, List<List<Node>> sccs) {
        stack.push(at);
        onStack[at] = true;
        ids[at] = low[at] = idCounter++;

        Node node = graph.getNode(at);

        for (Edge edge : node.getEdges()) {
            int to = edge.getDestination().getId();

            if (ids[to] == -1) {
                // Case 1: 'to' is unvisited. Recurse down.
                dfs(to, sccs);
                // Upon return, propagate the low-link value up
                low[at] = Math.min(low[at], low[to]);
            } else if (onStack[to]) {
                // Case 2: 'to' is on the stack. This is a BACK EDGE!
                // We found a cycle. Update low-link.
                low[at] = Math.min(low[at], ids[to]);
            }
        }

        // Root of an SCC found
        if (ids[at] == low[at]) {
            List<Node> currentSCC = new ArrayList<>();
            while (true) {
                int nodeIndex = stack.pop();
                onStack[nodeIndex] = false;
                currentSCC.add(graph.getNode(nodeIndex));
                if (nodeIndex == at) break; // Stop when we pop the root
            }
            sccs.add(currentSCC);
            sccCount++;
        }
    }

    // Helper to get only the largest SCC (Project Requirement)
    public static List<Node> getLargestSCC(List<List<Node>> allSccs) {
        if (allSccs.isEmpty()) return new ArrayList<>();

        List<Node> largest = allSccs.get(0);
        for (List<Node> scc : allSccs) {
            if (scc.size() > largest.size()) {
                largest = scc;
            }
        }
        return largest;
    }
}