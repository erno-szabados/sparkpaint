package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.BrushToolRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * BrushTool is a drawing tool that allows users to paint on a canvas using various brush shapes.
 * It supports different brush shapes (square, circle, spray), size adjustments, and antialiasing.
 * The tool can also blend colors based on the specified blend strength.
 * Actual rendering is delegated to BrushToolRenderer.
 */
public class BrushTool implements DrawingTool {

    public enum BrushShape {
        SQUARE,
        CIRCLE,
        SPRAY
    }

    public static final int DEFAULT_BLEND_STRENGTH = 25;
    public static final int DEFAULT_SPRAY_DENSITY = 25;
    public static final int DEFAULT_SPRAY_SIZE = 25;

    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private BrushShape shape = BrushShape.SPRAY;
    private int size = DEFAULT_SPRAY_SIZE;
    private int sprayDensity = DEFAULT_SPRAY_DENSITY;
    private boolean useAntiAliasing = true;
    private float maxBlendStrength = 0.1f;  // Range: 0.01f to 1.0f

    // Will be initialized on first use
    private BrushToolRenderer renderer;

    public BrushTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    /**
     * Handles mouse press events to initiate drawing.
     * Converts the screen coordinates to world coordinates and checks if the click is within a selection.
     * If a selection exists, it only allows drawing within that selection.
     *
     * @param e The mouse event triggering the drawing.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Save last point and update canvas
        lastPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        canvas.saveToUndoStack();

        // Get appropriate graphics context and draw
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
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        drawBrush(e, drawPoint, g2d);
        g2d.dispose();

        canvas.repaint();
    }

    /**
     * Handles mouse drag events to continue drawing.
     * Converts the screen coordinates to world coordinates and checks if the drag is within a selection.
     * If a selection exists, it only allows drawing within that selection.
     *
     * @param e The mouse event triggering the drawing.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if dragging inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
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
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        drawBrush(e, drawPoint, g2d);
        g2d.dispose();

        lastPoint = currentPoint;
        canvas.repaint();
    }

    /**
     * Delegates drawing to the BrushToolRenderer.
     */
    private void drawBrush(MouseEvent e, Point p, Graphics2D g2d) {
        Color paintColor;
        if (SwingUtilities.isLeftMouseButton(e)) {
            paintColor = canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            paintColor = canvas.getFillColor();
        } else {
            return;
        }

        // Lazy initialization of renderer if needed
        if (renderer == null) {
            renderer = new BrushToolRenderer(canvas);
        }

        BufferedImage targetImage;
        Selection selection = canvas.getSelection();
        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
        } else {
            targetImage = canvas.getCurrentLayerImage();
        }

        // Delegate to renderer
        renderer.drawBrush(
                targetImage,
                shape,
                p.x, p.y, size,
                paintColor,
                sprayDensity,
                useAntiAliasing,
                maxBlendStrength,
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
        switch (shape) {
            case SQUARE:
                return "Brush tool: Square mode";
            case CIRCLE:
                return "Brush tool: Circle mode";
            case SPRAY:
                return "Brush tool: Spray mode";
            default:
                return "Brush tool: Unknown mode";
        }
    }

    public void setMaxBlendStrength(float strength) {
        this.maxBlendStrength = Math.max(0.01f, Math.min(1.0f, strength));
    }

    public float getMaxBlendStrength() {
        return maxBlendStrength;
    }

    public void setShape(BrushShape shape) {
        this.shape = shape;
        canvas.setCursorShape(shape);
    }

    public void setSize(int size) {
        this.size = size;
        canvas.setCursorSize(size);
    }

    public void setSprayDensity(int sprayDensity) {
        this.sprayDensity = sprayDensity;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}