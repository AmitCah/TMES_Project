package com.tmes.main;

import com.tmes.graph.Graph;
import com.tmes.graph.GraphBuilder;
import com.tmes.graph.TarjanSCC;

public class Main {
    static void main(String[] args) {
        String password = "StrongPassword123!";
        int k = 4; // Length of K-mers
        System.out.println("--- Phase 1: Building Base Layer ---");
        Graph graph = GraphBuilder.buildBaseLayer(password, k);
        graph.printGraph();

        System.out.println("\n--- Phase 2: Adding Shortcuts ---");
        GraphBuilder.addShortcutLayer(graph);

        System.out.println("\n--- Final Graph Structure ---");
        graph.printGraph();
        System.out.println("\n--- Tarjan's Algorithms ---");
        TarjanSCC tarjanSCC = new TarjanSCC(graph);
        GraphBuilder.createSubgraph(TarjanSCC.getLargestSCC(tarjanSCC.run()));
    }
}
