package com.tmes.encryption;

import com.tmes.graph.Edge;
import com.tmes.graph.Graph;
import com.tmes.graph.Node;

import java.util.List;

/**
 * Dynamically mutates the graph topology (unifying disconnected components,
 * upgrading bottlenecks, and injecting edges) until the cryptographic
 * requirements for P, Q, and K are met.
 */
public class TopologyOptimizer {

    // Hard limit to prevent infinite loops (Big O bound) and guarantee algorithm termination.
    private static final int MAX_ITERATIONS = 10;

    // Mathematically forces the graph to either have multiple parallel paths or one heavily upgraded bottleneck.
    private static final int TARGET_P = 300;

    // Forces the optimizer to actively build "bypass" edges if the natural bottleneck characters are too similar.
    private static final int TARGET_Q = 150;

    // Forces the modulo shift action to bump P until the remainder pushes K into a high-entropy range.
    private static final int TARGET_K = 18;

    /**
     * Iteratively evaluates and mutates the graph until security thresholds are achieved.
     * @return The structurally optimized Graph.
     * Complexity: O(V * E^2)
     */
    public static int[] optimize(Graph graph, Node source, Node sink, int sccSize, List<List<Node>> disconnectedSccs) {        System.out.println("\n--- Starting Topology Optimization ---");

        FlowResult currentFlow = EdmondsKarp.compute(graph, source, sink);
        int bestP = currentFlow.maxFlow;
        int bestQ = EdmondsKarp.calculateMinCutHash(currentFlow.minCutEdges);
        int currentCoreSize = sccSize;
        int bestK = calculateK(currentCoreSize, bestP);

        System.out.println("Initial State -> P: " + bestP + ", Q: " + bestQ + ", K: " + bestK);

        int iter = 1;
        boolean limitsMet = false;
        boolean canOptimize = true;

        // Core loop: Runs until targets are met, options are exhausted, or the hard limit is reached.
        while (iter <= MAX_ITERATIONS && !limitsMet && canOptimize) {

            if (bestP >= TARGET_P && bestQ >= TARGET_Q && bestK >= TARGET_K) {
                System.out.println("Optimization Iteration " + iter + ": All security thresholds met. Exiting early.");
                limitsMet = true;
            } else {
                Runnable undoAction = null;
                String actionName = "";
                int tempCoreSize = currentCoreSize;

                // Priority 1: If flow is low, and we have isolated graph islands, absorb them to create complex new routes.
                if (bestP < TARGET_P && !disconnectedSccs.isEmpty()) {
                    actionName = "Unify Components (Target: P & Topology)";

                    List<Node> island = disconnectedSccs.remove(0);
                    Node islandEntry = island.get(0);
                    Node islandExit = island.get(island.size() - 1);

                    // Bridge the main graph to the island with high-capacity edges to encourage flow diversion.
                    source.addEdge(islandEntry, 50);
                    islandExit.addEdge(sink, 50);

                    tempCoreSize += island.size();

                    // Store the exact state to safely rollback if this mutation decreases overall security.
                    final List<Node> finalIsland = island;
                    undoAction = () -> {
                        source.getEdges().removeIf(e -> e.getDestination().equals(islandEntry));
                        islandExit.getEdges().removeIf(e -> e.getDestination().equals(sink));
                        disconnectedSccs.add(0, finalIsland);
                    };

                    // Priority 2: If no islands remain but flow is still low, brute-force upgrade the strict mathematical bottleneck.
                } else if (bestP < TARGET_P) {
                    actionName = "Upgrade Bottleneck (Target: P)";
                    Edge targetEdge = getDeterministicBottleneck(currentFlow.minCutEdges);
                    if (targetEdge != null) {
                        targetEdge.setWeight(targetEdge.getWeight() + 10);
                        undoAction = () -> targetEdge.setWeight(targetEdge.getWeight() - 10);
                    }

                    // Priority 3: Flow is good, but the Min-Cut string hash (Q) is too weak. Inject bypass edges to force a new cut.
                } else if (bestQ < TARGET_Q) {
                    actionName = "Add Q-Boost Edge (Target: Q)";
                    Node bestSSetNode = null;
                    int maxXor = -1;

                    for (Edge e : currentFlow.minCutEdges) {
                        Node u = e.getSource();
                        boolean hasEdgeToSink = false;

                        for (int i = 0; i < u.getEdges().size() && !hasEdgeToSink; i++) {
                            if (u.getEdges().get(i).getDestination().equals(sink)) {
                                hasEdgeToSink = true;
                            }
                        }

                        // We only want nodes that don't already jump straight to the sink,
                        // and we pick the one that yields the highest XOR difference to maximize Q.
                        if (!hasEdgeToSink) {
                            int xorVal = calculateXor(u, sink);
                            if (xorVal > maxXor) {
                                maxXor = xorVal;
                                bestSSetNode = u;
                            }
                        }
                    }

                    if (bestSSetNode == null) {
                        bestSSetNode = source;
                    }

                    bestSSetNode.addEdge(sink, 1);
                    final Node targetForUndo = bestSSetNode;
                    undoAction = () -> targetForUndo.getEdges().remove(targetForUndo.getEdges().size() - 1);

                    // Priority 4: Micro-adjust capacities to manipulate the modulo arithmetic generating K.
                } else if (bestK < TARGET_K) {
                    actionName = "Modulo Shift (Target: K)";
                    Edge targetEdge = getDeterministicBottleneck(currentFlow.minCutEdges);
                    if (targetEdge != null) {
                        targetEdge.setWeight(targetEdge.getWeight() + 1);
                        undoAction = () -> targetEdge.setWeight(targetEdge.getWeight() - 1);
                    }
                }

                if (undoAction == null) {
                    System.out.println("Optimization Iteration " + iter + ": No valid action found. Halting.");
                    canOptimize = false;
                } else {
                    // Evaluate the mutated graph in a "sandbox" calculation.
                    FlowResult newFlow = EdmondsKarp.compute(graph, source, sink);
                    int newP = newFlow.maxFlow;
                    int newQ = EdmondsKarp.calculateMinCutHash(newFlow.minCutEdges);
                    int newK = calculateK(tempCoreSize, newP);

                    // Compare weighted scores to ensure we are strictly climbing towards the targets (Hill Climbing algorithm).
                    int oldScore = calculateStateScore(bestP, bestQ, bestK);
                    int newScore = calculateStateScore(newP, newQ, newK);

                    if (newScore > oldScore) {
                        System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: SUCCESS -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                        bestP = newP;
                        bestQ = newQ;
                        bestK = newK;
                        currentCoreSize = tempCoreSize;
                        currentFlow = newFlow;
                    } else {
                        System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: FAILED (Reverting) -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                        undoAction.run();
                    }
                }
            }
            iter++;
        }

        System.out.println("--- Optimization Complete ---\n");
        return new int[]{bestP, bestQ, bestK}; // Return the synchronized keys;
    }

