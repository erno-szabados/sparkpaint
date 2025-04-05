package com.esgdev.sparkpaint.engine.filters;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Implements ordered dithering using a Bayer matrix pattern
 * to convert an image to use only colors from a specified palette.
 */
public class OrderedDitheringFilter {
    // 8x8 Bayer matrix for ordered dithering
    private static final int[][] BAYER_MATRIX_8X8 = {
            {0, 32, 8, 40, 2, 34, 10, 42},
            {48, 16, 56, 24, 50, 18, 58, 26},
            {12, 44, 4, 36, 14, 46, 6, 38},
            {60, 28, 52, 20, 62, 30, 54, 22},
            {3, 35, 11, 43, 1, 33, 9, 41},
            {51, 19, 59, 27, 49, 17, 57, 25},
            {15, 47, 7, 39, 13, 45, 5, 37},
            {63, 31, 55, 23, 61, 29, 53, 21}
    };

    // Scale factor for the Bayer matrix
    private static final int BAYER_SCALE = 64;
    private static final double THRESHOLD_SCALING = 0.2;

    /**
     * Apply ordered dithering to the input image using the provided color palette.
     *
     * @param image    The input image to apply dithering to
     * @param palette  The color palette to use for dithering
     * @return         The dithered image
     */
    public static BufferedImage apply(BufferedImage image, List<Color> palette) {
        if (image == null || palette == null || palette.isEmpty()) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);

                // Get the threshold adjustment from the Bayer matrix
                int matrixX = x % 8;
                int matrixY = y % 8;
                int threshold = BAYER_MATRIX_8X8[matrixY][matrixX];

                // Apply threshold adjustment
                int r = adjustColorChannel(color.getRed(), threshold);
                int g = adjustColorChannel(color.getGreen(), threshold);
                int b = adjustColorChannel(color.getBlue(), threshold);

                // Keep alpha as is
                int a = color.getAlpha();

                // Find the nearest color in the palette
                Color newColor = findNearestColor(new Color(r, g, b, a), palette);
                result.setRGB(x, y, newColor.getRGB());
            }
        }

        return result;
    }

    /**
     * Adjusts a color channel value based on the dithering threshold.
     */
    private static int adjustColorChannel(int channel, int threshold) {
        // Apply scaled threshold adjustment
        int adjusted = channel + (int)((threshold - (double) BAYER_SCALE / 2) * THRESHOLD_SCALING);
        // Clamp to valid range
        return Math.max(0, Math.min(255, adjusted));
    }

    /**
     * Find the nearest color in the palette using Euclidean distance in RGB space.
     */
    public static Color findNearestColor(Color color, List<Color> palette) {
        // Handle transparent colors
        if (color.getAlpha() < 128) {
            return new Color(0, 0, 0, 0);
        }

        Color nearest = palette.get(0);
        double minDistance = Double.MAX_VALUE;

        for (Color paletteColor : palette) {
            // Skip transparent colors in the palette
            if (paletteColor.getAlpha() < 128) {
                continue;
            }

            double distance = colorDistance(color, paletteColor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = paletteColor;
            }
        }

        return nearest;
    }

    /**
     * Calculate Euclidean distance between two colors in RGB space.
     */
    public static double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}