package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class FreeHandSelectionTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point worldStartPoint;
    private boolean isDragging = false;
    private Point originalSelectionLocation = null;
    private final SelectionManager selectionManager;
    private boolean transparencyEnabled = false;
    private final GeneralPath currentPath = new GeneralPath();
    private boolean isDrawingPath = false;
    private Rectangle selectionBounds = null;

    public FreeHandSelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selectionManager = canvas.getSelectionManager();
    }

    public void setTransparencyEnabled(boolean enabled) {
        this.transparencyEnabled = enabled;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
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
            Selection selection = selectionManager.getSelection();
            if (!(selection instanceof PathSelection)) {
                return;
            }
            // Right-click to clear selection
            selectionManager.clearSelection();
            canvas.undo();
            isDragging = false;
            isDrawingPath = false;
            originalSelectionLocation = null;
            return;
        }

        Point screenStartPoint = e.getPoint();
        worldStartPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), screenStartPoint);
        Selection selection = selectionManager.getSelection();

        // Apply the old selection to the canvas regardless of its type
        if (selection != null && selection.hasOutline()) {
            // If we're clicking outside the selection, apply it to canvas
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (!selection.contains(worldPoint)) {
                selectionManager.applySelectionToCanvas();
                selectionManager.clearSelection();
                canvas.repaint();
                return; // Exit early after applying and clearing
            }
        }

        if (!(selection instanceof PathSelection) || !selection.hasOutline()) {
            // Start a new path selection
            currentPath.reset();
            currentPath.moveTo(worldStartPoint.x, worldStartPoint.y);
            isDrawingPath = true;
            selection = new PathSelection(currentPath, null);
            selectionManager.setSelection(selection);
        } else if (selection.contains(worldStartPoint)) {
            // Start dragging the existing selection
            isDragging = true;
            isDrawingPath = false;
            Rectangle bounds = selection.getBounds();
            if (originalSelectionLocation == null) {
                originalSelectionLocation = new Point(bounds.x, bounds.y);
            }
        } else {
            // Convert screen point to world coordinates
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

            // If there's a selection and we click outside it, apply selection to canvas first
            if (selection.hasOutline() && !selection.contains(worldPoint)) {
                selectionManager.applySelectionToCanvas();
                selectionManager.clearSelection();
                canvas.repaint();
                return; // Don't proceed with drawing outside selection
            } else {
                // Start a new path
                currentPath.reset();
                currentPath.moveTo(worldStartPoint.x, worldStartPoint.y);
                isDrawingPath = true;
                selection = new PathSelection(currentPath, null);
                selectionManager.setSelection(selection);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
            return;
        }

        if (isDrawingPath) {
            isDrawingPath = false;
            // Close the path
            currentPath.closePath();

            // Update the selection bounds
            selectionBounds = currentPath.getBounds();

            if (selectionBounds.width > 0 && selectionBounds.height > 0) {
                // Create a new image for the selection content
                BufferedImage selectionContent = new BufferedImage(
                        selectionBounds.width, selectionBounds.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = selectionContent.createGraphics();

                // Set the clip to the path (translated to image coordinates)
                GeneralPath translatedPath = new GeneralPath(currentPath);
                translatedPath.transform(AffineTransform.getTranslateInstance(-selectionBounds.x, -selectionBounds.y));
                g2d.setClip(translatedPath);

                // Draw the canvas content
                g2d.drawImage(canvas.getImage(), -selectionBounds.x, -selectionBounds.y, null);
                g2d.dispose();

                // Apply transparency if enabled
                if (transparencyEnabled) {
                    applyTransparencyToContent(selectionContent, canvas.getFillColor());
                }
                selection.setTransparent(transparencyEnabled);

                // Set the selection content
                selection.setContent(selectionContent);
                originalSelectionLocation = new Point(selectionBounds.x, selectionBounds.y);
                clearSelectionOriginalLocation((transparencyEnabled ? canvas.getFillColor() : canvas.getCanvasBackground()));
                // Notify that clipboard state has changed
                canvas.notifyClipboardStateChanged();
            } else {
                selectionManager.getSelection().setContent(null);
                originalSelectionLocation = null;
            }
        } else if (isDragging) {
            isDragging = false;
            Point worldEndPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (selection.contains(worldEndPoint)) {
                canvas.setCursor(handCursor);
            } else {
                canvas.setCursor(crosshairCursor);
            }
        }

        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point screenDragPoint = e.getPoint();
        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), screenDragPoint);

        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
            return;
        }

        if (isDrawingPath) {
            // Add point to the path
            currentPath.lineTo(worldDragPoint.x, worldDragPoint.y);
        } else if (isDragging) {
            // Calculate the new position
            int dx = worldDragPoint.x - worldStartPoint.x;
            int dy = worldDragPoint.y - worldStartPoint.y;

            // Translate the path
            GeneralPath path = ((PathSelection) selection).getPath();
            if (path != null) {
                AffineTransform transform = new AffineTransform();
                transform.translate(dx, dy);
                path.transform(transform);
                worldStartPoint = worldDragPoint;
            }
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
        return "Freehand selection tool selected";
    }

    private void applyTransparencyToContent(BufferedImage content, Color transparentColor) {
        for (int y = 0; y < content.getHeight(); y++) {
            for (int x = 0; x < content.getWidth(); x++) {
                int rgba = content.getRGB(x, y);

                // Check if this pixel has alpha = 0 (fully transparent already)
                // These are pixels outside the path but within the bounding rectangle
                if ((rgba >>> 24) == 0) {
                    // Keep it fully transparent
                    continue;
                }

                Color pixelColor = new Color(rgba, true);

                if (pixelColor.getRGB() == transparentColor.getRGB()) {
                    content.setRGB(x, y, 0x00000000); // Make transparent
                } else {
                    content.setRGB(x, y, rgba | 0xFF000000); // Ensure fully opaque
                }
            }
        }
    }

    private void clearSelectionOriginalLocation(Color color) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
            return;
        }

        if (originalSelectionLocation == null) {
            return;
        }

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        g2d.setColor(color);

        GeneralPath originalPath = ((PathSelection) selection).getPath();
        if (originalPath != null) {
            g2d.fill(originalPath);
        }

        g2d.dispose();
    }

    public void copySelectionToPermanentCanvas() {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
            return;
        }

        GeneralPath path = ((PathSelection) selection).getPath();
        BufferedImage content = selection.getContent();

        if (path == null || content == null) {
            return;
        }

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        Rectangle bounds = path.getBounds();
        g2d.drawImage(content, bounds.x, bounds.y, null);

        g2d.dispose();
        canvas.repaint();
    }
}