    /** Calculates the K parameter based on core topology size and flow capacity. */
    private static int calculateK(int sccSize, int p) {
        return (sccSize + p) % 20 + 5;
    }

    /** Computes the bitwise XOR sum between two strings. */
    private static int calculateXor(Node n1, Node n2) {
        int val = 0;
        int minLen = Math.min(n1.getData().length(), n2.getData().length());
        for (int i = 0; i < minLen; i++) {
            val += n1.getData().charAt(i) ^ n2.getData().charAt(i);
        }
        return val;
    }

    /** Generates a weighted integer score to compare graph states. */
    private static int calculateStateScore(int p, int q, int k) {
        int score = 0;
        // Heavy weights ensure the algorithm prioritizes reaching the thresholds first.
        score += Math.min(p, TARGET_P) * 10000;
        score += Math.min(q, TARGET_Q) * 100;
        score += Math.min(k, TARGET_K) * 1000;

        // Raw additions act as tie-breakers to prevent optimization plateaus.
        score += p;
        score += q;
        return score;
    }

    /** Scans the minimum cut to find a critical edge based on node ASCII sums. */
    private static Edge getDeterministicBottleneck(List<Edge> minCutEdges) {
        if (minCutEdges == null || minCutEdges.isEmpty()) return null;
        Edge bestEdge = minCutEdges.get(0);
        int maxScore = -1;
        for (Edge edge : minCutEdges) {
            int score = calculateNodeScore(edge.getSource()) + calculateNodeScore(edge.getDestination());
            if (score > maxScore) {
                maxScore = score;
                bestEdge = edge;
            }
        }
        return bestEdge;
    }

    /** Sums the ASCII values of a node's K-mer string. */
    private static int calculateNodeScore(Node node) {
        int score = 0;
        for (char c : node.getData().toCharArray()) {
            score += c;
        }
        return score;
    }
}