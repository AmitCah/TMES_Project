package com.tmes.main;

import com.tmes.graph.*;
import com.tmes.encryption.*;
import com.tmes.gui.TMESGui;

import javax.imageio.ImageIO;
import javax.swing.*;
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
        System.out.println("- Passwords must contain a diverse mix of characters to ensure a secure topology.\n");

        System.out.println("Select operation:");
        System.out.println("1. Run CLI Encryption");
        System.out.println("2. Run CLI Decryption");
        System.out.println("3. Launch Graphic User Interface (GUI)");
        System.out.print("Choice (1, 2, or 3): ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("3")) {
            System.out.println("Launching GUI...");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> new TMESGui().setVisible(true));
            return; // Exit the CLI thread
        }

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        try {
            if (choice.equals("1")) {
                runEncryption(password);
            } else if (choice.equals("2")) {
                runDecryption(password);
            } else {
                System.out.println("Invalid choice. Please run the program again.");
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
            System.err.println("ERROR: 'test_image.png' not found in the project root.");
            return;
        }

        System.out.println("1. Loading, sanitizing, and padding image...");
        BufferedImage rawImage = ImageIO.read(imgFile);
        BufferedImage safeImage = forceOpaqueRGB(rawImage);
        BufferedImage sourceImage = padToSquare(safeImage);
        int N = sourceImage.getWidth();
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
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
            System.err.println("ERROR: 'encrypted_result.png' not found.");
            return;
        }

        System.out.println("1. Loading encrypted image...");
        BufferedImage rawEncryptedImage = ImageIO.read(imgFile);
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

    // --- NOW PUBLIC UTILITY METHODS ---

    public static int[] deriveKeys(String password) {
        Graph graph = GraphBuilder.buildBaseLayer(password, 4);
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

        graph = TopologyOptimizer.optimize(graph, source, sink, largestSCC.size(), disconnectedSccs);

        FlowResult flow = EdmondsKarp.compute(graph, source, sink);
        int P = flow.getMaxFlow();
        int Q = EdmondsKarp.calculateMinCutHash(flow.getMinCutEdges());
        int K = (largestSCC.size() + P) % 20 + 5;

        return new int[]{P, Q, K};
    }

    public static BufferedImage padToSquare(BufferedImage img) {
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

    public static BufferedImage forceOpaqueRGB(BufferedImage img) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(img, 0, 0, java.awt.Color.BLACK, null);
        g2d.dispose();
        return rgbImage;
    }
}