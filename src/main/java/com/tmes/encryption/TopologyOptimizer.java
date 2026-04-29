package com.tmes.encryption;

import com.tmes.graph.Edge;
import com.tmes.graph.Graph;
import com.tmes.graph.Node;

import java.util.List;

public class TopologyOptimizer {

    // Hard limit to prevent infinite loops (Big O bound)
    private static final int MAX_ITERATIONS = 10;

    // Security Thresholds (Tweak these based on your image size needs)
    private static final int TARGET_P = 300;
    //300 mathematically forces the graph to either have a minimum of two parallel paths of data flowing from source to sink,
    //or one heavily upgraded bottleneck.
    private static final int TARGET_Q = 150;
    //150 is a realistic but strict threshold. It forces the optimizer to actively build "bypass" edges (using Action X)
    //if the natural bottleneck characters are too mathematically similar.
    private static final int TARGET_K = 18;
    //By targeting 18, you force the optimizer's "Modulo Shift" action to slightly bump P until the remainder pushes K into the
    //upper quartile of its possible range (18 to 24).
    //This guarantees aggressive scrambling without causing infinite loops trying to hit an impossible number.

    // Notice the new parameter: List<List<Node>> disconnectedSccs
    public static Graph optimize(Graph graph, Node source, Node sink, int sccSize, List<List<Node>> disconnectedSccs) {
        System.out.println("\n--- Starting Topology Optimization ---");

        // 1. Initial State Evaluation
        FlowResult currentFlow = EdmondsKarp.compute(graph, source, sink);
        int bestP = currentFlow.maxFlow;
        int bestQ = EdmondsKarp.calculateMinCutHash(currentFlow.minCutEdges);

        // We track total integrated nodes to ensure K scales correctly if we unify components
        int currentCoreSize = sccSize;
        int bestK = calculateK(currentCoreSize, bestP);

        System.out.println("Initial State -> P: " + bestP + ", Q: " + bestQ + ", K: " + bestK);

        // 2. The Simulation Loop
        for (int iter = 1; iter <= MAX_ITERATIONS; iter++) {

            if (bestP >= TARGET_P && bestQ >= TARGET_Q && bestK >= TARGET_K) {
                System.out.println("Optimization Iteration " + iter + ": All security thresholds met. Exiting early.");
                break;
            }

            Runnable undoAction = null;
            String actionName = "";
            int tempCoreSize = currentCoreSize;

            // 3. Decision Tree (Unifying components takes priority if P is low)
            if (bestP < TARGET_P && !disconnectedSccs.isEmpty()) {
                actionName = "Unify Components (Target: P & Topology)";

                // Pop the largest disconnected component we have
                List<Node> island = disconnectedSccs.remove(0);
                Node islandEntry = island.get(0);
                Node islandExit = island.get(island.size() - 1);

                // Integrate it into the main flow pipeline
                source.addEdge(islandEntry, 50);
                islandExit.addEdge(sink, 50);

                tempCoreSize += island.size(); // We added these nodes to the core pipeline

                final List<Node> finalIsland = island;
                undoAction = () -> {
                    // Revert the edges
                    source.getEdges().removeIf(e -> e.getDestination().equals(islandEntry));
                    islandExit.getEdges().removeIf(e -> e.getDestination().equals(sink));
                    // Put the island back
                    disconnectedSccs.add(0, finalIsland);
                };

            } else if (bestP < TARGET_P) {
                actionName = "Upgrade Bottleneck (Target: P)";
                Edge targetEdge = getDeterministicBottleneck(currentFlow.minCutEdges);
                if (targetEdge != null) {
                    targetEdge.setWeight(targetEdge.getWeight() + 10);
                    undoAction = () -> targetEdge.setWeight(targetEdge.getWeight() - 10);
                }
            } else if (bestQ < TARGET_Q) {
                actionName = "Add Q-Boost Edge (Target: Q)";
                Node bestSSetNode = null;
                int maxXor = -1;

                for (Edge e : currentFlow.minCutEdges) {
                    Node u = e.getSource();
                    boolean hasEdgeToSink = false;
                    for (Edge existingEdge : u.getEdges()) {
                        if (existingEdge.getDestination().equals(sink)) {
                            hasEdgeToSink = true;
                            break;
                        }
                    }

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
                break;
            }

            // 4. Evaluate Sandbox
            FlowResult newFlow = EdmondsKarp.compute(graph, source, sink);
            int newP = newFlow.maxFlow;
            int newQ = EdmondsKarp.calculateMinCutHash(newFlow.minCutEdges);
            int newK = calculateK(tempCoreSize, newP);

            // 5. Compare States
            int oldScore = calculateStateScore(bestP, bestQ, bestK);
            int newScore = calculateStateScore(newP, newQ, newK);

            if (newScore > oldScore) {
                System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: SUCCESS -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                bestP = newP;
                bestQ = newQ;
                bestK = newK;
                currentCoreSize = tempCoreSize; // Lock in the new core size if we unified components
                currentFlow = newFlow;
            } else {
                System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: FAILED (Reverting) -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                undoAction.run();
            }
        }

        System.out.println("--- Optimization Complete ---\n");
        return graph;
    }

    // --- Helpers ---

    private static int calculateK(int sccSize, int p) {
        return (sccSize + p) % 20 + 5;
    }

    private static int calculateXor(Node n1, Node n2) {
        int val = 0;
        int minLen = Math.min(n1.getData().length(), n2.getData().length());
        for (int i = 0; i < minLen; i++) {
            val += n1.getData().charAt(i) ^ n2.getData().charAt(i);
        }
        return val;
    }

    // Creates a weighted score to evaluate if a state is strictly "better"
    private static int calculateStateScore(int p, int q, int k) {
        int score = 0;
        score += Math.min(p, TARGET_P) * 10000;
        score += Math.min(q, TARGET_Q) * 100;
        score += Math.min(k, TARGET_K) * 1000;

        // Tie-breakers: Reward any raw increase to prevent optimization plateaus
        score += p;
        score += q;

        return score;
    }

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

    private static int calculateNodeScore(Node node) {
        int score = 0;
        for (char c : node.getData().toCharArray()) {
            score += c;
        }
        return score;
    }
}