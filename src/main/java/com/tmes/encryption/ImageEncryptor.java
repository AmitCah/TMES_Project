package com.tmes.encryption;

import java.awt.image.BufferedImage;
import java.awt.Graphics;

public class ImageEncryptor {

    public static BufferedImage encrypt(BufferedImage originalImage, ArnoldsCatMap map, int k) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage currentImage = deepCopy(originalImage);

        for (int i = 0; i < k; i++) {
            BufferedImage nextImage = new BufferedImage(width, height, originalImage.getType());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = currentImage.getRGB(x, y);

                    // 1. Separate Alpha and Color
                    int alpha = rgb & 0xFF000000;
                    int rawColor = rgb & 0x00FFFFFF;

                    // 2. Diffusion: XOR the color with a deterministic chaotic mask
                    int mask = generateMask(x, y, map.getP(), map.getQ(), i);
                    int scrambledColor = rawColor ^ mask;

                    // 3. Confusion: Move the pixel
                    int[] newPos = map.encryptPixel(x, y);

                    // Place it in the new image
                    nextImage.setRGB(newPos[0], newPos[1], alpha | scrambledColor);
                }
            }
            currentImage = nextImage;
        }
        return currentImage;
    }

    public static BufferedImage decrypt(BufferedImage encryptedImage, ArnoldsCatMap map, int k) {
        int width = encryptedImage.getWidth();
        int height = encryptedImage.getHeight();
        BufferedImage currentImage = deepCopy(encryptedImage);

        // IMPORTANT: Decryption must reverse the iterations backward!
        for (int i = k - 1; i >= 0; i--) {
            BufferedImage nextImage = new BufferedImage(width, height, encryptedImage.getType());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = currentImage.getRGB(x, y);

                    // 1. Confusion Inverse: Find where it came from
                    int[] oldPos = map.decryptPixel(x, y);

                    // 2. Separate Alpha and Color
                    int alpha = rgb & 0xFF000000;
                    int scrambledColor = rgb & 0x00FFFFFF;

                    // 3. Diffusion Inverse: Unmask using the original coordinates
                    int mask = generateMask(oldPos[0], oldPos[1], map.getP(), map.getQ(), i);
                    int restoredColor = scrambledColor ^ mask;

                    // 4. Place it back
                    nextImage.setRGB(oldPos[0], oldPos[1], alpha | restoredColor);
                }
            }
            currentImage = nextImage;
        }
        return currentImage;
    }

    // Generates a chaotic, reversible mask based on the topology keys and coordinates
    private static int generateMask(int x, int y, int p, int q, int iteration) {
        // Adding +1 prevents multiplication by zero at coordinates (0,0) or iteration 0
        int hash = (p * (x + 1) * 31) ^ (q * (y + 1) * 17) ^ ((iteration + 1) * p * q * 73);

        // Ensure we only return a 24-bit color mask (ignores the alpha channel)
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