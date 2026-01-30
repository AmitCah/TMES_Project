package com.tmes.encryption;

import java.awt.image.BufferedImage;
import java.awt.Graphics;

public class ImageEncryptor {

    public static BufferedImage encrypt(BufferedImage originalImage, ArnoldsCatMap map, int k) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Working copy
        BufferedImage currentImage = deepCopy(originalImage);

        // Loop K times (iterations of chaos)
        for (int i = 0; i < k; i++) {
            BufferedImage nextImage = new BufferedImage(width, height, originalImage.getType());

            // Iterate over every pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = currentImage.getRGB(x, y);

                    // Calculate where this pixel moves to
                    int[] newPos = map.encryptPixel(x, y);

                    // Place it in the new image
                    nextImage.setRGB(newPos[0], newPos[1], rgb);
                }
            }
            // Swap buffers: the new image becomes the current image for the next round
            currentImage = nextImage;
        }
        return currentImage;
    }

    public static BufferedImage decrypt(BufferedImage encryptedImage, ArnoldsCatMap map, int k) {
        int width = encryptedImage.getWidth();
        int height = encryptedImage.getHeight();

        BufferedImage currentImage = deepCopy(encryptedImage);

        // Loop K times (reverse order)
        for (int i = 0; i < k; i++) {
            BufferedImage nextImage = new BufferedImage(width, height, encryptedImage.getType());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = currentImage.getRGB(x, y);

                    // Calculate where this pixel CAME FROM (Inverse)
                    // Note: We are taking the pixel at (x,y) in the scrambled image
                    // and moving it back to its previous position.
                    int[] oldPos = map.decryptPixel(x, y);

                    nextImage.setRGB(oldPos[0], oldPos[1], rgb);
                }
            }
            currentImage = nextImage;
        }
        return currentImage;
    }

    // Helper to clone image
    public static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage cm = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        Graphics g = cm.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return cm;
    }
}