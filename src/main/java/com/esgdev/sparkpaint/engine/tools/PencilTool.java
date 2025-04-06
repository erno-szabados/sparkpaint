package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.PencilToolRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class PencilTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private boolean isDrawing = false;

    // Add PencilToolRenderer field
    private final PencilToolRenderer renderer;

    public PencilTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        // Initialize the renderer
        this.renderer = new PencilToolRenderer();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            isDrawing = false;
            return; // Don't draw outside selection when one exists
        }

        // Save last point and update canvas
        lastPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        isDrawing = true;
        canvas.saveToUndoStack();

        // Get appropriate drawing context and draw a single point
        DrawContext drawContext = prepareDrawContext(selection, lastPoint);
        renderer.drawPoint(drawContext.targetImage, drawContext.adjustedPoint,
                getDrawingColor(e), canvas.getLineThickness());
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if dragging inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            isDrawing = false;
            return; // Don't draw outside selection when one exists
        }

        // If we weren't drawing before, but now we can, set a new start point
        if (!isDrawing) {
            lastPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            isDrawing = true;
            return;
        }

        // Get current point in drawing coordinates
        Point currentPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Draw line segment from last point to current point
        DrawContext drawContext = prepareDrawContext(selection, lastPoint, currentPoint);
        renderer.drawLine(drawContext.targetImage, drawContext.adjustedStart,
                drawContext.adjustedEnd, getDrawingColor(e), canvas.getLineThickness());

        // Update last point and repaint
        lastPoint = currentPoint;
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        isDrawing = false;  // Reset drawing state when mouse is released
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
        return "Pencil tool selected";
    }

    // Helper class for drawing context with one point
    private static class DrawContext {
        final BufferedImage targetImage;
        final Point adjustedPoint;
        final Point adjustedStart;
        final Point adjustedEnd;

        // Constructor for single point
        DrawContext(BufferedImage targetImage, Point adjustedPoint) {
            this.targetImage = targetImage;
            this.adjustedPoint = adjustedPoint;
            this.adjustedStart = null;
            this.adjustedEnd = null;
        }

        // Constructor for line segment
        DrawContext(BufferedImage targetImage, Point adjustedStart, Point adjustedEnd) {
            this.targetImage = targetImage;
            this.adjustedPoint = null;
            this.adjustedStart = adjustedStart;
            this.adjustedEnd = adjustedEnd;
        }
    }

    private DrawContext prepareDrawContext(Selection selection, Point point) {
        Point adjustedPoint = point;
        BufferedImage targetImage;

        if (selection != null && selection.hasOutline()) {
            selection.setModified(true);
            targetImage = selection.getContent();

            // Adjust coordinates relative to selection bounds
            Rectangle bounds = selection.getBounds();
            adjustedPoint = new Point(point.x - bounds.x, point.y - bounds.y);
        } else {
            targetImage = canvas.getCurrentLayerImage();
        }

        return new DrawContext(targetImage, adjustedPoint);
    }

    private DrawContext prepareDrawContext(Selection selection, Point start, Point end) {
        Point adjustedStart = start;
        Point adjustedEnd = end;
        BufferedImage targetImage;

        if (selection != null && selection.hasOutline()) {
            selection.setModified(true);
            targetImage = selection.getContent();

            // Adjust coordinates relative to selection bounds
            Rectangle bounds = selection.getBounds();
            adjustedStart = new Point(start.x - bounds.x, start.y - bounds.y);
            adjustedEnd = new Point(end.x - bounds.x, end.y - bounds.y);
        } else {
            targetImage = canvas.getCurrentLayerImage();
        }

        return new DrawContext(targetImage, adjustedStart, adjustedEnd);
    }

    private Color getDrawingColor(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            return canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            return canvas.getFillColor();
        }
        // Default to drawing color
        return canvas.getDrawingColor();
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        // Forward to renderer
        renderer.setAntiAliasing(useAntiAliasing);
    }
}