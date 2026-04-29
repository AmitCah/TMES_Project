package com.tmes.encryption;

import com.tmes.graph.Edge;

import java.util.List;

public class FlowResult {
    public final int maxFlow;
    public final List<Edge> minCutEdges;

    public FlowResult(int maxFlow, List<Edge> minCutEdges) {
        this.maxFlow = maxFlow;
        this.minCutEdges = minCutEdges;
    }


}