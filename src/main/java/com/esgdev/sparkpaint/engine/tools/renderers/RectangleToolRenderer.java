package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * RectangleToolRenderer handles the actual rendering of rectangles for the RectangleTool.
 * This class is responsible for drawing both filled and non-filled rectangles,
 * handling transparent drawing, and creating preview visualizations.
 */
public class RectangleToolRenderer extends BaseRenderer {

    public RectangleToolRenderer() {
    }

    /**
     * Draws a rectangle on the specified image.
     */
    public void drawRectangle(BufferedImage targetImage, Graphics2D g2d, Rectangle bounds,
                              Color outlineColor, Color fillColor, float lineThickness,
                              boolean isFilled, boolean isPreview) {
        configureGraphics(g2d, lineThickness);

        boolean transparentFill = isFilled && fillColor.getAlpha() == 0;
        boolean transparentOutline = outlineColor.getAlpha() == 0;

        // For preview with transparency, use dashed strokes
        if (isPreview && (transparentFill || transparentOutline)) {
            drawTransparencyPreview(g2d, bounds, fillColor, outlineColor,
                    transparentFill, transparentOutline, lineThickness, isFilled);
        } else if (!isPreview && (transparentFill || transparentOutline)) {
            // For final drawing with transparency, draw only visible parts
            if (isFilled && !transparentFill) {
                g2d.setColor(fillColor);
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            if (!transparentOutline) {
                g2d.setColor(outlineColor);
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            // Normal drawing (no transparency)
            if (isFilled) {
                g2d.setColor(fillColor);
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            g2d.setColor(outlineColor);
            g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // Apply transparency mask if needed for final render
        if (!isPreview && (transparentFill || transparentOutline)) {
            applyTransparency(targetImage, bounds, transparentFill, transparentOutline,
                    lineThickness, isFilled, g2d.getClip());
        }
    }

    /**
     * Calculates the rectangle bounds based on start and end points.
     */
    public Rectangle calculateRectangleBounds(Point start, Point end, boolean isSquare) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        if (isSquare) {
            // Force a square by making width equal to height
            int side = Math.max(width, height);
            return new Rectangle(x, y, side, side);
        }

        return new Rectangle(x, y, width, height);
    }

    /**
     * Draw a preview for transparent rectangles using dashed patterns.
     */
    private void drawTransparencyPreview(Graphics2D g2d, Rectangle bounds,
                                         Color fillColor, Color outlineColor,
                                         boolean transparentFill, boolean transparentOutline,
                                         float lineThickness, boolean isFilled) {
        float[] dashPattern = {8.0f, 8.0f};

        // For filled preview with transparency
        if (isFilled) {
            if (transparentFill) {
                // Use red with semi-transparency to indicate transparent fill
                g2d.setColor(new Color(255, 0, 0, 32));
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            } else {
                // Show normal fill color with reduced opacity
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                        fillColor.getBlue(), 128));
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        // For transparent outline preview
        if (transparentOutline) {
            // Draw white line (wider)
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(lineThickness + 2,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f, dashPattern, 0.0f));
            g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

            // Draw black line on top (narrower, offset dash pattern)
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(lineThickness,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f, dashPattern, dashPattern[0]));
            g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            // Normal outline
            g2d.setColor(outlineColor);
            g2d.setStroke(new BasicStroke(lineThickness));
            g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /**
     * Apply transparency to pixels where the rectangle is rendered.
     */
    private void applyTransparency(BufferedImage image, Rectangle bounds,
                                   boolean transparentFill, boolean transparentOutline,
                                   float lineThickness, boolean isFilled, Shape clip) {
        // Check for null parameters
        if (image == null || bounds == null) {
            System.err.println("Null parameter in applyTransparency");
            return;
        }

        try {
            // Create mask for transparency
            BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Configure graphics
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Apply the same clip as the original graphics
            if (clip != null) {
                maskG2d.setClip(clip);
            }

            // Draw mask for transparent parts
            if (transparentFill && isFilled) {
                maskG2d.setColor(Color.WHITE);
                maskG2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            if (transparentOutline) {
                maskG2d.setColor(Color.WHITE);
                maskG2d.setStroke(new BasicStroke(lineThickness));
                maskG2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            maskG2d.dispose();

            // Add padding around bounds to account for stroke width
            int padding = (int) Math.ceil(lineThickness);
            int minX = Math.max(0, bounds.x - padding);
            int minY = Math.max(0, bounds.y - padding);
            int maxX = Math.min(image.getWidth(), bounds.x + bounds.width + padding);
            int maxY = Math.min(image.getHeight(), bounds.y + bounds.height + padding);

            // Apply transparency mask
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Check if this pixel is within clip region
                    if (clip == null || clip.contains(x, y)) {
                        int maskRGB = maskImage.getRGB(x, y);
                        // Only process pixels where the mask is non-zero
                        if ((maskRGB & 0xFF000000) != 0) {
                            // Set full transparency (alpha = 0)
                            int newRGB = image.getRGB(x, y) & 0x00FFFFFF;
                            image.setRGB(x, y, newRGB);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in applyTransparency: " + e.getMessage());
            e.printStackTrace();
        }
    }
}