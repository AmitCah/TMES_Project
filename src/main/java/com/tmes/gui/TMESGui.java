package com.tmes.gui;

import com.tmes.encryption.ArnoldsCatMap;
import com.tmes.encryption.ImageEncryptor;
import com.tmes.utils.PasswordGen;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * The graphical user interface for the Topological Multimedia Encryption System.
 * Manages user inputs, displays visual feedback (images), and redirects system logs
 * to a built-in text console. All heavy cryptography runs on separate background threads
 * to prevent the UI from freezing.
 */
public class TMESGui extends JFrame {

    private File selectedInputFile;
    private JLabel imageLabel;
    private JTextArea logArea;
    private JTextField passwordField;

    /**
     * Initializes the main application window, configures layout constraints,
     * and binds action listeners to user interaction events.
     */
    public TMESGui() {
        setTitle("TMES - Topological Multimedia Encryption System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL: Controls ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton btnSelectFile = new JButton("Select Image");
        JLabel lblFileName = new JLabel("No file selected");

        controlPanel.add(btnSelectFile);
        controlPanel.add(lblFileName);

        controlPanel.add(new JLabel("Password:"));
        passwordField = new JTextField(15);
        controlPanel.add(passwordField);

        JButton btnEncrypt = new JButton("Encrypt");
        JButton btnDecrypt = new JButton("Decrypt");
        controlPanel.add(btnEncrypt);
        controlPanel.add(btnDecrypt);

        // --- DEBUG: Auto-fill Password with Dynamic Length ---
        controlPanel.add(new JLabel("Password length:"));

        // JSpinner allows you to select a length. Default: 16, Min: 8, Max: 128, Step: 1
        JSpinner passLengthSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 128, 1));

        // Lock the spinner's text box size so it doesn't stretch the UI
        passLengthSpinner.setPreferredSize(new Dimension(50, 25));
        controlPanel.add(passLengthSpinner);

        JButton btnDebugPass = new JButton("Gen Pass");
        controlPanel.add(btnDebugPass);

        btnDebugPass.addActionListener(e -> {
            // Extract the requested length from the spinner and generate the password
            int requestedLength = (Integer) passLengthSpinner.getValue();
            passwordField.setText(PasswordGen.genPassword(requestedLength));
        });

        add(controlPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: Image Display ---
        imageLabel = new JLabel("Output Image Will Appear Here", SwingConstants.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);
        add(imageScrollPane, BorderLayout.CENTER);

        // --- BOTTOM PANEL: Live Logs ---
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(logArea);
        add(logScroll, BorderLayout.SOUTH);

        // Redirect console output to the internal log panel
        redirectSystemStreams();

        // --- ACTION LISTENERS ---
        btnSelectFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(new File("."));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedInputFile = fileChooser.getSelectedFile();
                lblFileName.setText(selectedInputFile.getName());
                logArea.append("Selected file: " + selectedInputFile.getAbsolutePath() + "\n");
                displayImage(selectedInputFile);
            }
        });

        btnEncrypt.addActionListener(e -> processImage(true));
        btnDecrypt.addActionListener(e -> processImage(false));
    }

    /**
     * Validates user inputs before passing the workload to a background thread.
     * Prevents UI lockups during complex graph generations and matrix calculations.
     *
     * @param isEncrypting True to run encryption, false to run decryption.
     */
    private void processImage(boolean isEncrypting) {
        String password = passwordField.getText();
        if (selectedInputFile == null || !selectedInputFile.exists()) {
            System.err.println("ERROR: Please select a valid input file first.");
            return;
        }
        if (password.length() < 8) {
            System.err.println("ERROR: Password must be at least 8 characters long.");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("--------------------------------------------------");
                if (isEncrypting) {
                    runEncryption(selectedInputFile, password);
                } else {
                    runDecryption(selectedInputFile, password);
                }
            } catch (Exception ex) {
                System.err.println("FATAL ERROR: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    /**
     * Executes the full encryption pipeline.
     */
    private void runEncryption(File imgFile, String password) throws IOException {
        System.out.println("[ENCRYPTION MODE]");
        BufferedImage rawImage = ImageIO.read(imgFile);

        BufferedImage safeImage = com.tmes.main.Main.forceOpaqueRGB(rawImage);
        BufferedImage sourceImage = com.tmes.main.Main.padToSquare(safeImage);
        int N = sourceImage.getWidth();

        int[] keys = com.tmes.main.Main.deriveKeys(password);
        System.out.println("   -> Keys generated: P=" + keys[0] + ", Q=" + keys[1] + ", K=" + keys[2]);

        ArnoldsCatMap engine = new ArnoldsCatMap(keys[0], keys[1], N);
        long start = System.currentTimeMillis();
        BufferedImage encrypted = ImageEncryptor.encrypt(sourceImage, engine, keys[2]);
        long time = System.currentTimeMillis() - start;

        File outFile = new File("encrypted_result.png");
        ImageIO.write(encrypted, "png", outFile);
        System.out.println("Done in " + time + "ms. Saved to: " + outFile.getAbsolutePath());

        SwingUtilities.invokeLater(() -> displayImage(outFile));
    }

    /**
     * Executes the full decryption pipeline.
     */
    private void runDecryption(File imgFile, String password) throws IOException {
        System.out.println("[DECRYPTION MODE]");
        BufferedImage rawEncryptedImage = ImageIO.read(imgFile);
        if (rawEncryptedImage.getWidth() != rawEncryptedImage.getHeight()) {
            System.err.println("ERROR: Invalid file. Encrypted images must be perfectly square.");
            return; // Kill the process safely before the math starts
        }
        BufferedImage encryptedImage = com.tmes.main.Main.forceOpaqueRGB(rawEncryptedImage);
        int N = encryptedImage.getWidth();

        int[] keys = com.tmes.main.Main.deriveKeys(password);
        System.out.println("   -> Keys generated: P=" + keys[0] + ", Q=" + keys[1] + ", K=" + keys[2]);

        ArnoldsCatMap engine = new ArnoldsCatMap(keys[0], keys[1], N);
        long start = System.currentTimeMillis();
        BufferedImage decrypted = ImageEncryptor.decrypt(encryptedImage, engine, keys[2]);
        long time = System.currentTimeMillis() - start;

        File outFile = new File("decrypted_result.png");
        ImageIO.write(decrypted, "png", outFile);
        System.out.println("Done in " + time + "ms. Saved to: " + outFile.getAbsolutePath());

        SwingUtilities.invokeLater(() -> displayImage(outFile));
    }
    /**
     * Loads an image file, dynamically scales it to fit the GUI constraints,
     * and updates the main display panel.
     */
    private void displayImage(File file) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img.getWidth() > 800 || img.getHeight() > 400) {
                    Image scaled = img.getScaledInstance(800, -1, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaled));
                } else {
                    imageLabel.setIcon(new ImageIcon(img));
                }
                imageLabel.setText("");
            } catch (IOException e) {
                System.err.println("Failed to display image in GUI.");
            }
    }

    /**
     * Captures standard console output (System.out) and error streams (System.err)
     * and redirects them into the GUI's log text area.
     */
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                updateTextArea(String.valueOf((char) b));
            }
            @Override
            public void write(byte[] b, int off, int len) {
                updateTextArea(new String(b, off, len));
            }
            private void updateTextArea(String text) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(text);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
}