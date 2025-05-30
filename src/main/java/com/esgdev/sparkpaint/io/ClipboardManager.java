package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
import com.esgdev.sparkpaint.engine.tools.ToolManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClipboardManager implements ClipboardManagement {
    private final DrawingCanvas canvas;
    private final List<ClipboardChangeListener> clipboardChangeListeners = new ArrayList<>();
    //private final SelectionManager selectionManager;

    public ClipboardManager(DrawingCanvas canvas) {
        this.canvas = canvas;
       // this.selectionManager = canvas.getSelectionManager();
    }

    public void cutSelection() {
        Selection selection = canvas.getSelection();
        if (selection == null) return;

        Rectangle selectionRectangle = selection.getBounds();
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        copySelection();
        deleteSelectionAreaFromCurrentLayer();
        canvas.getSelection().clearOutline();
        canvas.repaint();
        notifyClipboardStateChanged();
    }

    public void copySelection() {
        Selection selection = canvas.getSelection();
        if (selection == null) return;

        Rectangle selectionRectangle = selection.getBounds();
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        BufferedImage selectionImage = canvas.getSelection().getContent();
        try {
            ImageSelection.copyImage(selectionImage);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        selection.setActive(true);
        notifyClipboardStateChanged();
    }

    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        BufferedImage pastedImage = ImageSelection.pasteImage();
        if (pastedImage != null) {
            // Ensure we have a canvas with layers
            if (canvas.getLayers().isEmpty()) {
                canvas.createNewCanvas(DrawingCanvas.DEFAULT_CANVAS_WIDTH, DrawingCanvas.DEFAULT_CANVAS_HEIGHT, canvas.getCanvasBackground());
            }
            canvas.saveToUndoStack();

            // Determine paste location
            Point mousePosition = canvas.getMousePosition();
            int pasteX = 0;
            int pasteY = 0;

            if (mousePosition != null) {
                Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), mousePosition);
                if (canvas.contains(worldPoint)) {
                    pasteX = worldPoint.x;
                    pasteY = worldPoint.y;
                }
            }

            // Create selection with pasted content
            canvas.setCurrentTool(ToolManager.Tool.RECTANGLE_SELECTION);
            Rectangle selectionRectangle = new Rectangle(pasteX, pasteY, pastedImage.getWidth(), pastedImage.getHeight());
            GeneralPath path = new GeneralPath(selectionRectangle);

            Selection selection = new Selection(selectionRectangle, pastedImage);
            selection.setActive(true);
            selection.setPath(path);
            canvas.setSelection(selection);

            canvas.repaint();
            notifyClipboardStateChanged();
        }
    }

    public boolean hasSelection() {
        Selection selection = canvas.getSelection();
        return selection != null && selection.hasOutline();
    }

   public boolean canPaste() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] flavors = clipboard.getAvailableDataFlavors();

            for (DataFlavor flavor : flavors) {
                if (ImageSelection.PNG_FLAVOR.equals(flavor) || DataFlavor.imageFlavor.equals(flavor)) {
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

    public void deleteSelectionAreaFromCurrentLayer() {
        canvas.deleteSelectionAreaFromCurrentLayer();
    }

    public void deleteCurrentSelection() {
        canvas.clearSelection();
    }

    // For testing only
    void clipboardStateChanged(boolean canCopy, boolean canPaste) {
        for (ClipboardChangeListener listener : clipboardChangeListeners) {
            listener.clipboardStateChanged(canCopy, canPaste);
        }
    }
}