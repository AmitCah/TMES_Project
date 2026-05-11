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
/**
 * The Entry point for the Topological Multimedia Encryption System (TMES).
 * This class routes the user to either the Command Line Interface (CLI) or the Graphical User Interface (GUI).
 * It also serves as the main coordinator for the cryptographic pipeline, connecting the graph topology
 * generation, key derivation, and image processing modules.
 */
public class Main {

    /**
     * The main execution thread. Presents an interactive console menu enforcing security policies
     * and routes the application flow based on user selection.
     *
     * @param args Command line arguments (unused).
     */
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
                // Attempts to match the GUI's visual style to the host operating system.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            // Hands off execution to the Swing Event Dispatch Thread (EDT).
            SwingUtilities.invokeLater(() -> new TMESGui().setVisible(true));
            return;
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

    /**
     * Coordinates the full encryption sequence in CLI mode.
     * Loads the target image, sanitizes its format, derives cryptographic keys from the password,
     * and passes the data to the encryption engine.
     *
     * @param password The user-provided string used to generate the graph topology.
     * @throws IOException If the target image file cannot be read or written.
     */
    private static void runEncryption(String password) throws IOException {
        System.out.println("\n[ENCRYPTION MODE]");
        File imgFile = new File("test_image.png");
        if (!imgFile.exists()) {
            System.err.println("ERROR: 'test_image.png' not found in the project root.");
            return;
        }

        System.out.println("1. Loading, sanitizing, and padding image...");
        BufferedImage rawImage = ImageIO.read(imgFile);

        // Sanitize the image format before passing it to the mathematical engine.
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

    /**
     * Coordinates the full decryption sequence in CLI mode.
     * Reads the ciphertext image, re-derives the exact keys from the password,
     * and runs the encryption engine in reverse.
     *
     * @param password The user-provided string used to regenerate the graph topology.
     * @throws IOException If the encrypted image file cannot be read or written.
     */
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

    /**
     * Translates a user password into actionable cryptographic parameters (P, Q, and K).
     * Builds a De Bruijn inspired graph, isolates the most robust Strongly Connected Component (SCC),
     * optimizes the topology, and calculates network flow metrics.
     *
     * @param password The user's raw text password.
     * @return An integer array containing exactly three elements: [P, Q, K].
     * @throws RuntimeException If the password creates a graph too weak to form a valid SCC.
     */
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

        // The optimizer mutates the graph in-place and returns the final synchronized keys.
        int[] finalKeys = TopologyOptimizer.optimize(graph, source, sink, largestSCC.size(), disconnectedSccs);

        return finalKeys;
    }

    /**
     * Ensures an image is a perfect square, which is a strict mathematical requirement
     * for the Arnold's Cat Map transformation. If the image is rectangular, it creates
     * a square boundary based on the longest dimension and centers the original image inside it.
     *
     * @param img The original, potentially rectangular BufferedImage.
     * @return A perfectly square BufferedImage containing the original data centered.
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
     * leading to irreversible data loss during decryption. This forces a solid RGB background.
     *
     * @param img The raw image loaded from the file system.
     * @return An opaque BufferedImage strictly containing standard RGB data.
     */
    public static BufferedImage forceOpaqueRGB(BufferedImage img) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(img, 0, 0, java.awt.Color.BLACK, null);
        g2d.dispose();
        return rgbImage;
    }
}