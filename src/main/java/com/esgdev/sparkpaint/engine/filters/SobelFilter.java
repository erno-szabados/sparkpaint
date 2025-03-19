package com.esgdev.sparkpaint.engine.filters;

import java.awt.image.BufferedImage;

public class SobelFilter {

    public static BufferedImage applySobelFilter(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Sobel kernels
        int[][] gx = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] gy = {
                {-1, -2, -1},
                { 0,  0,  0},
                { 1,  2,  1}
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int pixelX = 0;
                int pixelY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = image.getRGB(x + kx, y + ky);
                        int intensity = (rgb >> 16) & 0xFF; // Use red channel for intensity
                        pixelX += intensity * gx[ky + 1][kx + 1];
                        pixelY += intensity * gy[ky + 1][kx + 1];
                    }
                }

                int magnitude = (int) Math.sqrt(pixelX * pixelX + pixelY * pixelY);

                // Normalize the magnitude and set the output pixel
                magnitude = Math.min(255, magnitude); // Cap to 255
                int gray = (magnitude << 16) | (magnitude << 8) | magnitude; // Grayscale
                output.setRGB(x, y, 0xFF000000 | gray); // Add alpha channel
            }
        }

        return output;
    }
}
