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
        private final Deque<CompressedLayerState> undoStack = new ArrayDeque<>();
        private final Deque<CompressedLayerState> redoStack = new ArrayDeque<>();
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
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] imageRaster = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

            // Avoid a deep copy by copying the reference
            System.arraycopy(intData, 0, imageRaster, 0, intData.length);

            return image;
        }

        // Helper classes for storing compressed layer state

        public static class CompressedLayer {
            private final byte[] compressedData;
            private final int width;
            private final int height;
            private final boolean visible;
            private final String name;

            public CompressedLayer(byte[] compressedData, int width, int height, boolean visible, String name) {
                this.compressedData = compressedData;
                this.width = width;
                this.height = height;
                this.visible = visible;
                this.name = name;
            }

            public byte[] getCompressedData() { return compressedData; }
            public int getWidth() { return width; }
            public int getHeight() { return height; }
            public boolean isVisible() { return visible; }
            public String getName() { return name; }
        }

        public static class CompressedLayerState {
            private final List<CompressedLayer> compressedLayers;
            private final int currentLayerIndex;

            public CompressedLayerState(List<CompressedLayer> compressedLayers, int currentLayerIndex) {
                this.compressedLayers = compressedLayers;
                this.currentLayerIndex = currentLayerIndex;
            }

            public List<CompressedLayer> getCompressedLayers() { return compressedLayers; }
            public int getCurrentLayerIndex() { return currentLayerIndex; }
        }

        public static class LayerState {
            private final List<Layer> layers;
            private final int currentLayerIndex;

            public LayerState(List<Layer> layers, int currentLayerIndex) {
                this.layers = layers;
                this.currentLayerIndex = currentLayerIndex;
            }

            public List<Layer> getLayers() { return layers; }
            public int getCurrentLayerIndex() { return currentLayerIndex; }
        }
    }