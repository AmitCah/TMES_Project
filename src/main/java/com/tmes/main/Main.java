package com.tmes.main;

import com.tmes.graph.*;
import com.tmes.encryption.*;
import com.tmes.gui.TMESGui;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The Entry point for the Topological Multimedia Encryption System (TMES).
 * This class launches the Graphical User Interface (GUI) and serves as the main
 * coordinator for the cryptographic pipeline, connecting the graph topology
 * generation, key derivation, and image processing modules.
 */
public class Main {

    /**
     * The main execution thread.
     * Initializes the system Look and Feel and launches the Graphic User Interface.
     *
     * @param args Command line arguments (unused).
     */
    public static void main(String[] args) {
        System.out.println("=== TMES GUI Mode Initialization ===");
        try {
            // Attempts to match the GUI's visual style to the host operating system.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Hands off execution to the Swing Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> new TMESGui().setVisible(true));
    }

    /**
     * Translates a user password into actionable cryptographic parameters (P, Q, and K).
     * Builds a De Bruijn inspired graph, isolates the most robust Strongly Connected Component (SCC),
     * optimizes the topology, and calculates network flow metrics.
     *
     * @param password The user's raw text password.
     * @return An integer array containing exactly three elements: [P, Q, K].
     * @throws RuntimeException If the password creates a graph too weak to form a valid SCC.
     * Complexity: O(V * E^2)
     */
    public static int[] deriveKeys(String password) {
        // Scales the De Bruijn window via Base-2 logarithm (Shannon Entropy) to prevent graph collapse and balance cyclic overlap.
        int kmerSize = Math.max(2, (int) (Math.log(password.length()) / Math.log(2)));

        Graph graph = GraphBuilder.buildBaseLayer(password, kmerSize);
        GraphBuilder.addShortcutLayer(graph);

        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Node>> allSccs = tarjan.run();
        List<Node> largestSCC = TarjanSCC.getLargestSCC(allSccs);

        if (largestSCC.size() < 2) {
            throw new RuntimeException("Cryptographic Exception: The provided password lacks structural complexity.");
        }

        Node source = largestSCC.getFirst();
        Node sink = largestSCC.getLast();

        List<List<Node>> disconnectedSccs = new java.util.ArrayList<>();
        for (List<Node> scc : allSccs) {
            if (scc != largestSCC) {
                disconnectedSccs.add(scc);
            }
        }

        int[] finalKeys = TopologyOptimizer.optimize(graph, source, sink, largestSCC.size(), disconnectedSccs, password.length());

        return finalKeys;
    }

    /**
     * Ensures an image is a perfect square, which is a strict mathematical requirement
     * for the Arnold's Cat Map transformation.
     * If the image is rectangular, it creates
     * a square boundary based on the longest dimension and centers the original image inside it.
     *
     * @param img The original, potentially rectangular BufferedImage.
     * @return A perfectly square BufferedImage containing the original data centered.
     * Complexity: O(N^2)
     */
    public static BufferedImage padToSquare(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 1. Find the longest side
        int maxDim = Math.max(w, h);
        // 2. CRYPTOGRAPHIC PADDING: Enforce a minimum dimension of 128
        maxDim = Math.max(maxDim, 128);

        BufferedImage squareImg = new BufferedImage(maxDim, maxDim, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = squareImg.createGraphics();

        // Center the original image inside the new, safe-sized matrix
        int x = (maxDim - w) / 2;
        int y = (maxDim - h) / 2;

        g2d.drawImage(img, x, y, null);
        g2d.dispose();

        return squareImg;
    }

    /**
     * Strips the Alpha (transparency) channel from an image.
     * Transparent pixels can cause XOR masking algorithms to output invisible data,
     * leading to irreversible data loss during decryption.
     * This forces a solid RGB background.
     *
     * @param img The raw image loaded from the file system.
     * @return An opaque BufferedImage strictly containing standard RGB data.
     * Complexity: O(W * H)
     */
    public static BufferedImage forceOpaqueRGB(BufferedImage img) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(img, 0, 0, java.awt.Color.BLACK, null);
        g2d.dispose();
        return rgbImage;
    }
}