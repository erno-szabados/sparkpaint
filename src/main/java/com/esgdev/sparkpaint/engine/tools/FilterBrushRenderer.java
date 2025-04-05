package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.filters.OrderedDitheringFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

/**
 * FilterBrushRenderer handles the actual implementation of different image filters
 * applied by the FilterBrushTool.
 */
public class FilterBrushRenderer {
    private final DrawingCanvas canvas;

    public FilterBrushRenderer(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Applies the specified filter to the target image at the given location.
     *
     * @param targetImage  The image to apply the filter to
     * @param filterType   The type of filter to apply
     * @param x            X coordinate of the filter application area
     * @param y            Y coordinate of the filter application area
     * @param size         Size of the filter application area
     * @param strength     Filter strength (0.0-1.0)
     * @param clip         The clipping region to respect when applying the filter
     */
    public void applyFilter(
            BufferedImage targetImage,
            FilterBrushTool.FilterType filterType,
            int x, int y, int size,
            float strength,
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
                applyDitherFilter(targetImage, startX, startY, endX, endY, size, strength, mask, clip, canvas.getDrawingColor(), canvas.getFillColor());
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
                    mask[x][y] = (int) (intensity * 255);
                } else {
                    mask[x][y] = 0;
                }
            }
        }

        return mask;
    }

    private void applyBlurFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                 int brushSize, float strength, int[][] mask, Shape clip) {
        // Gaussian blur implementation
        int radius = Math.max(1, Math.round(brushSize * strength / 4));

        // Create kernel with size based on radius
        int size = radius * 2 + 1;
        float[][] kernel = createGaussianKernel(radius);

        // Create a copy of the affected area to avoid sampling from already blurred pixels
        BufferedImage copy = new BufferedImage(endX - startX, endY - startY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
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

                    // Calculate weighted sum for each channel
                    float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                    float totalWeight = 0;

                    for (int ky = -radius; ky <= radius; ky++) {
                        for (int kx = -radius; kx <= radius; kx++) {
                            int sampleX = x + kx - startX;
                            int sampleY = y + ky - startY;

                            // Skip samples outside the copy area
                            if (sampleX < 0 || sampleX >= copy.getWidth() ||
                                    sampleY < 0 || sampleY >= copy.getHeight()) {
                                continue;
                            }

                            // Get color and kernel weight
                            int rgb = copy.getRGB(sampleX, sampleY);
                            float weight = kernel[ky + radius][kx + radius];

                            // Apply mask weight to create falloff at brush edges
                            float maskWeight = mask[maskX][maskY] / 255.0f * strength;
                            weight *= maskWeight;

                            // Accumulate weighted channels
                            sumA += ((rgb >> 24) & 0xFF) * weight;
                            sumR += ((rgb >> 16) & 0xFF) * weight;
                            sumG += ((rgb >> 8) & 0xFF) * weight;
                            sumB += (rgb & 0xFF) * weight;
                            totalWeight += weight;
                        }
                    }

                    // Normalize and set pixel
                    if (totalWeight > 0) {
                        int a = Math.min(255, Math.max(0, Math.round(sumA / totalWeight)));
                        int r = Math.min(255, Math.max(0, Math.round(sumR / totalWeight)));
                        int g = Math.min(255, Math.max(0, Math.round(sumG / totalWeight)));
                        int b = Math.min(255, Math.max(0, Math.round(sumB / totalWeight)));

                        // Get original pixel
                        int origRgb = image.getRGB(x, y);
                        int origA = (origRgb >> 24) & 0xFF;
                        int origR = (origRgb >> 16) & 0xFF;
                        int origG = (origRgb >> 8) & 0xFF;
                        int origB = origRgb & 0xFF;

                        // Blend based on mask intensity for smooth transitions
                        float blendFactor = mask[maskX][maskY] / 255.0f * strength;
                        a = Math.round(origA * (1 - blendFactor) + a * blendFactor);
                        r = Math.round(origR * (1 - blendFactor) + r * blendFactor);
                        g = Math.round(origG * (1 - blendFactor) + g * blendFactor);
                        b = Math.round(origB * (1 - blendFactor) + b * blendFactor);

                        image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }
        }
    }

    private float[][] createGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float[][] kernel = new float[size][size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(2.0f * Math.PI * sigma * sigma);
        float total = 0.0f;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float distance = x * x + y * y;
                kernel[y + radius][x + radius] =
                        (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
                total += kernel[y + radius][x + radius];
            }
        }

        // Normalize the kernel
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= total;
            }
        }

        return kernel;
    }

    private void applyNoiseFilter(BufferedImage image, int startX, int startY, int endX, int endY,
                                  int brushSize, float strength, int[][] mask, Shape clip) {
        // Use simple white noise instead of Perlin noise
        Random random = new Random();

        // Maximum noise amount based on strength
        int maxNoiseAmount = (int)(50 * strength); // Reduced range for more controlled effect

        // Apply noise with mask
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

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Generate white noise (-maxNoiseAmount to +maxNoiseAmount)
                    int noiseValue = random.nextInt(maxNoiseAmount * 2 + 1) - maxNoiseAmount;

                    // Scale noise by mask intensity
                    float intensity = mask[maskX][maskY] / 255.0f;
                    noiseValue = (int)(noiseValue * intensity);

                    // Apply noise to RGB values (affecting brightness)
                    r = Math.min(255, Math.max(0, r + noiseValue));
                    g = Math.min(255, Math.max(0, g + noiseValue));
                    b = Math.min(255, Math.max(0, b + noiseValue));

                    // Write back the modified pixel
                    image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
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
                        // Adjust color channels based on threshold
                        int r = adjustColorChannel(originalColor.getRed(), threshold, thresholdScaling);
                        int g = adjustColorChannel(originalColor.getGreen(), threshold, thresholdScaling);
                        int b = adjustColorChannel(originalColor.getBlue(), threshold, thresholdScaling);

                        // Keep original alpha
                        int a = originalColor.getAlpha();

                        // Find nearest color in palette
                        Color newColor = OrderedDitheringFilter.findNearestColor(new Color(r, g, b, a), palette);
                        image.setRGB(x, y, newColor.getRGB());
                    } else {
                        // Partial dithering - blend between original and dithered
                        // Adjust color channels
                        int r = adjustColorChannel(originalColor.getRed(), threshold, thresholdScaling);
                        int g = adjustColorChannel(originalColor.getGreen(), threshold, thresholdScaling);
                        int b = adjustColorChannel(originalColor.getBlue(), threshold, thresholdScaling);
                        int a = originalColor.getAlpha();

                        // Find nearest color in palette
                        Color ditheredColor = OrderedDitheringFilter.findNearestColor(new Color(r, g, b, a), palette);

                        // Blend based on intensity
                        r = (int) (originalColor.getRed() * (1 - intensity) + ditheredColor.getRed() * intensity);
                        g = (int) (originalColor.getGreen() * (1 - intensity) + ditheredColor.getGreen() * intensity);
                        b = (int) (originalColor.getBlue() * (1 - intensity) + ditheredColor.getBlue() * intensity);

                        image.setRGB(x, y, new Color(r, g, b, a).getRGB());
                    }
                }
            }
        }
    }

    private static List<Color> getPalette(float strength, Color primaryColor, Color secondaryColor) {
        int levels = 2 + (int) (strength * 6); // 2 to 8 levels
        List<Color> palette = new ArrayList<>();

        // Create gradient between drawing and fill colors
        for (int i = 0; i < levels; i++) {
            float ratio = (float) i / (levels - 1);
            int r = (int) (primaryColor.getRed() * (1-ratio) + secondaryColor.getRed() * ratio);
            int g = (int) (primaryColor.getGreen() * (1-ratio) + secondaryColor.getGreen() * ratio);
            int b = (int) (primaryColor.getBlue() * (1-ratio) + secondaryColor.getBlue() * ratio);
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