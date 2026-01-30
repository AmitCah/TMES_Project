package com.tmes.main;

import com.tmes.graph.*;
import com.tmes.encryption.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== TMES Real Image Test ===");

        try {
            // --- Step 0: Load Image ---
            String filename = "test_image.png"; // MAKE SURE THIS FILE EXISTS IN PROJECT ROOT
            File imgFile = new File(filename);

            if (!imgFile.exists()) {
                System.err.println("ERROR: Could not find file: " + imgFile.getAbsolutePath());
                System.err.println("Please place 'test_image.jpg' in the project root folder.");
                return;
            }

            System.out.println("[0] Loading image...");
            BufferedImage rawImage = ImageIO.read(imgFile);

            // Critical: ACM requires a square image. We crop to the center.
            BufferedImage sourceImage = forceSquare(rawImage);
            int N = sourceImage.getWidth();
            System.out.println("    Image Loaded & Cropped to: " + N + "x" + N);

            // Save the cropped source so you can compare exactly
            ImageIO.write(sourceImage, "png", new File("1_source_cropped.png"));


            // --- Step 1: Graph & Keys ---
            String password = "MySecurePassword2026!";
            System.out.println("[1] Generating Keys from password: " + password);

            // Build Graph
            Graph graph = GraphBuilder.buildBaseLayer(password, 4);
            GraphBuilder.addShortcutLayer(graph);

            // Filter Topology
            TarjanSCC tarjan = new TarjanSCC(graph);
            List<Node> largestSCC = TarjanSCC.getLargestSCC(tarjan.run());
            Graph coreGraph = createSubgraph(largestSCC);

            // Calculate Keys
            Node source = coreGraph.getNodes().get(0);
            Node sink = coreGraph.getNodes().get(coreGraph.getNodes().size() - 1);
            FlowResult flow = EdmondsKarp.compute(coreGraph, source, sink);

            int P = flow.maxFlow;
            int Q = calculateQ(flow.minCutEdges);
            int K = (largestSCC.size() + P) % 20 + 5; // Iterations

            System.out.println("    Keys: P=" + P + ", Q=" + Q + ", K=" + K);


            // --- Step 2: Encryption ---
            System.out.println("[2] Encrypting...");
            ArnoldsCatMap engine = new ArnoldsCatMap(P, Q, N);

            long start = System.currentTimeMillis();
            BufferedImage encrypted = ImageEncryptor.encrypt(sourceImage, engine, K);
            long time = System.currentTimeMillis() - start;

            System.out.println("    Encryption done in " + time + "ms");
            ImageIO.write(encrypted, "png", new File("2_encrypted_result.png"));
            System.out.println("    Saved to '2_encrypted_result.png'");


            // --- Step 3: Decryption ---
            System.out.println("[3] Decrypting...");
            BufferedImage decrypted = ImageEncryptor.decrypt(encrypted, engine, K);

            ImageIO.write(decrypted, "png", new File("3_decrypted_result.png"));
            System.out.println("    Saved to '3_decrypted_result.png'");

            System.out.println("=== Test Complete. Check your project folder! ===");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Helpers ---

    private static BufferedImage forceSquare(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int dim = Math.min(w, h); // Use smallest dimension

        // Crop center
        int x = (w - dim) / 2;
        int y = (h - dim) / 2;

        return img.getSubimage(x, y, dim, dim);
    }

    private static int calculateQ(List<Edge> edges) {
        int sum = 0;
        for (Edge e : edges) sum += e.getWeight();
        return (sum == 0) ? 1 : sum;
    }

    // Your safe version of createSubgraph
    private static Graph createSubgraph(List<Node> validNodes) {
        Graph g = new Graph();
        g.getNodes().addAll(validNodes);
        java.util.Set<Node> validSet = new java.util.HashSet<>(validNodes);
        for (Node n : g.getNodes()) {
            java.util.Iterator<Edge> it = n.getEdges().iterator();
            while (it.hasNext()) {
                Edge e = it.next();
                if (!validSet.contains(e.getDestination())) {
                    it.remove();
                }
            }
        }
        return g;
    }
}