package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.FilterBrushTool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

/**
 * FilterBrushRenderer handles the actual implementation of different image filters
 * applied by the FilterBrushTool.
 */
public class FilterBrushRenderer extends BaseRenderer {
    private final DrawingCanvas canvas;
    private final Random random = new Random();

    public FilterBrushRenderer(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Applies the specified filter to the target image at the given location.
     *
     * @param targetImage The image to apply the filter to
     * @param filterType  The type of filter to apply
     * @param x           X coordinate of the filter application area
     * @param y           Y coordinate of the filter application area
     * @param size        Size of the filter application area
     * @param strength    Filter strength (0.0-1.0)
     * @param clip        The clipping region to respect when applying the filter
     */
    public void applyFilter(
            BufferedImage targetImage,
            FilterBrushTool.FilterType filterType,
            int x, int y, int size,
            float strength,
            Shape clip) {

        // Boundary checks
        int startX = Math.max(0, x - size / 2);
        int startY = Math.max(0, y - size / 2);
        int endX = Math.min(targetImage.getWidth(), x + size / 2);
        int endY = Math.min(targetImage.getHeight(), y + size / 2);

        // Create a mask for the brush shape (circular)
        int[][] mask = RenderUtils.createCircularMask(size, size);

        // Apply the appropriate filter
        switch (filterType) {
            case BLUR:
                applyBlurFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip);
                break;
            case NOISE:
                applyNoiseFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip);
                break;
            case DITHER:
                applyDitherFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip,
                        canvas.getDrawingColor(), canvas.getFillColor());
                break;
            case BRIGHTEN:
                applyBrightnessFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip, true);
                break;
            case DARKEN:
                applyBrightnessFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip, false);
                break;
        }
    }

    private void applyBrightnessFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                       int brushSize, float strength, int[][] mask, Shape clip, boolean brighten) {
        final int maxAdjustment = (int) (100 * strength);
        final int sign = brighten ? 1 : -1;

        processPixels(image, startX, startY, endX, endY, mask, clip,
                (x, y, rgb, intensity) -> {
                    int a = (rgb >> 24) & 0xFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Calculate adjustment based on mask intensity
                    int adjustment = (int) (maxAdjustment * intensity) * sign;

                    // Apply brightness adjustment to RGB values
                    r = Math.min(255, Math.max(0, r + adjustment));
                    g = Math.min(255, Math.max(0, g + adjustment));
                    b = Math.min(255, Math.max(0, b + adjustment));

                    // Return the modified pixel
                    return (a << 24) | (r << 16) | (g << 8) | b;
                });
    }

    private void applyBlurFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                 int brushSize, float strength, int[][] mask, Shape clip) {
        // Gaussian blur implementation
        int radius = Math.max(1, Math.round(brushSize * strength / 4));

        // Create kernel with size based on radius - use utility method
        float[][] kernel = RenderUtils.createGaussianKernel(radius);

        // Create a copy of the affected area to avoid sampling from already blurred pixels
        BufferedImage copy = new BufferedImage(endX - startX, endY - startY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        configureGraphics(g2d); // Use inherited method to configure anti-aliasing
        g2d.drawImage(image.getSubimage(startX, startY, endX - startX, endY - startY), 0, 0, null);
        g2d.dispose();

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                // Check if this pixel is inside the brush's circular mask
                int maskX = x - startX;
                int maskY = y - startY;

                if (maskX >= 0 && maskX < mask.length &&
                        maskY >= 0 && maskY < mask[0].length && mask[maskX][maskY] > 0) {

                    // Check clip region if provided
                    if (clip != null && !clip.contains(x, y)) {
                        continue;
                    }

                    // Get mask intensity
                    float intensity = mask[maskX][maskY] / 255.0f;

                    // Skip pixels where the brush effect is minimal
                    if (intensity < 0.05f) continue;

                    // Calculate weighted sum for each channel
                    float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                    float totalWeight = 0;

                    for (int ky = -radius; ky <= radius; ky++) {
                        for (int kx = -radius; kx <= radius; kx++) {
                            int sampleX = x + kx - startX;
                            int sampleY = y + ky - startY;

                            // Bounds check for sample point
                            if (sampleX >= 0 && sampleX < copy.getWidth() &&
                                    sampleY >= 0 && sampleY < copy.getHeight()) {

                                float weight = kernel[ky + radius][kx + radius];
                                int rgb = copy.getRGB(sampleX, sampleY);

                                sumA += ((rgb >> 24) & 0xFF) * weight;
                                sumR += ((rgb >> 16) & 0xFF) * weight;
                                sumG += ((rgb >> 8) & 0xFF) * weight;
                                sumB += (rgb & 0xFF) * weight;
                                totalWeight += weight;
                            }
                        }
                    }

                    // Normalize and set pixel
                    if (totalWeight > 0) {
                        // Blend with original based on mask intensity
                        int origRGB = image.getRGB(x, y);
                        int origA = (origRGB >> 24) & 0xFF;
                        int origR = (origRGB >> 16) & 0xFF;
                        int origG = (origRGB >> 8) & 0xFF;
                        int origB = origRGB & 0xFF;

                        int newA = Math.round(sumA / totalWeight);
                        int newR = Math.round(sumR / totalWeight);
                        int newG = Math.round(sumG / totalWeight);
                        int newB = Math.round(sumB / totalWeight);

                        // Mix original and blurred based on intensity
                        int finalA = Math.round(origA * (1 - intensity) + newA * intensity);
                        int finalR = Math.round(origR * (1 - intensity) + newR * intensity);
                        int finalG = Math.round(origG * (1 - intensity) + newG * intensity);
                        int finalB = Math.round(origB * (1 - intensity) + newB * intensity);

                        // Set the result
                        image.setRGB(x, y, (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB);
                    }
                }
            }
        }
    }

    private void applyNoiseFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                  int brushSize, float strength, int[][] mask, Shape clip) {
        final int maxNoiseAmount = (int) (50 * strength);

        processPixels(image, startX, startY, endX, endY, mask, clip,
                (x, y, rgb, intensity) -> {
                    int a = (rgb >> 24) & 0xFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Generate white noise
                    int noiseValue = random.nextInt(maxNoiseAmount * 2 + 1) - maxNoiseAmount;
                    noiseValue = (int) (noiseValue * intensity);

                    // Apply noise to RGB values
                    r = Math.min(255, Math.max(0, r + noiseValue));
                    g = Math.min(255, Math.max(0, g + noiseValue));
                    b = Math.min(255, Math.max(0, b + noiseValue));

                    return (a << 24) | (r << 16) | (g << 8) | b;
                });
    }

    private void applyDitherFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                   int brushSize, float strength, int[][] mask, Shape clip,
                                   Color primaryColor, Color secondaryColor) {
        // Bayer matrix for ordered dithering (8x8)
        int[][] bayerMatrix = {
                {0, 32, 8, 40, 2, 34, 10, 42},
                {48, 16, 56, 24, 50, 18, 58, 26},
                {12, 44, 4, 36, 14, 46, 6, 38},
                {60, 28, 52, 20, 62, 30, 54, 22},
                {3, 35, 11, 43, 1, 33, 9, 41},
                {51, 19, 59, 27, 49, 17, 57, 25},
                {15, 47, 7, 39, 13, 45, 5, 37},
                {63, 31, 55, 23, 61, 29, 53, 21}
        };

        // Define color palette based on drawing and fill colors
        List<Color> palette = getPalette(strength, primaryColor, secondaryColor);

        double thresholdScaling = 0.2 * strength;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                // Check mask and clip
                int maskX = x - startX;
                int maskY = y - startY;

                if (maskX >= 0 && maskX < mask.length &&
                        maskY >= 0 && maskY < mask[0].length && mask[maskX][maskY] > 0) {

                    if (clip != null && !clip.contains(x, y)) {
                        continue;
                    }

                    // Get mask intensity
                    float intensity = mask[maskX][maskY] / 255.0f * strength;

                    // Skip pixels where the brush effect is minimal
                    if (intensity < 0.05f) continue;

                    // Get original pixel
                    int rgb = image.getRGB(x, y);
                    Color originalColor = new Color(rgb, true);

                    // Get threshold from Bayer matrix
                    int matrixX = x % 8;
                    int matrixY = y % 8;
                    int threshold = bayerMatrix[matrixY][matrixX];

                    // Apply dithering with the brush intensity
                    if (intensity >= 0.9f) { // Full dithering
                        // Find closest color in palette
                        Color closestColor = findClosestColor(originalColor, palette, threshold, thresholdScaling);
                        image.setRGB(x, y, closestColor.getRGB());
                    } else {
                        // Partial dithering - blend original with dithered
                        Color ditheredColor = findClosestColor(originalColor, palette, threshold, thresholdScaling);
                        Color blendedColor = RenderUtils.getBlendedColor(
                                originalColor.getRGB(), intensity, ditheredColor);
                        image.setRGB(x, y, blendedColor.getRGB());
                    }
                }
            }
        }
    }

    private Color findClosestColor(Color originalColor, List<Color> palette, int threshold, double thresholdScaling) {
        // Apply threshold adjustment
        int r = adjustColorChannel(originalColor.getRed(), threshold, thresholdScaling);
        int g = adjustColorChannel(originalColor.getGreen(), threshold, thresholdScaling);
        int b = adjustColorChannel(originalColor.getBlue(), threshold, thresholdScaling);

        Color adjustedColor = new Color(r, g, b, originalColor.getAlpha());

        // Find the closest color in palette
        Color closest = palette.get(0);
        double minDistance = colorDistance(adjustedColor, closest);

        for (Color color : palette) {
            double distance = colorDistance(adjustedColor, color);
            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }

        return closest;
    }

    private void processPixels(BufferedImage image, int startX, int startY, int endX, int endY,
                               int[][] mask, Shape clip, PixelProcessor processor) {
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                // Check mask and clip
                int maskX = x - startX;
                int maskY = y - startY;

                if (maskX >= 0 && maskX < mask.length &&
                        maskY >= 0 && maskY < mask[0].length && mask[maskX][maskY] > 0) {

                    if (clip != null && !clip.contains(x, y)) {
                        continue;
                    }

                    // Get original pixel
                    int rgb = image.getRGB(x, y);
                    int a = (rgb >> 24) & 0xFF;

                    // Skip fully transparent pixels
                    if (a == 0) continue;

                    // Calculate mask intensity
                    float intensity = mask[maskX][maskY] / 255.0f;

                    // Process the pixel
                    int newRgb = processor.processPixel(x, y, rgb, intensity);
                    image.setRGB(x, y, newRgb);
                }
            }
        }
    }

    // Functional interface for pixel processing
    private interface PixelProcessor {
        int processPixel(int x, int y, int rgb, float intensity);
    }

    private double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        int aDiff = c1.getAlpha() - c2.getAlpha();

        // Weighted distance calculation giving more weight to alpha differences
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff + 2 * aDiff * aDiff);
    }

    private static List<Color> getPalette(float strength, Color primaryColor, Color secondaryColor) {
        int levels = 2 + (int) (strength * 6); // 2 to 8 levels
        List<Color> palette = new ArrayList<>();

        // Create gradient between drawing and fill colors
        for (int i = 0; i < levels; i++) {
            float ratio = (float) i / (levels - 1);
            int r = (int) (primaryColor.getRed() * (1 - ratio) + secondaryColor.getRed() * ratio);
            int g = (int) (primaryColor.getGreen() * (1 - ratio) + secondaryColor.getGreen() * ratio);
            int b = (int) (primaryColor.getBlue() * (1 - ratio) + secondaryColor.getBlue() * ratio);
            palette.add(new Color(r, g, b));
        }

        // Add transparent color to palette
        palette.add(new Color(0, 0, 0, 0));
        return palette;
    }

    private int adjustColorChannel(int channel, int threshold, double thresholdScaling) {
        int bayerScale = 64; // Range of the Bayer matrix values
        // Apply threshold adjustment scaled by strength
        int adjusted = channel + (int) ((threshold - (double) bayerScale / 2) * thresholdScaling);
        // Clamp to valid range
        return Math.max(0, Math.min(255, adjusted));
    }
}