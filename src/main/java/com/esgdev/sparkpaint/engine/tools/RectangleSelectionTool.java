package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.RectangleSelection;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/// Handles user interactions for selecting and manipulating an area of the drawing canvas.
public class RectangleSelectionTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point worldStartPoint;
    private boolean isDragging = false;
    private Point worldDragOffset = null;
    private Point originalSelectionLocation = null;
    private final SelectionManager selectionManager;
    private boolean transparencyEnabled = false;

    public RectangleSelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selectionManager = canvas.getSelectionManager();
    }

    public void setTransparencyEnabled(boolean enabled) {
        this.transparencyEnabled = enabled;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            return;
        }
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Rectangle selectionRectangle = ((RectangleSelection) (selection)).getRectangle();
        if (selectionRectangle != null && selectionRectangle.contains(worldPoint)) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            Selection selection = selectionManager.getSelection();
            if (!(selection instanceof RectangleSelection)) {
                return;
            }
            // Right-click to clear selection
            copySelectionToPermanentCanvas();
            selectionManager.clearSelection();
            isDragging = false;
            originalSelectionLocation = null;
            canvas.repaint();
            return;
        }

        Point screenStartPoint = e.getPoint();
        worldStartPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), screenStartPoint);
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            // No selection or different selection type, start new rectangle selection
            selection = new RectangleSelection(new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0), null);
            selectionManager.setSelection(selection);
        }
        Rectangle selectionRectangle = ((RectangleSelection) (selection)).getRectangle();
        if (selectionRectangle == null) {
            ((RectangleSelection) selection).setRectangle(new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0));
            originalSelectionLocation = null;
        } else {
            if (selectionRectangle.contains(worldStartPoint)) {
                // Starting a drag operation
                isDragging = true;
                worldDragOffset = new Point(
                        (int) (worldStartPoint.getX() - selectionRectangle.x),
                        (int) (worldStartPoint.getY() - selectionRectangle.y));
                // Only set originalSelectionLocation if it hasn't been set yet
                if (originalSelectionLocation == null) {
                    originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                }
            } else {
                if (!selectionManager.getSelection().isEmpty()) {
                    // clicked outside of selection, finalize the current selection
                    isDragging = false;
                    copySelectionToPermanentCanvas();
                    selectionManager.clearSelection();
                    originalSelectionLocation = null;
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            return;
        }
        Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
        if (selectionRectangle == null) return;

        Point worldEndPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (isDragging) {
            isDragging = false;  // End drag operation
            if (selectionRectangle.contains(worldEndPoint)) {
                canvas.setCursor(handCursor);
            } else {
                canvas.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            // Handling new selection
            canvas.notifyClipboardStateChanged();
            if (selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                BufferedImage selectionContent = new BufferedImage(selectionRectangle.width, selectionRectangle.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = selectionContent.createGraphics();
                g2d.drawImage(canvas.getImage(), -selectionRectangle.x, -selectionRectangle.y, null);
                g2d.dispose();

                // Apply transparency if enabled
                if (transparencyEnabled) {
                    applyTransparencyToContent(selectionContent, canvas.getFillColor());
                }
                selection.setTransparent(transparencyEnabled);

                selectionManager.getSelection().setContent(selectionContent);
                originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                clearSelectionOriginalLocation((transparencyEnabled ? canvas.getFillColor() : canvas.getCanvasBackground()));
            } else {
                selectionManager.getSelection().setContent(null);
                originalSelectionLocation = null;
            }
        }
        canvas.repaint();
    }

    private void applyTransparencyToContent(BufferedImage content, Color transparentColor) {
        for (int y = 0; y < content.getHeight(); y++) {
            for (int x = 0; x < content.getWidth(); x++) {
                int rgba = content.getRGB(x, y);
                Color pixelColor = new Color(rgba, true);

                if (pixelColor.getRGB() == transparentColor.getRGB()) {
                    content.setRGB(x, y, 0x00000000);
                } else {
                    content.setRGB(x, y, rgba | 0xFF000000);
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point screenDragPoint = e.getPoint();
        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), screenDragPoint);
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            return;
        }
        Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
        if (selectionRectangle == null) return;
        if (isDragging) {
            int newX = (int) (worldDragPoint.getX() - worldDragOffset.x);
            int newY = (int) (worldDragPoint.getY() - worldDragOffset.y);
            selectionRectangle.setLocation(newX, newY);
        } else {
            int x = (int) Math.min(worldStartPoint.x, worldDragPoint.getX());
            int y = (int) Math.min(worldStartPoint.y, worldDragPoint.getY());
            int width = (int) Math.abs(worldDragPoint.getX() - worldStartPoint.x);
            int height = (int) Math.abs(worldDragPoint.getY() - worldStartPoint.y);
            selectionRectangle.setBounds(x, y, width, height);
        }
        canvas.repaint();
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // Handle zooming if needed
    }

    @Override
    public void setCursor() {
        canvas.setCursor(crosshairCursor);
    }

    @Override
    public String statusMessage() {
        return "Selection tool selected";
    }

    private void clearSelectionOriginalLocation(Color color) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            return;
        }
        Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
        if (selectionRectangle == null) return;
        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        Rectangle originalRect = new Rectangle(originalSelectionLocation.x, originalSelectionLocation.y, selectionRectangle.width, selectionRectangle.height);
        g2d.setColor(color);
        g2d.fill(originalRect);
        g2d.dispose();
    }

    public void copySelectionToPermanentCanvas() {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof RectangleSelection)) {
            return;
        }
        Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
        if (selectionRectangle == null) return;
        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2d.drawImage(selectionManager.getSelection().getContent(), selectionRectangle.x, selectionRectangle.y, null);
        g2d.dispose();
        canvas.repaint();
    }
}