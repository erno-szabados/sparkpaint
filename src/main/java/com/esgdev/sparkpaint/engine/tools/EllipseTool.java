package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class EllipseTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isFilled;
    private boolean useAntiAliasing = true;
    private boolean isCenterBased = false;

    public EllipseTool(DrawingCanvas canvas) {
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
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Create temporary canvas for preview
        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear the temp canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply rendering settings
        configureGraphics(g2d);
        applySelectionClip(g2d, selection);

        // Draw the circle with preview settings
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        drawCircle(g2d, startPoint, point, true, isShiftDown);

        g2d.dispose();
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (startPoint == null) return;

        Selection selection = canvas.getSelection();
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context and adjust points if needed
        DrawContext drawContext = prepareDrawContext(selection, startPoint, point);

        // Apply rendering settings
        configureGraphics(drawContext.g2d);

        // Draw the circle
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        drawCircle(drawContext.g2d, drawContext.start, drawContext.end, false, isShiftDown);

        // Apply transparency if needed
        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        if (transparentFill || transparentOutline) {
            Rectangle ellipseBounds = calculateEllipseBounds(drawContext.start, drawContext.end, isShiftDown);
            applyTransparency(drawContext.targetImage, ellipseBounds, transparentFill, transparentOutline);
        }

        drawContext.g2d.dispose();

        // Clear the temp canvas and reset state
        canvas.setToolCanvas(null);
        startPoint = null;
        canvas.repaint();
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
        String shapeType = "Circle tool";
        String baseType = isCenterBased ? " (center-based" : " (";
        String fillType = isFilled ? ", filled)" : ", outline)";
        return shapeType + baseType + fillType;
    }

    // Helper classes and methods

    private static class DrawContext {
        final Graphics2D g2d;
        final BufferedImage targetImage;
        final Point start;
        final Point end;

        DrawContext(Graphics2D g2d, BufferedImage targetImage, Point start, Point end) {
            this.g2d = g2d;
            this.targetImage = targetImage;
            this.start = start;
            this.end = end;
        }
    }

    private DrawContext prepareDrawContext(Selection selection, Point start, Point end) {
        Graphics2D g2d;
        Point adjustedStart = start;
        Point adjustedEnd = end;
        BufferedImage targetImage;

        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
            selection.setModified(true);
            targetImage = selection.getContent();

            // Adjust coordinates relative to selection bounds
            Rectangle bounds = selection.getBounds();
            adjustedStart = new Point(start.x - bounds.x, start.y - bounds.y);
            adjustedEnd = new Point(end.x - bounds.x, end.y - bounds.y);
        } else {
            targetImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) targetImage.getGraphics();
        }

        return new DrawContext(g2d, targetImage, adjustedStart, adjustedEnd);
    }

    private void configureGraphics(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private Rectangle calculateEllipseBounds(Point start, Point end, boolean isShiftDown) {
        if (isCenterBased) {
            int radius = (int) Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
            return new Rectangle(start.x - radius, start.y - radius, radius * 2, radius * 2);
        } else {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(end.x - start.x);
            int height = Math.abs(end.y - start.y);

            if (isShiftDown) {
                // Force a circle by making width equal to height
                int diameter = Math.max(width, height);
                return new Rectangle(x, y, diameter, diameter);
            }

            return new Rectangle(x, y, width, height);
        }
    }

    private void drawCircle(Graphics2D g2d, Point start, Point end, boolean isPreview, boolean isShiftDown) {
        Rectangle bounds = calculateEllipseBounds(start, end, isShiftDown);

        boolean transparentFill = isFilled && canvas.getFillColor().getAlpha() == 0;
        boolean transparentOutline = canvas.getDrawingColor().getAlpha() == 0;

        // For preview with transparency, use dashed strokes
        if (isPreview && (transparentFill || transparentOutline)) {
            drawTransparencyPreview(g2d, bounds, transparentFill, transparentOutline);
        } else if (!isPreview && (transparentFill || transparentOutline)) {
            // For final drawing with transparency, draw only visible parts
            if (isFilled && !transparentFill) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            if (!transparentOutline) {
                g2d.setColor(canvas.getDrawingColor());
                g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            // Normal drawing (no transparency)
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void drawTransparencyPreview(Graphics2D g2d, Rectangle bounds, boolean transparentFill, boolean transparentOutline) {
        float lineThickness = canvas.getLineThickness();
        float[] dashPattern = {8.0f, 8.0f};

        // For filled preview with transparency
        if (isFilled) {
            if (transparentFill) {
                // Use red with semi-transparency to indicate transparent fill
                g2d.setColor(new Color(255, 0, 0, 32));
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            } else {
                // Show normal fill color with reduced opacity
                Color fillColor = canvas.getFillColor();
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                        fillColor.getBlue(), 128));
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
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
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);

            // Draw black line on top (narrower, offset dash pattern)
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(lineThickness,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f, dashPattern, dashPattern[0]));
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            // Normal outline
            g2d.setColor(canvas.getDrawingColor());
            g2d.setStroke(new BasicStroke(lineThickness));
            g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void applyTransparency(BufferedImage image, Rectangle bounds, boolean transparentFill, boolean transparentOutline) {
        // Create mask for transparency
        BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();

        // Configure graphics
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw mask for transparent parts
        if (transparentFill && isFilled) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        if (transparentOutline) {
            maskG2d.setColor(Color.WHITE);
            maskG2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            maskG2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        maskG2d.dispose();

        // Apply transparency mask
        // Only process pixels within bounds for efficiency
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                // Check bounds to avoid array out of bounds
                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
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

    // Public setters

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