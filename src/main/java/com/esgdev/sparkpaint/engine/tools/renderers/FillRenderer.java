package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.FillTool;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * FillRenderer is responsible for rendering fill operations on a DrawingCanvas.
 * It supports linear and circular gradients, smart fills, and canvas fills.
 */
public class FillRenderer extends BaseRenderer {
    private final DrawingCanvas canvas;

    public FillRenderer(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Applies a linear gradient to the specified image
     */
    public void applyLinearGradient(BufferedImage image, Point start, Point end, GeneralPath clipPath) {
        Graphics2D g2d = image.createGraphics();

        // Set up quality rendering
        configureGraphics(g2d);

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
        configureGraphics(g2d);

        // Calculate radius
        double radius = center.distance(radiusPoint);

        // Handle zero radius case
        if (radius <= 0) {
            // Use a small minimum radius instead of zero
            radius = 1.0;
        }

        // Set up the radial gradient paint
        RadialGradientPaint gradient = new RadialGradientPaint(
                center,
                (float) radius,
                new float[]{0.0f, 1.0f},
                new Color[]{
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
    public void applySmartLinear(BufferedImage image, int x, int y, Color targetColor,
                                 Point startPoint, Point endPoint, int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a mask image to store which pixels should be filled
        BufferedImage mask = createMask(width, height, clipPath);

        // Use the smart fill algorithm to determine which pixels to fill
        RenderUtils.generateSmartFillMask(mask, image, x, y, targetColor, epsilon, clipPath);

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
     * Applies a smart circular gradient to the specified image
     */
    public void applySmartCircular(BufferedImage image, int x, int y, Color targetColor,
                                   Point centerPoint, Point radiusPoint, int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a mask image to store which pixels should be filled
        BufferedImage mask = createMask(width, height, clipPath);

        // Use the smart fill algorithm to determine which pixels to fill
        RenderUtils.generateSmartFillMask(mask, image, x, y, targetColor, epsilon, clipPath);

        // Calculate the radius of the circular gradient
        double radius = centerPoint.distance(radiusPoint);

        // Handle zero radius case
        if (radius <= 0) {
            // Use a small minimum radius instead of zero
            radius = 1.0;
        }

        // Create an image for the gradient
        BufferedImage gradientImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gradientG2d = gradientImage.createGraphics();

        // Create the radial gradient paint
        RadialGradientPaint gradient = new RadialGradientPaint(
                centerPoint,
                (float) radius,
                new float[]{0.0f, 1.0f},
                new Color[]{
                        canvas.getDrawingColor(),
                        canvas.getFillColor()
                }
        );
        gradientG2d.setPaint(gradient);
        gradientG2d.fillRect(0, 0, width, height);
        gradientG2d.dispose();

        // Apply the gradient only where the mask is white
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int maskRGB = mask.getRGB(px, py);
                if (maskRGB != 0) { // If not transparent
                    image.setRGB(px, py, gradientImage.getRGB(px, py));
                }
            }
        }
    }

    public void smartFill(BufferedImage image, int x, int y, Color targetColor, Color replacementColor,
                          int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        // Check if the fill is actually setting transparency
        boolean isTransparentFill = replacementColor.getAlpha() == 0;

        if (targetRGB == replacementRGB) {
            return;
        }

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

            int currentRGB = image.getRGB(x, y);

            double distance = FillTool.colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Handle transparency specially
            if (isTransparentFill) {
                // Set full transparency (alpha = 0), preserving RGB
                image.setRGB(x, y, currentRGB & 0x00FFFFFF);
            } else {
                image.setRGB(x, y, replacementRGB);
            }

            visited[x][y] = true;

            // Check neighbors
            if (x > 0) stack.push(new Point(x - 1, y));
            if (x < width - 1) stack.push(new Point(x + 1, y));
            if (y > 0) stack.push(new Point(x, y - 1));
            if (y < height - 1) stack.push(new Point(x, y + 1));
        }
    }

    public void canvasFill(BufferedImage image, Color replacementColor, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int replacementRGB = replacementColor.getRGB();
        boolean isTransparentFill = replacementColor.getAlpha() == 0;

        // Use clipPath if available, otherwise fill entire image
        if (clipPath != null) {
            // Fill only within the clip path
            Graphics2D g2d = image.createGraphics();
            configureGraphics(g2d);
            g2d.setClip(clipPath);

            if (isTransparentFill) {
                // Clear to transparency
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, width, height);
            } else {
                // Normal fill
                g2d.setColor(replacementColor);
                g2d.fillRect(0, 0, width, height);
            }
            g2d.dispose();
        } else {
            // Fill the entire image
            if (isTransparentFill) {
                // Clear each pixel to transparency
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, image.getRGB(x, y) & 0x00FFFFFF);
                    }
                }
            } else {
                // Normal fill
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, replacementRGB);
                    }
                }
            }
        }
    }
}