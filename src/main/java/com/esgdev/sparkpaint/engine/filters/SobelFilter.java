package com.esgdev.sparkpaint.engine.filters;

import java.awt.image.BufferedImage;

public class SobelFilter {

    public static BufferedImage apply(BufferedImage image) {
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
                {0, 0, 0},
                {1, 2, 1}
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int pixelX = 0;
                int pixelY = 0;
                int alphaX = 0;
                int alphaY = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = image.getRGB(x + kx, y + ky);
                        // Get intensity as average of RGB channels
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        int intensity = (r + g + b) / 3;

                        // Also detect alpha edges
                        int alpha = (rgb >> 24) & 0xFF;

                        pixelX += intensity * gx[ky + 1][kx + 1];
                        pixelY += intensity * gy[ky + 1][kx + 1];

                        // Apply Sobel to alpha channel as well
                        alphaX += alpha * gx[ky + 1][kx + 1];
                        alphaY += alpha * gy[ky + 1][kx + 1];
                    }
                }

                int magnitude = (int) Math.sqrt(pixelX * pixelX + pixelY * pixelY);
                int alphaMagnitude = (int) Math.sqrt(alphaX * alphaX + alphaY * alphaY);

                // Use the maximum of color edges and alpha edges
                magnitude = Math.max(magnitude, alphaMagnitude);

                // Normalize the magnitude
                magnitude = Math.min(255, magnitude);

                // Original pixel's alpha
                int originalAlpha = (image.getRGB(x, y) >> 24) & 0xFF;

                // Preserve original alpha in output
                int argb = (originalAlpha << 24) | (magnitude << 16) | (magnitude << 8) | magnitude;
                output.setRGB(x, y, argb);
            }
        }

        return output;
    }
}
