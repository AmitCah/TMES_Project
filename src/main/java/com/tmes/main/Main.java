package com.tmes.main;

import com.tmes.graph.*;
import com.tmes.encryption.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== TMES Interactive Mode ===");

        System.out.println("\n[Security Policy]");
        System.out.println("- Passwords must be at least 8 characters long.");
        System.out.println("- Passwords must be in English characters.");
        System.out.println("- Passwords must contain a diverse mix of characters to ensure a secure topology (avoid repeating patterns like 'AAAA').\n");

        System.out.println("Select operation:");
        System.out.println("1. Encrypt 'test_image.jpg'");
        System.out.println("2. Decrypt 'encrypted_result.png'");
        System.out.print("Choice (1 or 2): ");

        String choice = scanner.nextLine().trim();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        try {
            if (choice.equals("1")) {
                runEncryption(password);
            } else if (choice.equals("2")) {
                runDecryption(password);
            } else {
                System.out.println("Invalid choice. Please run the program again and select 1 or 2.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred during execution:");
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void runEncryption(String password) throws IOException {
        System.out.println("\n[ENCRYPTION MODE]");
        File imgFile = new File("test_image.png");
        if (!imgFile.exists()) {
            System.err.println("ERROR: 'test_image.jpg' not found in the project root.");
            return;
        }

        System.out.println("1. Loading, sanitizing, and padding image...");
        BufferedImage rawImage = ImageIO.read(imgFile);
        BufferedImage safeImage = forceOpaqueRGB(rawImage);
        BufferedImage sourceImage = padToSquare(safeImage);
        int N = sourceImage.getWidth();
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long to generate sufficient topological complexity.");
        }
        System.out.println("2. Deriving keys from password...");
        int[] keys = deriveKeys(password);
        System.out.println("   -> Keys: P=" + keys[0] + ", Q=" + keys[1] + ", K=" + keys[2]);

        System.out.println("3. Encrypting (Arnold's Cat Map + Diffusion)...");
        ArnoldsCatMap engine = new ArnoldsCatMap(keys[0], keys[1], N);
        long start = System.currentTimeMillis();
        BufferedImage encrypted = ImageEncryptor.encrypt(sourceImage, engine, keys[2]);
        long time = System.currentTimeMillis() - start;

        File outFile = new File("encrypted_result.png");
        ImageIO.write(encrypted, "png", outFile);
        System.out.println("Done in " + time + "ms. Saved to: " + outFile.getAbsolutePath());
    }

    private static void runDecryption(String password) throws IOException {
        System.out.println("\n[DECRYPTION MODE]");
        File imgFile = new File("encrypted_result.png");
        if (!imgFile.exists()) {
            System.err.println("ERROR: 'encrypted_result.png' not found. Encrypt an image first.");
            return;
        }

        System.out.println("1. Loading encrypted image...");
        BufferedImage rawEncryptedImage = ImageIO.read(imgFile);

        // THIS IS THE FIX: Convert the byte-backed image to an int-backed image
        BufferedImage encryptedImage = forceOpaqueRGB(rawEncryptedImage);

        int N = encryptedImage.getWidth();

        System.out.println("2. Deriving keys from password...");
        int[] keys = deriveKeys(password);
        System.out.println("   -> Keys generated: P=" + keys[0] + ", Q=" + keys[1] + ", K=" + keys[2]);

        System.out.println("3. Decrypting...");
        ArnoldsCatMap engine = new ArnoldsCatMap(keys[0], keys[1], N);
        long start = System.currentTimeMillis();
        BufferedImage decrypted = ImageEncryptor.decrypt(encryptedImage, engine, keys[2]);
        long time = System.currentTimeMillis() - start;

        File outFile = new File("decrypted_result.png");
        ImageIO.write(decrypted, "png", outFile);
        System.out.println("Done in " + time + "ms. Saved to: " + outFile.getAbsolutePath());
    }

    // Helper method to simulate a fresh key derivation from a password
    private static int[] deriveKeys(String password) {
        Graph graph = GraphBuilder.buildBaseLayer(password, 4);
        GraphBuilder.addShortcutLayer(graph);

        TarjanSCC tarjan = new TarjanSCC(graph);
        List<Node> largestSCC = TarjanSCC.getLargestSCC(tarjan.run());
        if (largestSCC.size() < 2) {
            throw new RuntimeException("Cryptographic Exception: The provided password lacks the structural complexity required to generate a secure encryption key. Please include a wider variety of unique characters, numbers, or symbols.");        }
        Graph coreGraph = GraphBuilder.createSubgraph(largestSCC);

        Node source = coreGraph.getNodes().getFirst();
        Node sink = coreGraph.getNodes().getLast();

        // Run Optimization
        coreGraph = TopologyOptimizer.optimize(coreGraph, source, sink, largestSCC.size());

        // Extract Final Keys
        FlowResult flow = EdmondsKarp.compute(coreGraph, source, sink);
        int P = flow.maxFlow;
        int Q = EdmondsKarp.calculateMinCutHash(flow.minCutEdges);
        int K = (largestSCC.size() + P) % 20 + 5;

        return new int[]{P, Q, K};
    }

    // Point 3: Pad to square to prevent data loss
    private static BufferedImage padToSquare(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int maxDim = Math.max(w, h);

        BufferedImage squareImg = new BufferedImage(maxDim, maxDim, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = squareImg.createGraphics();

        int x = (maxDim - w) / 2;
        int y = (maxDim - h) / 2;

        g2d.drawImage(img, x, y, null);
        g2d.dispose();

        return squareImg;
    }

    // Point 4: Force Opaque RGB to prevent alpha channel corruption
    private static BufferedImage forceOpaqueRGB(BufferedImage img) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(img, 0, 0, java.awt.Color.BLACK, null);
        g2d.dispose();
        return rgbImage;
    }

}