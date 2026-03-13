package com.tmes.graph;

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

    public static Graph optimize(Graph graph, Node source, Node sink, int sccSize) {
        System.out.println("\n--- Starting Topology Optimization ---");

        // 1. Initial State Evaluation
        FlowResult currentFlow = EdmondsKarp.compute(graph, source, sink);
        int bestP = currentFlow.maxFlow;
        int bestQ = EdmondsKarp.calculateMinCutHash(currentFlow.minCutEdges);
        int bestK = calculateK(sccSize, bestP);

        System.out.println("Initial State -> P: " + bestP + ", Q: " + bestQ + ", K: " + bestK);

        // 2. The Simulation Loop
        for (int iter = 1; iter <= MAX_ITERATIONS; iter++) {

            // Check if we hit all targets
            if (bestP >= TARGET_P && bestQ >= TARGET_Q && bestK >= TARGET_K) {
                System.out.println("Optimization Iteration " + iter + ": All security thresholds met. Exiting early.");
                break;
            }

            Runnable undoAction = null;
            String actionName = "";

            // 3. Decision Tree (P has
            // the highest priority, then Q, then K)
            if (bestP < TARGET_P) {
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

                // Find a node from the current Min-Cut sources that yields the highest XOR with the sink
                for (Edge e : currentFlow.minCutEdges) {
                    Node u = e.getSource();

                    // Ensure it doesn't already have a direct edge to the sink to prevent overwriting capacities
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

                // Failsafe: if all bottleneck sources already connect to sink, use the main graph source
                if (bestSSetNode == null) {
                    bestSSetNode = source;
                }

                // A capacity of 1 guarantees it saturates instantly and becomes a Min-Cut edge
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

            // Failsafe if graph is completely disconnected or empty
            if (undoAction == null) {
                System.out.println("Optimization Iteration " + iter + ": No valid action found. Halting.");
                break;
            }

            // 4. Evaluate Sandbox (Test the modification)
            FlowResult newFlow = EdmondsKarp.compute(graph, source, sink);
            int newP = newFlow.maxFlow;
            int newQ = EdmondsKarp.calculateMinCutHash(newFlow.minCutEdges);
            int newK = calculateK(sccSize, newP);

            // 5. Compare States (State-Space Search Evaluation)
            int oldScore = calculateStateScore(bestP, bestQ, bestK);
            int newScore = calculateStateScore(newP, newQ, newK);

            if (newScore > oldScore) {
                System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: SUCCESS -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                // Lock in the changes
                bestP = newP;
                bestQ = newQ;
                bestK = newK;
                currentFlow = newFlow; // Update bottleneck data for next iteration
            } else {
                System.out.println("Optimization Iteration " + iter + " [" + actionName + "]: FAILED (Reverting) -> P: " + newP + ", Q: " + newQ + ", K: " + newK);
                // Rollback using the O(1) undo
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

    private static Node getMaxAsciiNode(Graph graph) {
        Node best = null;
        int max = -1;
        for (Node n : graph.getNodes()) {
            int score = calculateNodeScore(n);
            if (score > max) { max = score; best = n; }
        }
        return best;
    }

    private static Node getMinAsciiNode(Graph graph) {
        Node best = null;
        int min = Integer.MAX_VALUE;
        for (Node n : graph.getNodes()) {
            int score = calculateNodeScore(n);
            if (score < min) { min = score; best = n; }
        }
        return best;
    }

    private static int calculateNodeScore(Node node) {
        int score = 0;
        for (char c : node.getData().toCharArray()) {
            score += c;
        }
        return score;
    }
}