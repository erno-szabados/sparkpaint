package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;

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

/**
 * HistoryManager is responsible for managing the undo and redo history of layers.
 * It compresses and decompresses layer data to save memory and improve performance.
 */
public class HistoryManager {
    private final Deque<CompressedLayerState> undoStack = new ArrayDeque<>();
    private final Deque<CompressedLayerState> redoStack = new ArrayDeque<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners;
    private static final int MAX_HISTORY_SIZE = 16;
    private static final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
    private static final Inflater inflater = new Inflater();

    /**
     * Constructor for HistoryManager.
     */
    public HistoryManager() {
        this.undoRedoChangeListeners = new ArrayList<>();
        // Add a shutdown hook to clean up the deflater and inflater resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deflater.end();
            inflater.end();
        }));
    }

    /**
     * Save the current state of layers to the undo stack.
     *
     * @param layers            The current layers to be saved.
     * @param currentLayerIndex The index of the current layer.
     */
    public void saveToUndoStack(List<Layer> layers, int currentLayerIndex) {
        if (layers != null && !layers.isEmpty()) {
            CompressedLayerState state = compressLayers(layers, currentLayerIndex);

            redoStack.clear();
            undoStack.push(state);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast(); // Keep the stack size within the limit
            }
            notifyUndoRedoStateChanged();
        }
    }

    /**
     * Undo the last action and return the previous state of layers.
     *
     * @param currentLayers     The current layers to be modified.
     * @param currentLayerIndex The index of the current layer.
     * @return The previous state of layers before the last action.
     */
    public LayerState undo(List<Layer> currentLayers, int currentLayerIndex) {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack
            CompressedLayerState currentState = compressLayers(currentLayers, currentLayerIndex);
            redoStack.push(currentState);

            // Pop previous state from undo stack and decompress it
            CompressedLayerState previousState = undoStack.pop();
            notifyUndoRedoStateChanged();
            return decompressLayers(previousState);
        }
        return new LayerState(new ArrayList<>(currentLayers), currentLayerIndex);
    }

    /**
     * Redo the last undone action and return the next state of layers.
     *
     * @param currentLayers     The current layers to be modified.
     * @param currentLayerIndex The index of the current layer.
     * @return The next state of layers after redoing the last undone action.
     */
    public LayerState redo(List<Layer> currentLayers, int currentLayerIndex) {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack
            CompressedLayerState currentState = compressLayers(currentLayers, currentLayerIndex);
            undoStack.push(currentState);

            // Pop next state from redo stack and decompress it
            CompressedLayerState nextState = redoStack.pop();
            notifyUndoRedoStateChanged();
            return decompressLayers(nextState);
        }
        return new LayerState(new ArrayList<>(currentLayers), currentLayerIndex);
    }

    /**
     * Compress the layers and return their state.
     *
     * @param layers            The layers to be compressed.
     * @param currentLayerIndex The index of the current layer.
     * @return The compressed state of the layers.
     */
    private CompressedLayerState compressLayers(List<Layer> layers, int currentLayerIndex) {
        List<CompressedLayer> compressedLayers = new ArrayList<>();

        for (Layer layer : layers) {
            BufferedImage image = layer.getImage();
            byte[] compressedData = compressImage(image);
            compressedLayers.add(new CompressedLayer(
                    compressedData,
                    image.getWidth(),
                    image.getHeight(),
                    layer.isVisible(),
                    layer.getName()
            ));
        }

        return new CompressedLayerState(compressedLayers, currentLayerIndex);
    }

    /**
     * Decompress the layers and return their state.
     *
     * @param compressedState The compressed state of the layers.
     * @return The decompressed state of the layers.
     */
    private LayerState decompressLayers(CompressedLayerState compressedState) {
        List<Layer> layers = new ArrayList<>();

        for (CompressedLayer compressedLayer : compressedState.getCompressedLayers()) {
            BufferedImage image = decompressImage(
                    compressedLayer.getCompressedData(),
                    compressedLayer.getWidth(),
                    compressedLayer.getHeight()
            );
            Layer layer = new Layer(image);
            layer.setVisible(compressedLayer.isVisible());
            layer.setName(compressedLayer.getName());
            layers.add(layer);
        }

        return new LayerState(layers, compressedState.getCurrentLayerIndex());
    }

    /**
     * Check if there are any actions to undo.
     *
     * @return true if there are actions to undo, false otherwise.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Check if there are any actions to redo.
     *
     * @return true if there are actions to redo, false otherwise.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clear the undo and redo history.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        notifyUndoRedoStateChanged();
    }

    /**
     * Add a listener to be notified of undo/redo state changes.
     *
     * @param listener The listener to add.
     */
    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        undoRedoChangeListeners.add(listener);
    }

    /**
     * Notify all registered listeners about the undo/redo state change.
     */
    private void notifyUndoRedoStateChanged() {
        boolean canUndo = canUndo();
        boolean canRedo = canRedo();
        for (UndoRedoChangeListener listener : undoRedoChangeListeners) {
            listener.undoRedoStateChanged(canUndo, canRedo);
        }
    }

    /**
     * Compresses a BufferedImage into a byte array using Deflater.
     *
     * @param image The BufferedImage to compress.
     * @return The compressed byte array.
     */
    public static byte[] compressImage(BufferedImage image) {
        byte[] rawData;
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new compatible image and copy the original image to it
        BufferedImage compatibleImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = compatibleImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Now we can safely get the data as DataBufferInt
        int[] intData = ((DataBufferInt) compatibleImage.getRaster().getDataBuffer()).getData();

        // Create a ByteBuffer view of the int[] without copying
        ByteBuffer byteBuffer = ByteBuffer.allocate(intData.length * 4);
        byteBuffer.asIntBuffer().put(intData);
        rawData = byteBuffer.array();

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

    /**
     * Decompresses a byte array back into a BufferedImage.
     *
     * @param compressedData The compressed byte array.
     * @param width          The width of the image.
     * @param height         The height of the image.
     * @return The decompressed BufferedImage.
     */
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
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] imageRaster = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // Avoid a deep copy by copying the reference
        System.arraycopy(intData, 0, imageRaster, 0, intData.length);

        return image;
    }
}