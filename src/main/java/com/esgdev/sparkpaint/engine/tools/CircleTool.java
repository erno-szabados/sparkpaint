package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class CircleTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isFilled;
    private boolean useAntiAliasing = true;
    private boolean isCenterBased = false;

    public CircleTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.isFilled = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert to world coordinates and check if we're in a selection
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't start drawing outside selection
        }


        // Save start point using appropriate coordinate system
        startPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        canvas.saveToUndoStack();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (startPoint == null) return;

        Selection selection = canvas.getSelection();

        // Get current point in appropriate coordinate system
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Create temporary canvas for preview
        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear the temp canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply rendering settings
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Apply selection clip
        applySelectionClip(g2d, selection);

        // Check if we're using transparent colors
        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        if (transparentFill || transparentOutline) {
            // Define dash pattern for the preview
            float[] dashPattern = {8.0f, 8.0f};
            float lineThickness = canvas.getLineThickness();

            if (isCenterBased) {
                // Draw center-based circle with dashed preview for transparency
                int radius = (int) Math.sqrt(Math.pow(point.x - startPoint.x, 2) + Math.pow(point.y - startPoint.y, 2));
                int x = startPoint.x - radius;
                int y = startPoint.y - radius;
                int diameter = radius * 2;

                // Draw filled preview if needed (semi-transparent)
                if (isFilled) {
                    if (transparentFill) {
                        // Use red with semi-transparency to indicate transparent fill
                        g2d.setColor(new Color(255, 0, 0, 32));
                        g2d.fillOval(x, y, diameter, diameter);
                    } else {
                        // Show normal fill color with reduced opacity
                        Color fillColor = canvas.getFillColor();
                        g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                                fillColor.getBlue(), 128));
                        g2d.fillOval(x, y, diameter, diameter);
                    }
                }

                // For transparent outline, use dashed pattern
                if (transparentOutline) {
                    // Draw white line (wider)
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(lineThickness + 2,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, 0.0f));
                    g2d.drawOval(x, y, diameter, diameter);

                    // Draw black line on top (narrower, offset dash pattern)
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(lineThickness,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, dashPattern[0]));
                    g2d.drawOval(x, y, diameter, diameter);
                } else {
                    // Normal outline
                    g2d.setColor(canvas.getDrawingColor());
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawOval(x, y, diameter, diameter);
                }
            } else {
                // Draw corner-based circle with dashed preview for transparency
                int x = Math.min(startPoint.x, point.x);
                int y = Math.min(startPoint.y, point.y);
                int width = Math.abs(point.x - startPoint.x);
                int height = Math.abs(point.y - startPoint.y);

                if (width > height) {
                    height = width;
                    if (point.y < startPoint.y) {
                        y = startPoint.y - height;
                    }
                } else {
                    width = height;
                    if (point.x < startPoint.x) {
                        x = startPoint.x - width;
                    }
                }

                // Draw filled preview if needed (semi-transparent)
                if (isFilled) {
                    if (transparentFill) {
                        // Use red with semi-transparency to indicate transparent fill
                        g2d.setColor(new Color(255, 0, 0, 32));
                        g2d.fillOval(x, y, width, height);
                    } else {
                        // Show normal fill color with reduced opacity
                        Color fillColor = canvas.getFillColor();
                        g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                                fillColor.getBlue(), 128));
                        g2d.fillOval(x, y, width, height);
                    }
                }

                // For transparent outline, use dashed pattern
                if (transparentOutline) {
                    // Draw white line (wider)
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(lineThickness + 2,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, 0.0f));
                    g2d.drawOval(x, y, width, height);

                    // Draw black line on top (narrower, offset dash pattern)
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(lineThickness,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, dashPattern[0]));
                    g2d.drawOval(x, y, width, height);
                } else {
                    // Normal outline
                    g2d.setColor(canvas.getDrawingColor());
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawOval(x, y, width, height);
                }
            }
        } else {
            // For regular (non-transparent) drawing, use the standard approach
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));

            if (isCenterBased) {
                drawCenterBasedCircle(g2d, startPoint, point);
            } else {
                drawCornerBasedCircle(g2d, startPoint, point);
            }
        }

        g2d.dispose();
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (startPoint == null) return;

        Selection selection = canvas.getSelection();

        // Get current point in appropriate coordinate system
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context for drawing
        Graphics2D g2d;
        Point adjustedStartPoint = startPoint;
        Point adjustedEndPoint = point;
        BufferedImage targetImage;

        if (selection != null && selection.hasOutline()) {
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();
            selection.setModified(true);
            targetImage = selection.getContent();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            adjustedStartPoint = new Point(startPoint.x - bounds.x, startPoint.y - bounds.y);
            adjustedEndPoint = new Point(point.x - bounds.x, point.y - bounds.y);
        } else {
            // Draw on current layer
            targetImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) targetImage.getGraphics();
        }

        // Apply rendering settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Check if we're using transparent colors
        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        // For regular (non-transparent) drawing, use the standard approach
        if (!transparentFill && !transparentOutline) {
            if (isCenterBased) {
                drawCenterBasedCircle(g2d, adjustedStartPoint, adjustedEndPoint);
            } else {
                drawCornerBasedCircle(g2d, adjustedStartPoint, adjustedEndPoint);
            }
        } else {
            // For transparent drawing, use special handling
            if (isCenterBased) {
                drawTransparentCenterBasedCircle(targetImage, g2d, adjustedStartPoint, adjustedEndPoint, transparentFill, transparentOutline);
            } else {
                drawTransparentCornerBasedCircle(targetImage, g2d, adjustedStartPoint, adjustedEndPoint, transparentFill, transparentOutline);
            }
        }

        g2d.dispose();

        // Clear the temp canvas and reset state
        canvas.setToolCanvas(null);
        startPoint = null;
        canvas.repaint();
    }

    /**
     * Draws a transparent corner-based circle
     */
    private void drawTransparentCornerBasedCircle(BufferedImage image, Graphics2D g2d, Point start, Point end,
                                                  boolean transparentFill, boolean transparentOutline) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        if (width > height) {
            height = width;
            if (end.y < start.y) {
                y = start.y - height;
            }
        } else {
            width = height;
            if (end.x < start.x) {
                x = start.x - width;
            }
        }

        // Create a mask image for transparency
        BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();

        // Apply same rendering hints
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw filled and/or outline
        if (isFilled && transparentFill) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.fillOval(x, y, width, height);
        }

        if (transparentOutline) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            maskG2d.drawOval(x, y, width, height);
        }

        // If only fill is transparent, still draw the normal outline
        if (isFilled && transparentFill && !transparentOutline) {
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, width, height);
        }

        // If only outline is transparent, still draw the normal fill
        if (isFilled && !transparentFill && transparentOutline) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, width, height);
        }

        maskG2d.dispose();

        // Apply transparency where mask is white
        applyTransparencyMask(image, maskImage, g2d.getClip());
    }

    /**
     * Draws a transparent center-based circle
     */
    private void drawTransparentCenterBasedCircle(BufferedImage image, Graphics2D g2d, Point center, Point edge,
                                                  boolean transparentFill, boolean transparentOutline) {
        int radius = (int) Math.sqrt(Math.pow(edge.x - center.x, 2) + Math.pow(edge.y - center.y, 2));
        int x = center.x - radius;
        int y = center.y - radius;
        int diameter = radius * 2;

        // Create a mask image for transparency
        BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();

        // Apply same rendering hints
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw filled and/or outline
        if (isFilled && transparentFill) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.fillOval(x, y, diameter, diameter);
        }

        if (transparentOutline) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            maskG2d.drawOval(x, y, diameter, diameter);
        }

        // If only fill is transparent, still draw the normal outline
        if (isFilled && transparentFill && !transparentOutline) {
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, diameter, diameter);
        }

        // If only outline is transparent, still draw the normal fill
        if (isFilled && !transparentFill && transparentOutline) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, diameter, diameter);
        }

        maskG2d.dispose();

        // Apply transparency where mask is white
        applyTransparencyMask(image, maskImage, g2d.getClip());
    }

    /**
     * Applies transparency to pixels in the image where the mask is non-zero
     */
    private void applyTransparencyMask(BufferedImage image, BufferedImage maskImage, Shape clip) {
        // Apply transparency to pixels where the mask is non-zero
        // Use the actual dimensions of the target image
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // No action needed for mouse scroll
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        if (isCenterBased) {
            return isFilled ? "Circle tool (center-based, filled)" : "Circle tool (center-based, outline)";
        } else {
            return isFilled ? "Circle tool (filled)" : "Circle tool (outline)";
        }
    }

    private void drawCornerBasedCircle(Graphics2D g2d, Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        if (width > height) {
            height = width;
            if (end.y < start.y) {
                y = start.y - height;
            }
        } else {
            width = height;
            if (end.x < start.x) {
                x = start.x - width;
            }
        }

        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, width, height);
        }

        g2d.setColor(canvas.getDrawingColor());
        g2d.drawOval(x, y, width, height);
    }

    private void drawCenterBasedCircle(Graphics2D g2d, Point center, Point edge) {
        int radius = (int) Math.sqrt(Math.pow(edge.x - center.x, 2) + Math.pow(edge.y - center.y, 2));
        int x = center.x - radius;
        int y = center.y - radius;
        int diameter = radius * 2;

        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, diameter, diameter);
        }

        g2d.setColor(canvas.getDrawingColor());
        g2d.drawOval(x, y, diameter, diameter);
    }

    public boolean isFilled() {
        return isFilled;
    }

    public void setFilled(boolean filled) {
        isFilled = filled;
    }

    public void setCenterBased(boolean centerBased) {
        isCenterBased = centerBased;
    }

    public boolean isCenterBased() {
        return isCenterBased;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}