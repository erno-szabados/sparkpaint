package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public abstract class AbstractSelectionTool implements DrawingTool {
    protected final DrawingCanvas canvas;
    protected final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    protected final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    protected Point worldStartPoint;
    protected boolean isDragging = false;
    protected Point originalSelectionLocation = null;
    protected final SelectionManager selectionManager;
    protected boolean transparencyEnabled = false;

    protected AbstractSelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selectionManager = canvas.getSelectionManager();
    }

    public void setTransparencyEnabled(boolean enabled) {
        this.transparencyEnabled = enabled;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Selection selection = selectionManager.getSelection();
        if (selection == null) {
            return;
        }
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (selection.contains(worldPoint)) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick();
            return;
        }

        worldStartPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Selection selection = selectionManager.getSelection();

        // Apply existing selection if clicking outside
        if (selection != null && selection.hasOutline()) {
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (!selection.contains(worldPoint)) {
                // Apply the selection to the current layer instead of clearing it
                copySelectionToPermanentCanvas();
                selectionManager.clearSelection();
                canvas.repaint();
                return;
            }
        }

        handleSelectionStart(e);
    }

    protected void handleRightClick() {
        Selection selection = selectionManager.getSelection();
        if (selection == null) {
            return;
        }
        selectionManager.clearSelection();
        canvas.undo();
        isDragging = false;
        originalSelectionLocation = null;
    }

    @Override
    public void setCursor() {
        canvas.setCursor(crosshairCursor);
    }

    protected void applyTransparencyToContent(BufferedImage content, Color transparentColor) {
        for (int y = 0; y < content.getHeight(); y++) {
            for (int x = 0; x < content.getWidth(); x++) {
                int rgba = content.getRGB(x, y);

                // Skip already transparent pixels
                if ((rgba >>> 24) == 0) continue;

                Color pixelColor = new Color(rgba, true);
                if (pixelColor.getRGB() == transparentColor.getRGB()) {
                    content.setRGB(x, y, 0x00000000);
                } else {
                    content.setRGB(x, y, rgba | 0xFF000000);
                }
            }
        }
    }

    public void copySelectionToPermanentCanvas() {
        Selection selection = selectionManager.getSelection();
        if (selection == null) {
            return;
        }

        BufferedImage content = selection.getContent();
        if (content == null) return;

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getLayerManager().getCurrentLayerImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        drawSelectionToCanvas(g2d, selection, content);
        g2d.dispose();
        canvas.repaint();
    }

    // In AbstractSelectionTool.java
    protected void clearOriginalSelectionAreaWithTransparency() {
        Selection selection = selectionManager.getSelection();
        if (selection == null || originalSelectionLocation == null) {
            return;
        }

        // Save to undo stack before modifying
        canvas.saveToUndoStack();

        // Get the current layer image
        BufferedImage currentLayer = canvas.getLayerManager().getCurrentLayerImage();
        Graphics2D g2d = currentLayer.createGraphics();

        // Use Clear composite for transparency
        g2d.setComposite(AlphaComposite.Clear);

        // Clear the area based on selection shape
        if (selection.getPath() != null) {
            g2d.fill(selection.getPath());
        }

        g2d.dispose();
    }

    /**
     * Creates a transparent selection image preserving original transparency
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

    // Abstract methods for specialized tool behavior
    protected abstract void handleSelectionStart(MouseEvent e);

    protected abstract void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content);
}