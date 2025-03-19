
package com.esgdev.sparkpaint.engine.selection;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

/// Manages the selection state and operations on the drawing canvas.
public class SelectionManager {
    private final DrawingCanvas canvas;
    private Selection selection;

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
        selection.clear();
        canvas.repaint();
    }

    public void selectAll() {
        canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_SELECTION);
        BufferedImage image = canvas.getImage();
        if (image != null) {
            Rectangle rect = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            BufferedImage content = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = content.createGraphics();
            g2d.drawImage(image, -rect.x, -rect.y, null);
            g2d.dispose();
            selection = new RectangleSelection(rect, content);
            canvas.notifyClipboardStateChanged();
            canvas.repaint();
        }
    }

    public void deleteSelection() {
        if (!selection.isEmpty()) {
            canvas.saveToUndoStack();
            Graphics2D g2d = canvas.getCanvasGraphics();
            selection.delete(g2d, canvas.getCanvasBackground());
            g2d.dispose();
            selection.clear();
            canvas.notifyClipboardStateChanged();
            canvas.saveToUndoStack();
            canvas.repaint();
        }
    }

    public void rotateSelection(int degrees) {
        if (getSelection().getContent() == null) return;
        getSelection().rotate(degrees);

        canvas.repaint();
    }

    /**
     * Gets a graphics context appropriate for drawing - either for the current selection or canvas.
     * @param canvas The drawing canvas
     * @return Graphics2D context configured with proper transforms and clipping
     */
    public Graphics2D getDrawingGraphics(DrawingCanvas canvas) {
        Selection selection = getSelection();
        if (selection == null || !selection.hasOutline()) {
            // No selection, return canvas graphics
            return (Graphics2D) canvas.getImage().getGraphics();
        }

        // We have a selection - prepare a graphics context for its content
        BufferedImage content = selection.getContent();
        Rectangle bounds = selection.getBounds();

        if (content == null || bounds == null) {
            return (Graphics2D) canvas.getImage().getGraphics();
        }

        Graphics2D g2d = content.createGraphics();

        // Apply clipping based on selection type
        if (selection instanceof PathSelection) {
            // For path selection, create translated path for clipping
            GeneralPath path = ((PathSelection) selection).getPath();
            if (path != null) {
                GeneralPath translatedPath = new GeneralPath(path);
                translatedPath.transform(AffineTransform.getTranslateInstance(-bounds.x, -bounds.y));
                g2d.setClip(translatedPath);
            }
        } else if (selection instanceof RectangleSelection) {
            // For rectangle selection, the entire content is valid
            // No additional clipping needed
        }

        return g2d;
    }

    /**
     * Translates screen coordinates to selection-local coordinates if there's a selection,
     * or to canvas world coordinates otherwise.
     * @param screenPoint Point in screen coordinates
     * @param zoomFactor Current canvas zoom factor
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
     * @param worldPoint Point in world coordinates
     * @return true if the point is within a selection, false otherwise
     */
    public boolean isWithinSelection(Point worldPoint) {
        Selection selection = getSelection();
        return selection != null && selection.hasOutline() && selection.contains(worldPoint);
    }

    public void applySelectionToCanvas() {
        if (selection == null || !selection.hasOutline() || selection.getContent() == null) {
            return;
        }

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        Rectangle bounds = selection.getBounds();
        g2d.drawImage(selection.getContent(), bounds.x, bounds.y, null);

        g2d.dispose();
        canvas.repaint();
    }
}