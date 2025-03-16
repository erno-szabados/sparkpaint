
package com.esgdev.sparkpaint.engine.selection;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
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
            selection.delete(canvas.getCanvasGraphics(), canvas.getCanvasBackground());
            selection.clear();
            canvas.notifyClipboardStateChanged();
            canvas.saveToUndoStack();
            canvas.repaint();
        }
    }

    public void rotateSelection(int degrees) {
        if (getSelection().getContent() == null) return;

        BufferedImage original = getSelection().getContent();
        int width = original.getWidth();
        int height = original.getHeight();

        // Create new rotated image
        BufferedImage rotated = new BufferedImage(
                degrees % 180 == 0 ? width : height,
                degrees % 180 == 0 ? height : width,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = rotated.createGraphics();
        g2d.translate((rotated.getWidth() - width) / 2,
                (rotated.getHeight() - height) / 2);
        g2d.rotate(Math.toRadians(degrees), width / 2.0, height / 2.0);
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        // Save state before rotation
        canvas.saveToUndoStack();

        // Clear original area
        if (!(selection instanceof RectangleSelection)) {
            // TODO: Handle non-rectangular selections
            return;
        }
        Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();

        // Update selection content and rectangle
        selection.setContent(rotated);
        selectionRectangle.setSize(rotated.getWidth(), rotated.getHeight());

        canvas.repaint();
    }
}