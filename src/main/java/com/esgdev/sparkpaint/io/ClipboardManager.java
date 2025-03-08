package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

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

    public ClipboardManager(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    public void cutSelection() {
        if (canvas.getSelectionRectangle() == null
                || canvas.getSelectionRectangle().width <= 0
                || canvas.getSelectionRectangle().height <= 0) {
            return;
        }
        copySelection(false);
        eraseSelection();
        canvas.setSelectionRectangle(null); // Clear selection after cutting.
        canvas.repaint();
        notifyClipboardStateChanged();
    }

    public void copySelection() {
        copySelection(true);
    }

    private void copySelection(boolean clearRectangle) {
        if (canvas.getSelectionRectangle() == null
                || canvas.getSelectionRectangle().width <= 0
                || canvas.getSelectionRectangle().height <= 0) {
            return;
        }
        // Extract the selected region from the canvas image.
        BufferedImage canvasImage = (BufferedImage) canvas.getImage();
        BufferedImage selectionImage = canvasImage.getSubimage(
                canvas.getSelectionRectangle().x,
                canvas.getSelectionRectangle().y,
                canvas.getSelectionRectangle().width,
                canvas.getSelectionRectangle().height);
        ImageSelection.copyImage(selectionImage);
        if (clearRectangle) {
            canvas.setSelectionRectangle(null);
            canvas.repaint();
        }
        notifyClipboardStateChanged();
    }

    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        BufferedImage pastedImage = ImageSelection.pasteImage();
        if (pastedImage != null) {
            if (canvas.getImage() == null || canvas.getCanvasGraphics() == null) {
                canvas.createNewCanvas(DrawingCanvas.DEFAULT_CANVAS_WIDTH, DrawingCanvas.DEFAULT_CANVAS_HEIGHT, canvas.getCanvasBackground());
                canvas.saveToUndoStack();
            }
            Point mousePosition = canvas.getMousePosition();
            int pasteX = 0;
            int pasteY = 0;

            if (mousePosition != null && canvas.contains(mousePosition)) {
                pasteX = mousePosition.x;
                pasteY = mousePosition.y;
            }
            canvas.getCanvasGraphics().drawImage(pastedImage, pasteX, pasteY, null);

            canvas.setSelectionRectangle(new Rectangle(pasteX, pasteY, pastedImage.getWidth(), pastedImage.getHeight()));
            canvas.setSelectionContent(pastedImage);

            canvas.repaint();
            notifyClipboardStateChanged();
        }
    }

    public boolean hasSelection() {
        return canvas.getSelectionRectangle() != null;
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
        // Clear the selected region by filling it with the canvas background color.
        Graphics2D graphics = canvas.getCanvasGraphics();
        graphics.setColor(canvas.getCanvasBackground());
        graphics.fillRect(canvas.getSelectionRectangle().x, canvas.getSelectionRectangle().y, canvas.getSelectionRectangle().width,
                canvas.getSelectionRectangle().height);
        graphics.dispose();
    }
}