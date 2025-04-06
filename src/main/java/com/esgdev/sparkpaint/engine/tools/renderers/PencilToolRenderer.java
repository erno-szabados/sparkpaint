package com.esgdev.sparkpaint.engine.tools.renderers;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * PencilToolRenderer handles the actual rendering of pencil strokes.
 * This class is responsible for drawing points and lines, handling transparent drawing,
 * and applying appropriate rendering settings.
 */
public class PencilToolRenderer {

    private boolean useAntiAliasing = true;

    public PencilToolRenderer() {
    }

    /**
     * Draws a single point at the specified location.
     */
    public void drawPoint(BufferedImage targetImage, Point point, Color color, float thickness) {
        if (color.getAlpha() == 0) {
            drawTransparentPoint(targetImage, point);
        } else {
            Graphics2D g2d = (Graphics2D) targetImage.getGraphics();
            configureGraphics(g2d, color, thickness);
            g2d.drawLine(point.x, point.y, point.x, point.y);
            g2d.dispose();
        }
    }

    /**
     * Draws a line from one point to another.
     */
    public void drawLine(BufferedImage targetImage, Point from, Point to, Color color, float thickness) {
        if (color.getAlpha() == 0) {
            drawTransparentLine(targetImage, from, to, thickness);
        } else {
            Graphics2D g2d = (Graphics2D) targetImage.getGraphics();
            configureGraphics(g2d, color, thickness);
            g2d.drawLine(from.x, from.y, to.x, to.y);
            g2d.dispose();
        }
    }

    /**
     * Draws a transparent point (erasing point).
     */
    private void drawTransparentPoint(BufferedImage targetImage, Point point) {
        // Check bounds
        int x = point.x;
        int y = point.y;

        if (x < 0 || y < 0 || x >= targetImage.getWidth() || y >= targetImage.getHeight()) {
            return;
        }

        // Set full transparency (alpha = 0)
        int newRGB = targetImage.getRGB(x, y) & 0x00FFFFFF;
        targetImage.setRGB(x, y, newRGB);
    }

    /**
     * Draws a transparent line (erasing line) using Bresenham's algorithm.
     */
    private void drawTransparentLine(BufferedImage targetImage, Point p1, Point p2, float thickness) {
        // Check bounds - skip if completely out of bounds
        if ((p1.x < 0 && p2.x < 0) ||
                (p1.y < 0 && p2.y < 0) ||
                (p1.x >= targetImage.getWidth() && p2.x >= targetImage.getWidth()) ||
                (p1.y >= targetImage.getHeight() && p2.y >= targetImage.getHeight())) {
            return;
        }

        try {
            // Create a mask for the line
            BufferedImage maskImage = new BufferedImage(
                    targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Configure mask graphics
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            maskG2d.setStroke(new BasicStroke(thickness));
            maskG2d.setColor(Color.WHITE);

            // Draw line on mask
            maskG2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            maskG2d.dispose();

            // Get bounds of affected area with padding for stroke width
            int padding = (int) Math.ceil(thickness);
            int minX = Math.max(0, Math.min(p1.x, p2.x) - padding);
            int minY = Math.max(0, Math.min(p1.y, p2.y) - padding);
            int maxX = Math.min(targetImage.getWidth(), Math.max(p1.x, p2.x) + padding);
            int maxY = Math.min(targetImage.getHeight(), Math.max(p1.y, p2.y) + padding);

            // Apply transparency to each pixel on the line
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    int maskRGB = maskImage.getRGB(x, y);
                    // Only process pixels where the mask is non-zero
                    if ((maskRGB & 0xFF000000) != 0) {
                        // Set full transparency (alpha = 0)
                        int newRGB = targetImage.getRGB(x, y) & 0x00FFFFFF;
                        targetImage.setRGB(x, y, newRGB);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error drawing transparent line: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Configure graphics context with rendering settings.
     */
    public void configureGraphics(Graphics2D g2d, Color color, float thickness) {
        g2d.setStroke(new BasicStroke(thickness));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(color);
    }

    /**
     * Set whether to use antialiasing for rendering.
     */
    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}