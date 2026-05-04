package com.tmes.encryption;

import java.awt.image.BufferedImage;
import java.awt.Graphics;

/**
 * Handles the actual pixel by pixel encryption and decryption of the image.
 * Uses a "Ping-Pong" buffer system for high-performance memory swapping.
 * Implements Shannon's two properties of secure ciphers:
 * 1. Diffusion: Changing the actual color of the pixel using an XOR mask.
 * 2. Confusion: Moving the pixel to a new location using Arnold's Cat Map.
 */
public class ImageEncryptor {

    /**
     * A private helper class to manage the Ping-Pong buffer state.
     * Centralizes the creation of the dual-image memory structure and extracts
     * the raw 1D pixel arrays required for high-speed read/write operations.
     */
    private static class BufferContext {
        final BufferedImage imgA;
        final BufferedImage imgB;
        final int[] bufferA;
        final int[] bufferB;

        BufferContext(BufferedImage source) {
            int width = source.getWidth();
            int height = source.getHeight();

            // Create exactly TWO image buffers once to save memory.
            this.imgA = deepCopy(source);
            this.imgB = new BufferedImage(width, height, source.getType());

            // Extract the raw 1D integer arrays from the images for extreme read/write speed.
            this.bufferA = ((java.awt.image.DataBufferInt) imgA.getRaster().getDataBuffer()).getData();
            this.bufferB = ((java.awt.image.DataBufferInt) imgB.getRaster().getDataBuffer()).getData();
        }
    }

    /**
     * Encrypts an image by applying diffusion and confusion K times.
     *
     * @param originalImage The source image to encrypt.
     * @param map The initialized Arnold's Cat Map engine.
     * @param k The number of scrambling iterations (derived from graph topology).
     * @return The fully encrypted BufferedImage.
     */
    public static BufferedImage encrypt(BufferedImage originalImage, ArnoldsCatMap map, int k) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Initialize the dual-buffer system for high-speed memory swapping.
        BufferContext context = new BufferContext(originalImage);

        // Pointers for the Ping-Pong swap logic.
        int[] currentBuffer = context.bufferA;
        int[] nextBuffer = context.bufferB;

        for (int i = 0; i < k; i++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int flatIndex = y * width + x;
                    int rgb = currentBuffer[flatIndex];

                    // Separate the Alpha channel (transparency) from the RGB color data.
                    int alpha = rgb & 0xFF000000;
                    int rawColor = rgb & 0x00FFFFFF;

                    // DIFFUSION: Generate a dynamic mask and XOR it against the color.
                    int mask = generateMask(x, y, map.getP(), map.getQ(), i);
                    int scrambledColor = rawColor ^ mask;

                    // CONFUSION: Calculate the new coordinate using the Cat Map.
                    int[] newPos = map.encryptPixel(x, y);
                    //2D array to 1D array = y * width + x
                    int newFlatIndex = newPos[1] * width + newPos[0];

                    // Write the scrambled color to the new location in the destination array.
                    nextBuffer[newFlatIndex] = (alpha | scrambledColor);
                }
            }
            // Swap the memory pointers for the next iteration (Ping-Pong).
            int[] temp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = temp;
        }

        // Return the buffer that holds the final result based on whether K is even or odd.
        return (k % 2 == 0) ? context.imgA : context.imgB;
    }

    /**
     * Decrypts an image by running the encryption loop entirely in reverse.
     *
     * @param encryptedImage The scrambled image to decrypt.
     * @param map The initialized Arnold's Cat Map engine.
     * @param k The number of scrambling iterations used during encryption.
     * @return The restored, decrypted BufferedImage.
     */
    public static BufferedImage decrypt(BufferedImage encryptedImage, ArnoldsCatMap map, int k) {
        int width = encryptedImage.getWidth();
        int height = encryptedImage.getHeight();

        // Initialize the dual-buffer system for high-speed memory swapping.
        BufferContext context = new BufferContext(encryptedImage);

        int[] currentBuffer = context.bufferA;
        int[] nextBuffer = context.bufferB;

        // Loop runs backward (from K-1 down to 0) to perfectly unroll the encryption layers.
        for (int i = k - 1; i >= 0; i--) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int flatIndex = y * width + x;
                    int rgb = currentBuffer[flatIndex];

                    // CONFUSION REVERSAL: Find where this pixel originally came from.
                    int[] oldPos = map.decryptPixel(x, y);
                    int oldFlatIndex = oldPos[1] * width + oldPos[0];

                    int alpha = rgb & 0xFF000000;
                    int scrambledColor = rgb & 0x00FFFFFF;

                    // DIFFUSION REVERSAL: Regenerate the exact same mask using the OLD coordinates.
                    int mask = generateMask(oldPos[0], oldPos[1], map.getP(), map.getQ(), i);

                    // XORing the scrambled color with the same mask restores the original color.
                    int restoredColor = scrambledColor ^ mask;

                    // Place the restored color back in its original position.
                    nextBuffer[oldFlatIndex] = (alpha | restoredColor);
                }
            }
            int[] temp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = temp;
        }

        return (k % 2 == 0) ? context.imgA : context.imgB;
    }

    /**
     * Generates a deterministic, pseudo-random integer mask based on the pixel's location,
     * the cryptographic keys, and the current iteration.
     */
    private static int generateMask(int x, int y, int p, int q, int iteration) {
        // Multipliers (31, 17, 73) are prime numbers used to spread out the bits.
        int hash = (p * (x + 1) * 31) ^ (q * (y + 1) * 17) ^ ((iteration + 1) * p * q * 73);
        // Force the mask to fit within the 24-bit RGB space.
        return hash & 0x00FFFFFF;
    }

    /**
     * Creates an independent, deep copy of a BufferedImage in memory.
     */
    public static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage cm = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        Graphics g = cm.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return cm;
    }
}