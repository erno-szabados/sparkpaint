package com.esgdev.sparkpaint.engine.tools;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * FilterBrushRenderer handles the actual implementation of different image filters
 * applied by the FilterBrushTool.
 */
public class FilterBrushRenderer {

    /**
     * Applies the specified filter to the target image at the given location.
     *
     * @param targetImage    The image to apply the filter to
     * @param filterType     The type of filter to apply
     * @param x              X coordinate of the filter application area
     * @param y              Y coordinate of the filter application area
     * @param size           Size of the filter application area
     * @param strength       Filter strength (0.0-1.0)
     * @param antiAliasing   Whether to use anti-aliasing in the filter application
     * @param clip           The clipping region to respect when applying the filter
     */
    public void applyFilter(
            BufferedImage targetImage,
            FilterBrushTool.FilterType filterType,
            int x, int y, int size,
            float strength,
            boolean antiAliasing,
            Shape clip) {

        // Boundary checks
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(targetImage.getWidth(), x + size);
        int endY = Math.min(targetImage.getHeight(), y + size);

        // Create a mask for the brush shape (circular)
        int[][] mask = createBrushMask(size, size);

        // Apply the appropriate filter
        switch (filterType) {
            case BLUR:
                applyBlurFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip);
                break;
            case NOISE:
                applyNoiseFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip);
                break;
            case DITHER:
                applyDitherFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip);
                break;
        }
    }

    private int[][] createBrushMask(int width, int height) {
        int[][] mask = new int[width][height];
        int centerX = width / 2;
        int centerY = height / 2;
        double radius = Math.min(width, height) / 2.0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                if (distance <= radius) {
                    // Fade intensity from center (1.0) to edge (0.0)
                    double intensity = 1.0 - (distance / radius);
                    mask[x][y] = (int)(intensity * 255);
                } else {
                    mask[x][y] = 0;
                }
            }
        }

        return mask;
    }

    private void applyBlurFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                int brushSize, float strength, int[][] mask, Shape clip) {
        // Placeholder for blur implementation
        // Will be implemented later
    }

    private void applyNoiseFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                 int brushSize, float strength, int[][] mask, Shape clip) {
        // Placeholder for noise implementation
        // Will be implemented later
    }

    private void applyDitherFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                  int brushSize, float strength, int[][] mask, Shape clip) {
        // Placeholder for dither implementation
        // Will be implemented later
    }
}