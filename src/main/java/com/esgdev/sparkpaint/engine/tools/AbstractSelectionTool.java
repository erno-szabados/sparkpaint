package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Abstract base class for selection tools in the drawing application.
 * This class provides common functionality for selection tools such as
 * handling mouse events, managing selection state, and drawing selections.
 */
public abstract class AbstractSelectionTool implements DrawingTool {
    protected final DrawingCanvas canvas;
    protected final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    protected final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    protected Point worldStartPoint;
    protected boolean isDragging = false;
    protected Point originalSelectionLocation = null;
    protected boolean transparencyEnabled = false;

    protected AbstractSelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    public void setTransparencyEnabled(boolean enabled) {
        this.transparencyEnabled = enabled;
    }

    /**
     * Handles mouse moved events to update the cursor.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            return;
        }
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (selection.contains(worldPoint) && selection.isActive()) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

    /**
     * Handles mouse pressed events to update the selection.
     * This method is called when the user drags the mouse while holding down a button.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick();
            return;
        }

        worldStartPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Selection selection = canvas.getSelection();

        // Apply existing selection if clicking outside
        if (selection != null && selection.hasOutline() && selection.isActive()) {
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (!selection.contains(worldPoint)) {
                // Apply the selection to the current layer instead of clearing it
                copySelectionToPermanentCanvas();
                canvas.clearSelection();
                canvas.repaint();
                return;
            }
        }

        handleSelectionStart(e);
    }

    @Override
    public void setCursor() {
        canvas.setCursor(crosshairCursor);
    }

    /**
     * Finalizes the selection and clears the original selection area.
     * This method is called when the selection is completed.
     */
    public void copySelectionToPermanentCanvas() {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            return;
        }

        BufferedImage content = selection.getContent();
        if (content == null) return;

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getCurrentLayerImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        drawSelectionToCanvas(g2d, selection, content);
        g2d.dispose();
        canvas.repaint();
    }

    /**
     * Handles right-click events to clear the selection and undo the last action.
     * This method is called when the user right-clicks on the canvas.
     */
    protected void handleRightClick() {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            return;
        }
        canvas.clearSelection();
        canvas.undo();
        isDragging = false;
        originalSelectionLocation = null;
    }

   /**
     * Creates a transparent version of the selection image.
     *
     * @param original The original image to be made transparent.
     * @return A new BufferedImage with transparency.
     */
    protected BufferedImage createTransparentSelectionImage(BufferedImage original) {
        if (original == null) return null;

        // Create a transparent version that preserves original transparency
        BufferedImage transparentContent = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        // Copy the content preserving transparency
        Graphics2D g2d = transparentContent.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return transparentContent;
    }

    protected boolean isSelectionTooSmall(Rectangle bounds) {
        // Consider selections smaller than 3x3 pixels as "too small"
        return bounds == null || bounds.width < 3 || bounds.height < 3;
    }

    // Abstract methods for specialized tool behavior
    protected abstract void handleSelectionStart(MouseEvent e);

    protected abstract void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content);

    protected abstract void finalizeSelection(Selection selection);
}