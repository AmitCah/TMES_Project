package com.tmes.encryption;

import java.awt.image.BufferedImage;
import java.awt.Graphics;

public class ImageEncryptor {

    public static BufferedImage encrypt(BufferedImage originalImage, ArnoldsCatMap map, int k) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 1. Create exactly TWO images once (Ping-Pong buffers)
        BufferedImage imgA = deepCopy(originalImage);
        BufferedImage imgB = new BufferedImage(width, height, originalImage.getType());

        // 2. Extract their raw 1D arrays for direct memory access
        int[] bufferA = ((java.awt.image.DataBufferInt) imgA.getRaster().getDataBuffer()).getData();
        int[] bufferB = ((java.awt.image.DataBufferInt) imgB.getRaster().getDataBuffer()).getData();

        int[] currentBuffer = bufferA;
        int[] nextBuffer = bufferB;

        for (int i = 0; i < k; i++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int flatIndex = y * width + x;
                    int rgb = currentBuffer[flatIndex];

                    int alpha = rgb & 0xFF000000;
                    int rawColor = rgb & 0x00FFFFFF;

                    // Diffusion phase
                    int mask = generateMask(x, y, map.getP(), map.getQ(), i);
                    int scrambledColor = rawColor ^ mask;

                    // Confusion phase (move the pixel)
                    int[] newPos = map.encryptPixel(x, y);
                    int newFlatIndex = newPos[1] * width + newPos[0];

                    // Write directly to the destination array
                    nextBuffer[newFlatIndex] = (alpha | scrambledColor);
                }
            }
            // 3. Swap the pointers for the next iteration
            int[] temp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = temp;
        }

        return (k % 2 == 0) ? imgA : imgB;
    }

    public static BufferedImage decrypt(BufferedImage encryptedImage, ArnoldsCatMap map, int k) {
        int width = encryptedImage.getWidth();
        int height = encryptedImage.getHeight();

        BufferedImage imgA = deepCopy(encryptedImage);
        BufferedImage imgB = new BufferedImage(width, height, encryptedImage.getType());

        int[] bufferA = ((java.awt.image.DataBufferInt) imgA.getRaster().getDataBuffer()).getData();
        int[] bufferB = ((java.awt.image.DataBufferInt) imgB.getRaster().getDataBuffer()).getData();

        int[] currentBuffer = bufferA;
        int[] nextBuffer = bufferB;

        // Loop runs backward to unroll the diffusion
        for (int i = k - 1; i >= 0; i--) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int flatIndex = y * width + x;
                    int rgb = currentBuffer[flatIndex];

                    // Find original position
                    int[] oldPos = map.decryptPixel(x, y);
                    int oldFlatIndex = oldPos[1] * width + oldPos[0];

                    int alpha = rgb & 0xFF000000;
                    int scrambledColor = rgb & 0x00FFFFFF;

                    // Reverse the mask using the old coordinates
                    int mask = generateMask(oldPos[0], oldPos[1], map.getP(), map.getQ(), i);
                    int restoredColor = scrambledColor ^ mask;

                    nextBuffer[oldFlatIndex] = (alpha | restoredColor);
                }
            }
            int[] temp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = temp;
        }

        return (k % 2 == 0) ? imgA : imgB;
    }

    private static int generateMask(int x, int y, int p, int q, int iteration) {
        int hash = (p * (x + 1) * 31) ^ (q * (y + 1) * 17) ^ ((iteration + 1) * p * q * 73);
        return hash & 0x00FFFFFF;
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage cm = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        Graphics g = cm.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return cm;
    }
}