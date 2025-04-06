package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * EllipseToolRenderer handles the actual rendering of ellipses for the EllipseTool.
 * This class is responsible for drawing both filled and non-filled ellipses,
 * handling transparent drawing, and creating preview visualizations.
 */
public class EllipseToolRenderer extends BaseRenderer {

    public EllipseToolRenderer(DrawingCanvas canvas) {
        // No initialization needed
    }

    /**
     * Draws an ellipse on the specified image.
     */
    public void drawEllipse(BufferedImage targetImage, Graphics2D g2d, Rectangle bounds,
                            Color outlineColor, Color fillColor, float lineThickness,
                            boolean isFilled, boolean isPreview) {
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
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            if (!transparentOutline) {
                g2d.setColor(outlineColor);
                g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            // Normal drawing (no transparency)
            if (isFilled) {
                g2d.setColor(fillColor);
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            g2d.setColor(outlineColor);
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // Apply transparency mask if needed for final render
        if (!isPreview && (transparentFill || transparentOutline)) {
            applyTransparency(targetImage, bounds, transparentFill, transparentOutline,
                    lineThickness, isFilled, g2d.getClip());
        }
    }

    /**
     * Calculates the rectangle bounds for an ellipse based on start and end points.
     */
    public Rectangle calculateEllipseBounds(Point start, Point end, boolean isCircle, boolean isCenterBased) {
        if (isCenterBased) {
            // Center-based: start point is the center
            int radius = (int) Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
            return new Rectangle(start.x - radius, start.y - radius, radius * 2, radius * 2);
        } else {
            // Corner-based: standard rectangle calculation
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(end.x - start.x);
            int height = Math.abs(end.y - start.y);

            if (isCircle) {
                // Force a circle by making width equal to height
                int diameter = Math.max(width, height);
                return new Rectangle(x, y, diameter, diameter);
            }

            return new Rectangle(x, y, width, height);
        }
    }

    /**
     * Draw a preview for transparent ellipses using dashed patterns.
     */
    private void drawTransparencyPreview(Graphics2D g2d, Rectangle bounds,
                                         Color fillColor, Color outlineColor,
                                         boolean transparentFill, boolean transparentOutline,
                                         float lineThickness, boolean isFilled) {
        // For filled preview with transparency
        if (isFilled) {
            if (transparentFill) {
                // Use red with semi-transparency to indicate transparent fill
                g2d.setColor(new Color(255, 0, 0, 32));
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            } else {
                // Show normal fill color with reduced opacity
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                        fillColor.getBlue(), 128));
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        // For transparent outline preview
        if (transparentOutline) {
            // Use utility method for dashed outline
            Shape ovalShape = new java.awt.geom.Ellipse2D.Float(
                    bounds.x, bounds.y, bounds.width, bounds.height);
            RenderUtils.drawDashedOutline(g2d, ovalShape, lineThickness);
        } else {
            // Normal outline
            g2d.setColor(outlineColor);
            g2d.setStroke(new BasicStroke(lineThickness));
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /**
     * Apply transparency to pixels where the ellipse is rendered.
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
            BufferedImage maskImage = createMask(image.getWidth(), image.getHeight(), clip);
            Graphics2D maskG2d = maskImage.createGraphics();
            configureGraphics(maskG2d, Color.WHITE, lineThickness);

            // Draw mask for transparent parts
            if (transparentFill && isFilled) {
                maskG2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            if (transparentOutline) {
                maskG2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            maskG2d.dispose();

            // Add padding around bounds to account for stroke width
            int padding = (int) Math.ceil(lineThickness);
            int minX = Math.max(0, bounds.x - padding);
            int minY = Math.max(0, bounds.y - padding);
            int maxX = Math.min(image.getWidth(), bounds.x + bounds.width + padding);
            int maxY = Math.min(image.getHeight(), bounds.y + bounds.height + padding);

            // Apply transparency mask using base class method
            applyTransparencyMask(image, maskImage, clip);
        } catch (Exception e) {
            System.err.println("Exception in applyTransparency: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configure graphics context with rendering settings.
     * This method is now inherited from BaseRenderer
     */
    @Override
    public void configureGraphics(Graphics2D g2d, float lineThickness) {
        super.configureGraphics(g2d, lineThickness);
    }
}