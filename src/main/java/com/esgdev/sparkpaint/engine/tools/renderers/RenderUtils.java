package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.tools.FillTool;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * Utility class for common rendering operations.
 */
public class RenderUtils {

    /**
     * Blends two colors based on a blend strength.
     */
    public static Color getBlendedColor(int currentRGB, float blendStrength, Color paintColor) {
        Color currentColor = new Color(currentRGB, true);

        // Extract alpha channel
        int currentAlpha = currentColor.getAlpha();

        // If completely transparent, just use the paint color with some opacity
        if (currentAlpha == 0) {
            return new Color(
                    paintColor.getRed(),
                    paintColor.getGreen(),
                    paintColor.getBlue(),
                    (int) (255 * blendStrength)
            );
        }

        // For partially or fully opaque pixels, blend color components
        int r = (int) ((1 - blendStrength) * currentColor.getRed() + blendStrength * paintColor.getRed());
        int g = (int) ((1 - blendStrength) * currentColor.getGreen() + blendStrength * paintColor.getGreen());
        int b = (int) ((1 - blendStrength) * currentColor.getBlue() + blendStrength * paintColor.getBlue());

        // Blend the alpha channel as well
        int a = Math.min(255, currentAlpha + (int) (blendStrength * (255 - currentAlpha)));

        return new Color(r, g, b, a);
    }

    /**
     * Creates a circular gradient brush mask.
     */
    public static int[][] createCircularMask(int width, int height) {
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

    /**
     * Creates a 2D Gaussian kernel for blur operations.
     */
    public static float[][] createGaussianKernel(int radius) {
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

    /**
     * Draws a dashed pattern for transparent shape previews.
     */
    public static void drawDashedOutline(Graphics2D g2d, Shape shape, float lineThickness) {
        float[] dashPattern = {8.0f, 8.0f};

        // Draw white line (wider)
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(lineThickness + 2,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 0.0f));
        g2d.draw(shape);

        // Draw black line on top (narrower, offset dash pattern)
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(lineThickness,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dashPattern, dashPattern[0]));
        g2d.draw(shape);
    }

    /**
     * Helper method to generate smart fill mask
     * TODO move to RenderUtils
     */
    public static void generateSmartFillMask(BufferedImage mask, BufferedImage source,
                                       int x, int y, Color targetColor,
                                       int epsilon, GeneralPath clipPath) {
        int width = source.getWidth();
        int height = source.getHeight();
        int targetRGB = targetColor.getRGB();

        // Use flood fill to identify pixels to include
        boolean[][] visited = new boolean[width][height];
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            x = p.x;
            y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height || visited[x][y]) {
                continue;
            }

            int currentRGB = source.getRGB(x, y);

            // Check color distance
            double distance = FillTool.colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Mark this pixel in the mask
            mask.setRGB(x, y, 0xFFFFFFFF); // Opaque white
            visited[x][y] = true;

            // Check neighbors
            if (x > 0) stack.push(new Point(x - 1, y));
            if (x < width - 1) stack.push(new Point(x + 1, y));
            if (y > 0) stack.push(new Point(x, y - 1));
            if (y < height - 1) stack.push(new Point(x, y + 1));
        }
    }
}