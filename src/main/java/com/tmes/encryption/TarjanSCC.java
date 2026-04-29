package com.tmes.encryption;

import com.tmes.graph.Edge;
import com.tmes.graph.Graph;
import com.tmes.graph.Node;

import java.util.*;

/**
 * Implementation of Tarjan's algorithm for finding Strongly Connected Components (SCCs).
 * This algorithm isolates the most robust parts of the graph topology
 * to ensure encryption keys are derived from complex, interconnected sub-structures.
 */
public class TarjanSCC {
    private int idCounter;
    private int sccCount;

    private int[] ids;
    private int[] low;
    private boolean[] onStack;
    private Deque<Integer> stack;

    private final Graph graph;

    public TarjanSCC(Graph graph) {
        this.graph = graph;
        int n = graph.getNodes().size();
        this.ids = new int[n];
        this.low = new int[n];
        this.onStack = new boolean[n];
        this.stack = new ArrayDeque<>();

        // -1 signifies an unvisited node.
        Arrays.fill(ids, -1);
        this.idCounter = 0;
        this.sccCount = 0;
    }

    public List<List<Node>> run() {
        List<List<Node>> sccs = new ArrayList<>();
        List<Node> nodes = graph.getNodes();

        // Loop through all nodes to ensure we don't miss disconnected graph components.
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
                // Case 1: Unvisited node. We recurse deeper into the DFS tree.
                dfs(to, sccs);
                // Upon returning, we propagate the lowest reachable ID (low link) upwards.
                low[at] = Math.min(low[at], low[to]);
            } else if (onStack[to]) {
                // Case 2: Visited node currently on the stack. This is a back edge.
                // We update our low link to the target's ID (not its low-link, to strictly prevent crossing SCC boundaries).
                low[at] = Math.min(low[at], ids[to]);
            }
        }

        // If the current node's ID matches its low link, it is the root of an SCC.
        if (ids[at] == low[at]) {
            List<Node> currentSCC = new ArrayList<>();
            int nodeIndex;

            // Pop nodes from the stack until we extract the root, forming the complete SCC isolated from the rest.
            do {
                nodeIndex = stack.pop();
                onStack[nodeIndex] = false;
                currentSCC.add(graph.getNode(nodeIndex));
            } while (nodeIndex != at);

            sccs.add(currentSCC);
            sccCount++;
        }
    }

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