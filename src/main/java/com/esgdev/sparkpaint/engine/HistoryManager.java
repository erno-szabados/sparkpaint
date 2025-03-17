package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class HistoryManager {
    private final Deque<CompressedImage> undoStack = new ArrayDeque<>();
    private final Deque<CompressedImage> redoStack = new ArrayDeque<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners;
    private static final int MAX_HISTORY_SIZE = 16;
    private static final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
    private static final Inflater inflater = new Inflater();

    public HistoryManager() {
        this.undoRedoChangeListeners = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deflater.end();
            inflater.end();
        }));
    }

    public void saveToUndoStack(BufferedImage image) {
        if (image != null) {
            BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            //System.out.println("uncompressed image size: " + (copy.getWidth() * copy.getHeight() * 3) + " bytes");
            byte[] compressed = compressImage(image);
            //System.out.printf("Compressed image size: %d bytes\n", compressed.length);
            CompressedImage compressedImage = new CompressedImage(compressed, image.getWidth(), image.getHeight());
            redoStack.clear();
            undoStack.push(compressedImage);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast(); // Keep the stack size within the limit
            }
            notifyUndoRedoStateChanged();
        }
    }

    public BufferedImage undo(BufferedImage currentImage) {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack
            byte[] compressed = compressImage(currentImage);
            CompressedImage currentState = new CompressedImage(compressed, currentImage.getWidth(), currentImage.getHeight());
            redoStack.push(currentState);

            // Pop previous state from undo stack and decompress it
            CompressedImage previousState = undoStack.pop();
            notifyUndoRedoStateChanged();
            return decompressImage(previousState.getCompressedData(), previousState.getWidth(), previousState.getHeight());

        }
        return currentImage;
    }

    public BufferedImage redo(BufferedImage currentImage) {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack
            byte[] compressed = compressImage(currentImage);
            CompressedImage currentState = new CompressedImage(compressed, currentImage.getWidth(), currentImage.getHeight());
            undoStack.push(currentState);

            // Pop next state from redo stack and decompress it
            CompressedImage nextState = redoStack.pop();
            notifyUndoRedoStateChanged();
            return decompressImage(nextState.getCompressedData(), nextState.getWidth(), nextState.getHeight());
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

    public static byte[] compressImage(BufferedImage image) {
        byte[] rawData;

        if (image.getRaster().getDataBuffer() instanceof DataBufferInt) {
            // Access the int[] directly
            int[] intData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

            // Create a ByteBuffer view of the int[] without copying
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(intData.length * 4);
            byteBuffer.asIntBuffer().put(intData); // Avoids copying the data
            rawData = new byte[intData.length * 4];
            byteBuffer.get(rawData);
        } else {
            throw new IllegalArgumentException("Unsupported DataBuffer type");
        }
        deflater.reset();
        deflater.setInput(rawData);
        deflater.finish();

        // Perform compression
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int len = deflater.deflate(buffer);
            compressedStream.write(buffer, 0, len);
        }

        // Return compressed data as a byte array
        return compressedStream.toByteArray();
    }

    public static BufferedImage decompressImage(byte[] compressedData, int width, int height) {
        inflater.reset();
        inflater.setInput(compressedData);

        // Allocate buffer for raw data (no copying)
        byte[] rawData = new byte[width * height * 4]; // 4 bytes per pixel (ARGB)
        try {
            inflater.inflate(rawData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decompress image data", e);
        }

        // Directly wrap the decompressed data into a ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawData);

        // Create an int[] view of the decompressed data
        int[] intData = new int[width * height];
        byteBuffer.asIntBuffer().get(intData);

        // Create a BufferedImage and set its raster to use the decompressed int[] data
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] imageRaster = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // Avoid a deep copy by copying the reference
        System.arraycopy(intData, 0, imageRaster, 0, intData.length);

        return image;
    }
}