package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.SelectionManager;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClipboardManager {
    private final DrawingCanvas canvas;
    private final List<ClipboardChangeListener> clipboardChangeListeners = new ArrayList<>();
    private final SelectionManager selectionManager;

    public ClipboardManager(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selectionManager = canvas.getSelectionManager();
    }

    public void cutSelection() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        copySelection();
        eraseSelection();
        selectionManager.getSelection().setRectangle(null);
        canvas.repaint();
        notifyClipboardStateChanged();
    }

    public void copySelection() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        BufferedImage selectionImage = selectionManager.getSelection().getContent();
        ImageSelection.copyImage(selectionImage);
        notifyClipboardStateChanged();
    }

    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        BufferedImage pastedImage = ImageSelection.pasteImage();
        if (pastedImage != null) {
            if (canvas.getImage() == null || canvas.getCanvasGraphics() == null) {
                canvas.createNewCanvas(DrawingCanvas.DEFAULT_CANVAS_WIDTH, DrawingCanvas.DEFAULT_CANVAS_HEIGHT, canvas.getCanvasBackground());
            }
            canvas.saveToUndoStack();
            Point mousePosition = canvas.getMousePosition();
            int pasteX = 0;
            int pasteY = 0;


            if (mousePosition != null ) {
                Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), mousePosition);
                if (canvas.contains(worldPoint)) {
                    pasteX = worldPoint.x;
                    pasteY = worldPoint.y;
                }
            }

            selectionManager.getSelection().setRectangle(new Rectangle(pasteX, pasteY, pastedImage.getWidth(), pastedImage.getHeight()));
            selectionManager.getSelection().setContent(pastedImage);

            canvas.repaint();
            notifyClipboardStateChanged();
        }
    }

    public boolean hasSelection() {
        return selectionManager.getSelection().getRectangle() != null;
    }

    public boolean canPaste() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] flavors = clipboard.getAvailableDataFlavors();

            for (DataFlavor flavor : flavors) {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    public void addClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardChangeListeners.add(listener);
    }

    public void removeClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardChangeListeners.remove(listener);
    }

    public void notifyClipboardStateChanged() {
        boolean canCopy = hasSelection();
        boolean canPaste = canPaste();

        for (ClipboardChangeListener listener : clipboardChangeListeners) {
            listener.clipboardStateChanged(canCopy, canPaste);
        }
    }

    private void eraseSelection() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        // Clear the selected region by filling it with the canvas background color.
        Graphics2D graphics = canvas.getCanvasGraphics();
        graphics.setColor(canvas.getCanvasBackground());
        graphics.fillRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width,
                selectionRectangle.height);
        graphics.dispose();
    }
}