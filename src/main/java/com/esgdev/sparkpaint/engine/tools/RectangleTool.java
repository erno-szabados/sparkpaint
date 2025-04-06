package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.RectangleToolRenderer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class RectangleTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isFilled;

    // Add RectangleToolRenderer field
    private final RectangleToolRenderer renderer;

    public RectangleTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.isFilled = false;
        // Initialize the renderer
        this.renderer = new RectangleToolRenderer();
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

        applySelectionClip(g2d, selection);

        // Calculate rectangle bounds - is shift down for perfect square?
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        Rectangle bounds = renderer.calculateRectangleBounds(startPoint, point, isShiftDown);

        // Draw the rectangle preview
        renderer.drawRectangle(tempCanvas, g2d, bounds,
                canvas.getDrawingColor(), canvas.getFillColor(),
                canvas.getLineThickness(), isFilled, true);

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

        // Calculate rectangle bounds - is shift down for perfect square?
        boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        Rectangle bounds = renderer.calculateRectangleBounds(drawContext.start, drawContext.end, isShiftDown);

        // Draw the final rectangle
        renderer.drawRectangle(drawContext.targetImage, drawContext.g2d, bounds,
                canvas.getDrawingColor(), canvas.getFillColor(),
                canvas.getLineThickness(), isFilled, false);

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
        return isFilled ? "Rectangle tool (filled)" : "Rectangle tool (outline)";
    }

    // Helper class for drawing context
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

    // Public setters
    public boolean isFilled() {
        return isFilled;
    }

    public void setFilled(boolean filled) {
        isFilled = filled;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        // Forward to renderer
        renderer.setAntiAliasing(useAntiAliasing);
    }
}