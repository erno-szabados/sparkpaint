package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class PencilTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private boolean useAntiAliasing = true;
    private boolean isDrawing = false;

    public PencilTool(DrawingCanvas canvas) {
        this.canvas = canvas;
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

        // Get appropriate graphics context and draw a single point
        drawPoint(e, lastPoint);
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
        drawLine(e, lastPoint, currentPoint);

        // Update last point and repaint
        lastPoint = currentPoint;
        canvas.repaint();
    }

    private void drawPoint(MouseEvent e, Point point) {
        Graphics2D g2d = getGraphicsForDrawing();
        if (g2d == null) return;

        configureGraphics(e, g2d);

        // Convert point if drawing in selection
        Point drawPoint = adjustPointForSelection(point);

        // Draw a single point (as a 1-pixel line)
        g2d.drawLine(drawPoint.x, drawPoint.y, drawPoint.x, drawPoint.y);
        g2d.dispose();
    }

    private void drawLine(MouseEvent e, Point from, Point to) {
        Graphics2D g2d = getGraphicsForDrawing();
        if (g2d == null) return;

        configureGraphics(e, g2d);

        // Convert points if drawing in selection
        Point fromPoint = adjustPointForSelection(from);
        Point toPoint = adjustPointForSelection(to);

        g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
        g2d.dispose();
    }

    private Graphics2D getGraphicsForDrawing() {
        Selection selection = canvas.getSelection();

        if (selection != null && selection.hasOutline()) {
            return canvas.getDrawingGraphics();
        } else {
            BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
            return currentLayerImage.createGraphics();
        }
    }

    private Point adjustPointForSelection(Point point) {
        Selection selection = canvas.getSelection();

        if (selection != null && selection.hasOutline()) {
            Rectangle bounds = selection.getBounds();
            return new Point(point.x - bounds.x, point.y - bounds.y);
        }

        return point;
    }

    private void configureGraphics(MouseEvent e, Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        if (SwingUtilities.isLeftMouseButton(e)) {
            g2d.setColor(canvas.getDrawingColor());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            g2d.setColor(canvas.getFillColor());
        }
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

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}