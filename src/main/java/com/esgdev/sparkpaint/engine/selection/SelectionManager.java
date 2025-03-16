
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
        getSelection().rotate(degrees);

        canvas.repaint();
    }
}