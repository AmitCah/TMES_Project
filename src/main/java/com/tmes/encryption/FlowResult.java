package com.tmes.encryption;

import com.tmes.graph.Edge;

import java.util.List;

/**
 * An object that encapsulates the results of the Edmonds-Karp network flow algorithm.
 * It holds both the calculated maximum flow (which serves as the basis for the cryptographic key P)
 * and the edges that make up the minimum cut (used to generate the cryptographic key Q).
 */
public class FlowResult {

    /**
     * The total maximum flow capacity of the network from the source to the sink.
     */
    public final int maxFlow;

    /**
     * The collection of edges that physically restrict the flow, forming the minimum cut.
     */
    public final List<Edge> minCutEdges;

    /**
     * Constructs a new FlowResult instance.
     *
     * @param maxFlow     The maximum flow capacity calculated by the algorithm.
     * @param minCutEdges A list of the bottleneck edges representing the minimum cut.
     */
    public FlowResult(int maxFlow, List<Edge> minCutEdges) {
        this.maxFlow = maxFlow;
        this.minCutEdges = minCutEdges;
    }

}