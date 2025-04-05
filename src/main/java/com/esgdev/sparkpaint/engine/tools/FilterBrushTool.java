package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * FilterBrushTool applies image filters (blur, noise, dither) using a brush-like interface.
 * The actual filter implementation is delegated to a FilterBrushRenderer.
 */
public class FilterBrushTool implements DrawingTool {

    public enum FilterType {
        BLUR,
        NOISE,
        DITHER
    }

    public static final int DEFAULT_SIZE = 25;
    public static final int DEFAULT_STRENGTH = 25;

    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private FilterType filterType = FilterType.BLUR;
    private int size = DEFAULT_SIZE;
    private float strength = DEFAULT_STRENGTH / 100f;  // Range: 0.01f to 1.0f

    // Will be initialized on first use
    private FilterBrushRenderer renderer;

    public FilterBrushTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        // Initialize cursor shape and size
        canvas.setCursorShape(BrushTool.BrushShape.CIRCLE);
        canvas.setCursorSize(DEFAULT_SIZE);
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
            return; // Don't apply filter outside selection when one exists
        }

        // Save last point and update canvas
        lastPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        canvas.saveToUndoStack();

        // Get appropriate graphics context and apply filter
        Graphics2D g2d;
        Point drawPoint = lastPoint;

        if (selection != null && selection.hasOutline()) {
            selection.setModified(true);
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            drawPoint = new Point(lastPoint.x - bounds.x, lastPoint.y - bounds.y);
        } else {
            // Draw on current layer
            g2d = canvas.getCurrentLayerImage().createGraphics();
        }

        applyFilter(e, drawPoint, g2d);
        g2d.dispose();

        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if dragging inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't apply filter outside selection when one exists
        }

        // Get current point and update
        Point currentPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context for drawing
        Graphics2D g2d;
        Point drawPoint = currentPoint;

        if (selection != null && selection.hasOutline()) {
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            drawPoint = new Point(currentPoint.x - bounds.x, currentPoint.y - bounds.y);
        } else {
            // Draw on current layer
            g2d = canvas.getCurrentLayerImage().createGraphics();
        }

        applyFilter(e, drawPoint, g2d);
        g2d.dispose();

        lastPoint = currentPoint;
        canvas.repaint();
    }

    private void applyFilter(MouseEvent e, Point p, Graphics2D g2d) {
        BufferedImage targetImage;
        Selection selection = canvas.getSelection();
        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
        } else {
            targetImage = canvas.getCurrentLayerImage();
        }

        // Lazy initialization of renderer if needed
        if (renderer == null) {
            renderer = new FilterBrushRenderer(canvas);
        }

        // Apply the filter at the given point with current settings
        int x = p.x - size / 2;
        int y = p.y - size / 2;

        renderer.applyFilter(
                targetImage,
                filterType,
                x, y, size,
                strength,
                g2d.getClip()
        );
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
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
        switch (filterType) {
            case BLUR:
                return "Filter Brush: Blur mode";
            case NOISE:
                return "Filter Brush: Noise mode";
            case DITHER:
                return "Filter Brush: Dither mode";
            default:
                return "Filter Brush: Unknown mode";
        }
    }

    // Add this to FilterBrushTool class
    public void initializeCursor() {
        // Set circular cursor for filter brush
        canvas.setCursorShape(BrushTool.BrushShape.CIRCLE);
        canvas.setCursorSize(size);
    }

    // Update setSize method
    public void setSize(int size) {
        this.size = size;
        canvas.setCursorSize(size);
        canvas.setCursorShape(BrushTool.BrushShape.CIRCLE); // Always use circle shape for filters
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public void setStrength(float strength) {
        this.strength = Math.max(0.01f, Math.min(1.0f, strength));
    }
}