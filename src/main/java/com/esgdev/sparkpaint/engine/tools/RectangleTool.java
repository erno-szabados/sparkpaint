
package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class RectangleTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isFilled;
    private boolean useAntiAliasing = true;

    public RectangleTool(DrawingCanvas canvas) {
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

        // Calculate rectangle dimensions
        int x = Math.min(startPoint.x, point.x);
        int y = Math.min(startPoint.y, point.y);
        int width = Math.abs(point.x - startPoint.x);
        int height = Math.abs(point.y - startPoint.y);

        // Check if Shift key is pressed
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        if (isShiftDown) {
            // Make it a square
            int side = Math.max(width, height);
            width = side;
            height = side;
        }

        // Check if we're using transparent colors
        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        if (transparentFill || transparentOutline) {
            // Define dash pattern for the preview
            float[] dashPattern = {8.0f, 8.0f};
            float lineThickness = canvas.getLineThickness();

            // Draw filled preview if needed (semi-transparent)
            if (isFilled) {
                if (transparentFill) {
                    // Use red with semi-transparency to indicate transparent fill
                    g2d.setColor(new Color(255, 0, 0, 32));
                    g2d.fillRect(x, y, width, height);
                } else {
                    // Show normal fill color with reduced opacity
                    Color fillColor = canvas.getFillColor();
                    g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                            fillColor.getBlue(), 128));
                    g2d.fillRect(x, y, width, height);
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
                g2d.drawRect(x, y, width, height);

                // Draw black line on top (narrower, offset dash pattern)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(lineThickness,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dashPattern, dashPattern[0]));
                g2d.drawRect(x, y, width, height);
            } else {
                // Normal outline
                g2d.setColor(canvas.getDrawingColor());
                g2d.setStroke(new BasicStroke(lineThickness));
                g2d.drawRect(x, y, width, height);
            }
        } else {
            // For regular (non-transparent) drawing, use the standard approach
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));

            // Draw filled rectangle if needed
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillRect(x, y, width, height);
            }

            // Draw rectangle outline
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawRect(x, y, width, height);
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

        // Calculate rectangle dimensions
        int x = Math.min(adjustedStartPoint.x, adjustedEndPoint.x);
        int y = Math.min(adjustedStartPoint.y, adjustedEndPoint.y);
        int width = Math.abs(adjustedEndPoint.x - adjustedStartPoint.x);
        int height = Math.abs(adjustedEndPoint.y - adjustedStartPoint.y);

        // Check if Shift key is pressed
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        if (isShiftDown) {
            // Make it a square
            int side = Math.max(width, height);
            width = side;
            height = side;
        }

        // Check if we're using transparent colors
        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        // For regular (non-transparent) drawing, use the standard approach
        if (!transparentFill && !transparentOutline) {
            // Draw filled rectangle if needed
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillRect(x, y, width, height);
            }

            // Draw rectangle outline
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawRect(x, y, width, height);
        } else {
            // For transparent drawing, use special handling
            drawTransparentRectangle(targetImage, g2d, x, y, width, height, transparentFill, transparentOutline);
        }

        g2d.dispose();

        // Clear the temp canvas and reset state
        canvas.setToolCanvas(null);
        startPoint = null;
        canvas.repaint();
    }

    private void drawTransparentRectangle(BufferedImage image, Graphics2D g2d, int x, int y, int width, int height,
                                          boolean transparentFill, boolean transparentOutline) {
        // Create a mask image for transparency - use the target image dimensions
        BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();

        // Apply same rendering hints
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw filled and/or outline
        if (isFilled && transparentFill) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.fillRect(x, y, width, height);
        }

        if (transparentOutline) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            maskG2d.drawRect(x, y, width, height);
        }

        // If only fill is transparent, still draw the normal outline
        if (isFilled && transparentFill && !transparentOutline) {
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawRect(x, y, width, height);
        }

        // If only outline is transparent, still draw the normal fill
        if (isFilled && !transparentFill && transparentOutline) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillRect(x, y, width, height);
        }

        maskG2d.dispose();

        // Apply transparency where mask is white - pass the dimensions of the target image
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
        return isFilled ? "Rectangle tool (filled)" : "Rectangle tool (outline)";
    }

    public void setFilled(boolean filled) {
        isFilled = filled;
    }

    public boolean isFilled() {
        return isFilled;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}