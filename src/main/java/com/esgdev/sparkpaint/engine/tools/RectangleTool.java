package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

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

        //SelectionManager selectionManager = canvas.getSelectionManager();

        // Get current point in appropriate coordinate system
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Create temporary canvas for preview
        BufferedImage tempCanvas = canvas.getTempCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear the temp canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply rendering settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Calculate rectangle dimensions
        int x = Math.min(startPoint.x, point.x);
        int y = Math.min(startPoint.y, point.y);
        int width = Math.abs(point.x - startPoint.x);
        int height = Math.abs(point.y - startPoint.y);

        // Draw filled rectangle if needed
        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillRect(x, y, width, height);
        }

        // Draw rectangle outline
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawRect(x, y, width, height);
        g2d.dispose();

        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (startPoint == null) return;

        //SelectionManager selectionManager = canvas.getSelectionManager();
        Selection selection = canvas.getSelection();

        // Get current point in appropriate coordinate system
        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context for drawing
        Graphics2D g2d;
        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
        } else {
            // Draw on current layer instead of main image
            BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) currentLayerImage.getGraphics();
        }

        // Apply rendering settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Calculate rectangle dimensions
        int x = Math.min(startPoint.x, point.x);
        int y = Math.min(startPoint.y, point.y);
        int width = Math.abs(point.x - startPoint.x);
        int height = Math.abs(point.y - startPoint.y);

        // Draw filled rectangle if needed
        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillRect(x, y, width, height);
        }

        // Draw rectangle outline
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawRect(x, y, width, height);
        g2d.dispose();

        // Clear the temp canvas and reset state
        canvas.setTempCanvas(null);
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