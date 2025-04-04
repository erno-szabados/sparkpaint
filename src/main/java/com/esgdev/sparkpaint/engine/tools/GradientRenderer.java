package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * Utility class for applying various gradient effects to images
 */
public class GradientRenderer {
    private final DrawingCanvas canvas;
    private boolean useAntiAliasing = true;

    public GradientRenderer(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }

    /**
     * Applies a linear gradient to the specified image
     */
    public void applyLinearGradient(BufferedImage image, Point start, Point end, GeneralPath clipPath) {
        Graphics2D g2d = image.createGraphics();

        // Set up quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON :
                        RenderingHints.VALUE_ANTIALIAS_OFF);

        // Set up gradient
        GradientPaint gradient = new GradientPaint(
                start.x, start.y, canvas.getDrawingColor(),
                end.x, end.y, canvas.getFillColor(),
                false // Don't cycle the gradient
        );
        g2d.setPaint(gradient);

        // Apply clipping if needed
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Fill the area with gradient
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
    }

    /**
     * Applies a circular gradient to the specified image
     */
    public void applyCircularGradient(BufferedImage image, Point center, Point radiusPoint, GeneralPath clipPath) {
        Graphics2D g2d = image.createGraphics();

        // Set up quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Calculate radius
        double radius = center.distance(radiusPoint);

        // Set up the radial gradient paint
        RadialGradientPaint gradient = new RadialGradientPaint(
                center,                           // Center point
                (float) radius,                    // Radius
                new float[]{0.0f, 1.0f},         // Distribution
                new Color[]{                      // Colors
                        canvas.getDrawingColor(),
                        canvas.getFillColor()
                }
        );

        g2d.setPaint(gradient);

        // Apply clipping if needed
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Fill the area with gradient
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
    }

    /**
     * Applies a smart linear gradient to the specified image
     */
    public void smartLinearGradientFill(BufferedImage image, int x, int y, Color targetColor,
                                        Point startPoint, Point endPoint, int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();

        // Create a mask image to store which pixels should be filled
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = mask.createGraphics();
        maskG2d.setComposite(AlphaComposite.Clear);
        maskG2d.fillRect(0, 0, width, height);
        maskG2d.dispose();

        // Use the smart fill algorithm to determine which pixels to fill
        generateSmartFillMask(mask, image, x, y, targetColor, epsilon, clipPath);

        // Create and fill a buffer with gradient
        BufferedImage gradientImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gradientG2d = gradientImage.createGraphics();

        // Create the gradient
        GradientPaint gradient = new GradientPaint(
                startPoint.x, startPoint.y, canvas.getDrawingColor(),
                endPoint.x, endPoint.y, canvas.getFillColor(),
                false // Don't cycle the gradient
        );
        gradientG2d.setPaint(gradient);
        gradientG2d.fillRect(0, 0, width, height);
        gradientG2d.dispose();

        // Apply the gradient only where mask is white
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int maskRGB = mask.getRGB(px, py);
                if (maskRGB != 0) { // If not transparent
                    image.setRGB(px, py, gradientImage.getRGB(px, py));
                }
            }
        }
    }

    /**
     * Helper method to generate smart fill mask
     */
    private void generateSmartFillMask(BufferedImage mask, BufferedImage source,
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
            double distance = colorDistance(currentRGB, targetRGB);
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

    /**
     * Calculate color distance between two RGB values
     */
    private double colorDistance(int rgb1, int rgb2) {
        // Extract color components including alpha
        int a1 = (rgb1 >> 24) & 0xFF;
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int a2 = (rgb2 >> 24) & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        // Normalize alpha values
        double alpha1 = a1 / 255.0;
        double alpha2 = a2 / 255.0;

        // Calculate weighted color differences
        double deltaR = (r1 - r2) * alpha1 * alpha2;
        double deltaG = (g1 - g2) * alpha1 * alpha2;
        double deltaB = (b1 - b2) * alpha1 * alpha2;
        double deltaA = (a1 - a2);

        // Return the combined distance
        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB + deltaA * deltaA);
    }
}