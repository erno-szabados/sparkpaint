
package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;

/// Manages the selection state and operations on the drawing canvas.
public class SelectionManager {
    private final DrawingCanvas canvas;
    private Selection selection;

    public SelectionManager(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selection = new Selection(null, null);
    }

    public Selection getSelection() {
        return selection;
    }

    public void clearSelection() {
        selection.clear();
        canvas.repaint();
    }

    public void selectAll() {
        canvas.setCurrentTool(DrawingCanvas.Tool.SELECTION);
        BufferedImage image = (BufferedImage) canvas.getImage();
        if (image != null) {
            Rectangle rect = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            BufferedImage content = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = content.createGraphics();
            g2d.drawImage(image, -rect.x, -rect.y, null);
            g2d.dispose();
            selection = new Selection(rect, content);
            canvas.notifyClipboardStateChanged();
            canvas.repaint();
        }
    }

    public void deleteSelection() {
        if (!selection.isEmpty()) {
            canvas.saveToUndoStack();
            Graphics2D g2d = canvas.getCanvasGraphics();
            g2d.setColor(canvas.getCanvasBackground());
            g2d.fillRect(selection.getRectangle().x, selection.getRectangle().y, selection.getRectangle().width, selection.getRectangle().height);
            g2d.dispose();
            selection.clear();
            canvas.notifyClipboardStateChanged();
            canvas.saveToUndoStack();
            canvas.repaint();
        }
    }
}