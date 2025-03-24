package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class LineTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean useAntiAliasing = true;

    public LineTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //SelectionManager selectionManager = canvas.getSelectionManager();
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
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Set color based on mouse button
        if (SwingUtilities.isLeftMouseButton(e)) {
            g2d.setColor(canvas.getDrawingColor());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            g2d.setColor(canvas.getFillColor());
        }

        applySelectionClip(g2d, selection);

        // Draw the preview line
        g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
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
        if (selection != null && selection.hasOutline()) {
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            Point selectionStartPoint = new Point(startPoint.x - bounds.x, startPoint.y - bounds.y);
            Point selectionEndPoint = new Point(point.x - bounds.x, point.y - bounds.y);

            // Store original points temporarily
            Point originalStart = startPoint;
            Point originalEnd = point;

            // Use adjusted points for drawing in selection
            startPoint = selectionStartPoint;
            point = selectionEndPoint;
        } else {
            // Draw on current layer
            BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) currentLayerImage.getGraphics();
        }

        // Apply rendering settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Set color based on mouse button
        if (SwingUtilities.isLeftMouseButton(e)) {
            g2d.setColor(canvas.getDrawingColor());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            g2d.setColor(canvas.getFillColor());
        }

        // Draw the final line
        g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
        g2d.dispose();

        // Reset startPoint to original value if it was modified for selection
        if (selection != null && selection.hasOutline()) {
            startPoint = null; // We're done with drawing
        } else {
            startPoint = null;
        }

        // Clear the temp canvas and repaint
        canvas.setToolCanvas(null);
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
        return "Line tool selected";
    }

    // Setter for anti-aliasing
    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}