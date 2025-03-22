package com.esgdev.sparkpaint.engine.selection;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Manages the selection of areas within the drawing canvas.
 * Handles creating, modifying, and deleting selections.
 */
public class SelectionManager {
    private final DrawingCanvas canvas;
    public Selection selection;

    public SelectionManager(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    public void setSelection(Selection selection) {
        this.selection = selection;
    }

    public Selection getSelection() {
        return selection;
    }

    public void clearSelection() {
        if (selection != null) {
            selection.clear();
            canvas.repaint();
        }
    }

    /**
     * Selects all visible layers in the canvas and creates a selection rectangle around them.
     */
    public void selectAll() {
        canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_SELECTION);

        // Create a composite image of all visible layers
        BufferedImage compositeImage = createCompositeImage();

        if (compositeImage != null) {
            Rectangle rect = new Rectangle(0, 0, compositeImage.getWidth(), compositeImage.getHeight());
            BufferedImage content = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = content.createGraphics();
            g2d.drawImage(compositeImage, 0, 0, null);
            g2d.dispose();
            selection = new Selection(rect, content);
            canvas.notifyClipboardStateChanged();
            canvas.repaint();
        }
    }

    /**
     * Creates a composite image of all visible layers in the canvas.
     *
     * @return A BufferedImage containing the composite of all visible layers.
     */
    private BufferedImage createCompositeImage() {
        // Get the dimensions from the current layer
        BufferedImage currentLayer = canvas.getLayerManager().getCurrentLayerImage();
        if (currentLayer == null) return null;

        int width = currentLayer.getWidth();
        int height = currentLayer.getHeight();

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        // Draw all visible layers
        List<Layer> layers = canvas.getLayerManager().getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }

        g2d.dispose();
        return composite;
    }

    /**
     * Deletes the current selection from the current layer.
     */
    public void deleteSelection() {
        if (selection == null || selection.getBounds() == null) {
            return;
        }

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getLayerManager().getCurrentLayerImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));

        GeneralPath path = selection.getPath();
        if (path != null) {
            g2d.fill(path);
        }

        g2d.dispose();
        selection.clear();
        canvas.notifyClipboardStateChanged();
        canvas.repaint();
    }

    /**
     * Rotates the selection content by the specified degrees.
     *
     * @param degrees The angle in degrees to rotate the selection.
     */
    public void rotateSelection(int degrees) {
        if (selection == null || selection.getContent() == null) return;
        selection.rotate(degrees);
        canvas.repaint();
    }

    /**
     * Flips the selection content either horizontally or vertically.
     *
     * @param horizontal true to flip horizontally, false to flip vertically
     */
    public void flipSelection(boolean horizontal) {
        if (selection == null || selection.getContent() == null) return;
        if (horizontal) {
            selection.flipHorizontal();
        } else {
            selection.flipVertical();
        }
        canvas.repaint();
    }

    /**
     * Gets a graphics context appropriate for drawing - either for the current selection or current layer.
     *
     * @param canvas The drawing canvas
     * @return Graphics2D context configured with proper transforms and clipping
     */
    public Graphics2D getDrawingGraphics(DrawingCanvas canvas) {
        Selection selection = getSelection();
        if (selection == null || !selection.hasOutline()) {
            // No selection, return current layer graphics
            return (Graphics2D) canvas.getLayerManager().getCurrentLayerImage().getGraphics();
        }

        // We have a selection - prepare a graphics context for its content
        BufferedImage content = selection.getContent();
        Rectangle bounds = selection.getBounds();

        if (content == null || bounds == null) {
            return (Graphics2D) canvas.getLayerManager().getCurrentLayerImage().getGraphics();
        }

        Graphics2D g2d = content.createGraphics();

        // Apply clipping based on selection type
        // For path selection, create translated path for clipping
        GeneralPath path = selection.getPath();
        if (path != null) {
            GeneralPath translatedPath = new GeneralPath(path);
            translatedPath.transform(AffineTransform.getTranslateInstance(-bounds.x, -bounds.y));
            g2d.setClip(translatedPath);
        }

        return g2d;
    }

    /**
     * Translates screen coordinates to selection-local coordinates if there's a selection,
     * or to canvas world coordinates otherwise.
     *
     * @param screenPoint Point in screen coordinates
     * @param zoomFactor  Current canvas zoom factor
     * @return Point in the appropriate coordinate system
     */
    public Point getDrawingCoordinates(Point screenPoint, float zoomFactor) {
        // First convert to canvas world coordinates
        Point worldPoint = DrawingTool.screenToWorld(zoomFactor, screenPoint);

        Selection selection = getSelection();
        if (selection == null || !selection.contains(worldPoint)) {
            // No selection or point is outside selection, return world coordinates
            return worldPoint;
        }

        // Convert to selection-local coordinates
        Rectangle bounds = selection.getBounds();
        return new Point(worldPoint.x - bounds.x, worldPoint.y - bounds.y);
    }

    /**
     * Checks if the given world point is within the current selection.
     *
     * @param worldPoint Point in world coordinates
     * @return true if the point is within a selection, false otherwise
     */
    public boolean isWithinSelection(Point worldPoint) {
        Selection selection = getSelection();
        return selection != null && selection.hasOutline() && selection.contains(worldPoint);
    }

}