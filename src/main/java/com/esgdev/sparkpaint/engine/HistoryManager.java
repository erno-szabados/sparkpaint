package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HistoryManager {
    private final Deque<DeltaImage> undoStack = new ArrayDeque<>();
    private final Deque<DeltaImage> redoStack = new ArrayDeque<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners;
    private static final int MAX_HISTORY_SIZE = 16;

    public HistoryManager() {
        this.undoRedoChangeListeners = new ArrayList<>();
    }

    public void saveToUndoStack(BufferedImage currentImage) {
        BufferedImage previousImage = getLastImage();
        DeltaImage deltaImage = createDeltaImage(previousImage, currentImage);
        if (deltaImage != null) {
            undoStack.push(deltaImage);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast();
            }
            notifyUndoRedoStateChanged();
        }
    }

    public BufferedImage undo(BufferedImage currentImage) {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack before popping from undo stack
            redoStack.push(createDeltaImage(null, currentImage));

            // Apply delta image from undo stack to current image
            DeltaImage deltaImage = undoStack.pop();
            applyDeltaImage(currentImage, deltaImage);
            notifyUndoRedoStateChanged();
        }
        return currentImage;
    }

    public BufferedImage redo(BufferedImage currentImage) {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack before popping from redo stack
            undoStack.push(createDeltaImage(null, currentImage));

            // Apply delta image from redo stack to current image
            DeltaImage deltaImage = redoStack.pop();
            applyDeltaImage(currentImage, deltaImage);
            notifyUndoRedoStateChanged();
        }
        return currentImage;
    }


    public static class DeltaImage {
        BufferedImage image;
        Rectangle boundingBox;

        DeltaImage(BufferedImage image, Rectangle boundingBox) {
            this.image = copyImage(image);
            this.boundingBox = boundingBox;
        }

        private static BufferedImage copyImage(BufferedImage source) {
            if (source == null) return null;
            BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(source, 0, 0, null);
            g2d.dispose();
            return copy;
        }
    }

    public BufferedImage getLastImage() {
        if (!undoStack.isEmpty()) {
            DeltaImage deltaImage = undoStack.peek();
            return deltaImage.image;
        }
        return null;
    }

    private DeltaImage createDeltaImage(BufferedImage previous, BufferedImage current) {
        if (previous == null) {
            return new DeltaImage(current, new Rectangle(0, 0, current.getWidth(), current.getHeight()));
        }

        int width = current.getWidth();
        int height = current.getHeight();
        int minX = width, minY = height, maxX = 0, maxY = 0;
        boolean hasDifference = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (current.getRGB(x, y) != previous.getRGB(x, y)) {
                    hasDifference = true;
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (!hasDifference) {
            return null; // No difference
        }

        Rectangle boundingBox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        BufferedImage deltaImage = current.getSubimage(minX, minY, boundingBox.width, boundingBox.height);
        System.out.println("Delta Image: " + boundingBox);

        return new DeltaImage(deltaImage, boundingBox);
    }

    private void applyDeltaImage(BufferedImage baseImage, DeltaImage deltaImage) {
        if (deltaImage == null) return;

        Graphics2D g2d = baseImage.createGraphics();
        // Make sure to only apply the delta to the changed area
        g2d.drawImage(deltaImage.image, deltaImage.boundingBox.x, deltaImage.boundingBox.y,
                deltaImage.boundingBox.width, deltaImage.boundingBox.height, null);
        g2d.dispose();
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