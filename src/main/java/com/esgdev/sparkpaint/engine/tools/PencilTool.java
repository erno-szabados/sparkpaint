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
        Color drawColor = getDrawingColor(e);

        // Check if using transparency
        if (drawColor.getAlpha() == 0) {
            drawTransparentPoint(e, point);
        } else {
            Graphics2D g2d = getGraphicsForDrawing();
            if (g2d == null) return;

            configureGraphics(g2d, drawColor);

            // Convert point if drawing in selection
            Point drawPoint = adjustPointForSelection(point);

            // Draw a single point (as a 1-pixel line)
            g2d.drawLine(drawPoint.x, drawPoint.y, drawPoint.x, drawPoint.y);
            g2d.dispose();

            // Set the modified flag if we drew into a selection
            Selection selection = canvas.getSelection();
            if (selection != null && selection.hasOutline()) {
                selection.setModified(true);
            }
        }
    }

    private void drawTransparentPoint(MouseEvent e, Point point) {
        Selection selection = canvas.getSelection();
        BufferedImage targetImage;
        Point drawPoint;

        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
            Rectangle bounds = selection.getBounds();
            drawPoint = new Point(point.x - bounds.x, point.y - bounds.y);
            selection.setModified(true);
        } else {
            targetImage = canvas.getCurrentLayerImage();
            drawPoint = point;
        }

        // Get drawing bounds
        int x = drawPoint.x;
        int y = drawPoint.y;

        // Check bounds
        if (x < 0 || y < 0 || x >= targetImage.getWidth() || y >= targetImage.getHeight()) {
            return;
        }

        // Set full transparency (alpha = 0)
        int newRGB = targetImage.getRGB(x, y) & 0x00FFFFFF;
        targetImage.setRGB(x, y, newRGB);
    }

    private void drawLine(MouseEvent e, Point from, Point to) {
        Color drawColor = getDrawingColor(e);

        // Check if using transparency
        if (drawColor.getAlpha() == 0) {
            drawTransparentLine(e, from, to);
        } else {
            Graphics2D g2d = getGraphicsForDrawing();
            if (g2d == null) return;

            configureGraphics(g2d, drawColor);

            // Convert points if drawing in selection
            Point fromPoint = adjustPointForSelection(from);
            Point toPoint = adjustPointForSelection(to);

            g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
            g2d.dispose();

            // Set the modified flag if we drew into a selection
            Selection selection = canvas.getSelection();
            if (selection != null && selection.hasOutline()) {
                selection.setModified(true);
            }
        }
    }

    private void drawTransparentLine(MouseEvent e, Point p1, Point p2) {
        Selection selection = canvas.getSelection();
        BufferedImage targetImage;
        Graphics2D g2d;
        Point fromPoint, toPoint;

        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
            g2d = canvas.getDrawingGraphics();
            Rectangle bounds = selection.getBounds();
            fromPoint = new Point(p1.x - bounds.x, p1.y - bounds.y);
            toPoint = new Point(p2.x - bounds.x, p2.y - bounds.y);
            selection.setModified(true);
        } else {
            targetImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) targetImage.getGraphics();
            fromPoint = p1;
            toPoint = p2;
        }

        // Check bounds - skip if completely out of bounds
        if ((fromPoint.x < 0 && toPoint.x < 0) ||
                (fromPoint.y < 0 && toPoint.y < 0) ||
                (fromPoint.x >= targetImage.getWidth() && toPoint.x >= targetImage.getWidth()) ||
                (fromPoint.y >= targetImage.getHeight() && toPoint.y >= targetImage.getHeight())) {
            g2d.dispose();
            return;
        }

        try {
            // Create a temporary mask image for the line
            BufferedImage maskImage = new BufferedImage(targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Set up the mask with the same stroke settings
            maskG2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Draw white line on mask
            maskG2d.setColor(Color.WHITE);
            maskG2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
            maskG2d.dispose();

            // Calculate bounds of affected area for efficiency
            int lineWidth = (int) Math.ceil(canvas.getLineThickness());
            int minX = Math.max(0, Math.min(fromPoint.x, toPoint.x) - lineWidth);
            int minY = Math.max(0, Math.min(fromPoint.y, toPoint.y) - lineWidth);
            int maxX = Math.min(targetImage.getWidth(), Math.max(fromPoint.x, toPoint.x) + lineWidth);
            int maxY = Math.min(targetImage.getHeight(), Math.max(fromPoint.y, toPoint.y) + lineWidth);

            // Apply transparency to pixels where the mask is non-zero
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Check if this pixel is within clip region
                    Shape clip = g2d.getClip();
                    if (clip == null || clip.contains(x, y)) {
                        int maskRGB = maskImage.getRGB(x, y);
                        // Only process pixels where the mask is non-zero
                        if ((maskRGB & 0xFF000000) != 0) {
                            // Set full transparency (alpha = 0)
                            int newRGB = targetImage.getRGB(x, y) & 0x00FFFFFF;
                            targetImage.setRGB(x, y, newRGB);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Exception in drawTransparentLine: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            g2d.dispose();
        }
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

    private Color getDrawingColor(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            return canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            return canvas.getFillColor();
        }
        // Default to drawing color
        return canvas.getDrawingColor();
    }

    private void configureGraphics(Graphics2D g2d, Color color) {
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(color);
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