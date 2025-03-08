package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class UndoRedoManager {
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners;
    private static final int MAX_HISTORY_SIZE = 16;

    public UndoRedoManager() {
        this.undoRedoChangeListeners = new ArrayList<>();
    }

    public void saveToUndoStack(BufferedImage image) {
        if (image != null) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            undoStack.push(copy);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast(); // Keep the stack size within the limit
            }
            notifyUndoRedoStateChanged();
        }
    }

    public BufferedImage undo(BufferedImage currentImage) {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack
            BufferedImage currentState = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = currentState.createGraphics();
            g2d.drawImage(currentImage, 0, 0, null);
            g2d.dispose();
            redoStack.push(currentState);

            // Pop previous state from undo stack and return it
            BufferedImage previousState = undoStack.pop();
            notifyUndoRedoStateChanged();
            return previousState;
        }
        return currentImage;
    }

    public BufferedImage redo(BufferedImage currentImage) {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack
            BufferedImage currentState = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = currentState.createGraphics();
            g2d.drawImage(currentImage, 0, 0, null);
            g2d.dispose();
            undoStack.push(currentState);

            // Pop next state from redo stack and return it
            BufferedImage nextState = redoStack.pop();
            notifyUndoRedoStateChanged();
            return nextState;
        }
        return currentImage;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        notifyUndoRedoStateChanged();
    }

    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        undoRedoChangeListeners.add(listener);
    }

    private void notifyUndoRedoStateChanged() {
        boolean canUndo = canUndo();
        boolean canRedo = canRedo();
        for (UndoRedoChangeListener listener : undoRedoChangeListeners) {
            listener.undoRedoStateChanged(canUndo, canRedo);
        }
    }

}