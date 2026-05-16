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

    /**
     * Iteratively evaluates and mutates the graph until security thresholds are achieved.
     * @return The structurally optimized Graph keys [P, Q, K].
     * Complexity: O(V * E^2 * L). L is the password's length
     */
    public static int[] optimize(Graph graph, Node source, Node sink, int sccSize, List<List<Node>> disconnectedSccs, int passLen) {
        System.out.println("\n--- Starting Topology Optimization ---");
        // Demands 10% of maximum theoretical ASCII edge capacity (252) per character to force proportional structural expansion.
        final int TARGET_P = passLen * 25;
        // Set exactly above the absolute XOR maximum of homogeneous lowercase letters (31) to mathematically force cross-domain (letter-digit) mutations.
        final int TARGET_Q = passLen * 32;
        // Guarantees a minimum of 15 iterations (min passLen 8 + 7), exceeding the 14-round AES-256 standard to ensure total visual diffusion.
        final int TARGET_K = passLen + 7;
        // Acts as an O(V*E^2) execution circuit breaker that scales with input entropy to preserve topological sparsity.
        final int MAX_ITERATIONS = Math.max(10, passLen);

        FlowResult currentFlow = EdmondsKarp.compute(graph, source, sink);
        int bestP = currentFlow.maxFlow;
        int bestQ = EdmondsKarp.calculateMinCutHash(currentFlow.minCutEdges);
        int currentCoreSize = sccSize;
        int bestK = calculateK(currentCoreSize, bestP);

        System.out.println("Initial State -> P: " + bestP + ", Q: " + bestQ + ", K: " + bestK);
        System.out.println("Target: P=" + TARGET_P + ", Q=" + TARGET_Q + ", K=" + TARGET_K + ", MAX_ITERATIONS=" + MAX_ITERATIONS);

        int iter = 1;
        boolean limitsMet = false;
        boolean canOptimize = true;

        // Core loop: Runs until targets are met, options are exhausted, or the dynamic limit is reached.
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
                    int oldScore = calculateStateScore(bestP, bestQ, bestK, TARGET_P, TARGET_Q, TARGET_K);
                    int newScore = calculateStateScore(newP, newQ, newK, TARGET_P, TARGET_Q, TARGET_K);

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
        return new int[]{bestP, bestQ, bestK}; // Return the synchronized keys
    }

    /**
     * Calculates the K parameter (number of encryption iterations) based on the core topology size
     * and the maximum flow capacity.
     * Complexity: O(1)
     *
     * @param sccSize The number of nodes in the largest Strongly Connected Component.
     * @param p       The Maximum Flow capacity.
     * @return The derived K value, constrained mathematically between 5 and 24.
     */
    private static int calculateK(int sccSize, int p) {
        return (sccSize + p) % 20 + 5;
    }

    /**
     * Computes the bitwise XOR sum between the K-mer strings of two nodes.
     * Used to quantitatively evaluate the structural difference between topological points.
     * Complexity: O(log L) (Since the K-mer string length scales logarithmically with the password length).
     *
     * @param n1 The first node.
     * @param n2 The second node.
     * @return The computed XOR sum.
     */
    private static int calculateXor(Node n1, Node n2) {
        int val = 0;
        int minLen = Math.min(n1.getData().length(), n2.getData().length());
        for (int i = 0; i < minLen; i++) {
            val += n1.getData().charAt(i) ^ n2.getData().charAt(i);
        }
        return val;
    }

    /**
     * Generates a weighted integer score to evaluate and compare the cryptographic strength
     * of different graph states during optimization.
     * Complexity: O(1)
     *
     * @param p The Maximum Flow metric.
     * @param q The Min-Cut Hash metric.
     * @param k The calculated iterations metric.
     * @param targetP Dynamic target for flow capacity.
     * @param targetQ Dynamic target for Min-Cut Hash.
     * @param targetK Dynamic target for K iterations.
     * @return A weighted score prioritizing threshold achievement, with raw values acting as tie-breakers.
     */
    private static int calculateStateScore(int p, int q, int k, int targetP, int targetQ, int targetK) {
        int score = 0;
        // Heavy weights ensure the algorithm prioritizes reaching the thresholds first.
        score += Math.min(p, targetP) * 10000;
        score += Math.min(q, targetQ) * 100;
        score += Math.min(k, targetK) * 1000;

        // Raw additions act as tie-breakers to prevent optimization plateaus.
        score += p;
        score += q;
        return score;
    }

    /**
     * Scans the minimum cut to find the most critical bottleneck edge based on the ASCII sums
     * of its connected nodes.
     * Complexity: O(C log L) where C is the number of edges in the minimum cut and L is the password length,
     * as calculating the node scores dynamically scales with the logarithmic K-mer window.
     *
     * @param minCutEdges The list of edges forming the minimum cut.
     * @return The selected deterministic bottleneck edge, or null if the list is empty.
     */
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

    /**
     * Computes a primitive topological weight for a node by summing the ASCII values
     * of its K-mer string.
     * Complexity: O(log L) (Bounded by the dynamic K-mer length).
     *
     * @param node The node to evaluate.
     * @return The sum of the ASCII values.
     */
    private static int calculateNodeScore(Node node) {
        int score = 0;
        for (char c : node.getData().toCharArray()) {
            score += c;
        }
        return score;
    }
}