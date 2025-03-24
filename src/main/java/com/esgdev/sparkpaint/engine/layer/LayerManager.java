package com.esgdev.sparkpaint.engine.layer;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LayerManager implements LayerManagement {
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = 0;
    private final DrawingCanvas canvas;
    private BufferedImage transparencyBackground;
    private static final int CHECKERBOARD_SIZE = 8; // Size of each checkerboard square
    private static final Color CHECKERBOARD_COLOR1 = new Color(200, 200, 200); // Light gray
    private static final Color CHECKERBOARD_COLOR2 = new Color(255, 255, 255); // White
    private boolean transparencyVisualizationEnabled = true;
    private final List<LayerChangeListener> layerChangeListeners = new ArrayList<>();


    public LayerManager(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    // Initialize with at least one layer
    public void initializeLayers(int width, int height) {
        layers.clear();
        layers.add(new Layer(width, height));
        currentLayerIndex = 0;
        createTransparencyBackground(width, height);
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    public BufferedImage getTransparencyBackground() {
        return transparencyBackground;
    }

    public boolean isTransparencyVisualizationEnabled() {
        return transparencyVisualizationEnabled;
    }

    /**
     * Sets whether transparency visualization is enabled or not.
     *
     * @param enabled true to enable transparency visualization, false to disable it
     */
    public void setTransparencyVisualizationEnabled(boolean enabled) {
        this.transparencyVisualizationEnabled = enabled;
        canvas.repaint();
    }

    /**
     * Adds a new layer to the layer manager.
     */
    public void addNewLayer() {
        layers.add(new Layer(canvas.getWidth(), canvas.getHeight()));
        currentLayerIndex = layers.size() - 1;
        canvas.repaint();
    }

    /**
     * Duplicates the current layer and adds it above the current layer.
     * The duplicate layer becomes the current layer.
     *
     * @return true if the operation was successful
     */
    public boolean duplicateCurrentLayer() {
        if (currentLayerIndex < 0 || currentLayerIndex >= layers.size()) {
            return false;
        }

        // Get the current layer
        Layer currentLayer = layers.get(currentLayerIndex);

        // Create a new layer with the same dimensions
        Layer duplicatedLayer = new Layer(currentLayer.getImage().getWidth(),
                currentLayer.getImage().getHeight());

        // Copy the image data from the current layer
        Graphics2D g2d = duplicatedLayer.getImage().createGraphics();
        g2d.drawImage(currentLayer.getImage(), 0, 0, null);
        g2d.dispose();

        // Set layer name
        duplicatedLayer.setName(currentLayer.getName() + " (Copy)");

        // Add the duplicated layer above the current layer
        layers.add(currentLayerIndex + 1, duplicatedLayer);

        // Set the duplicate as the current layer
        currentLayerIndex++;

        canvas.repaint();
        return true;
    }

    /**
     * Deletes the current layer and sets the next layer as current.
     */
    public void deleteCurrentLayer() {
        if (layers.size() <= 1) return;

        layers.remove(currentLayerIndex);
        if (currentLayerIndex >= layers.size()) {
            currentLayerIndex = layers.size() - 1;
        }
        canvas.repaint();
    }

    /**
     * Deletes a layer at the specified index.
     *
     * @param index the index of the layer to delete
     */
    public void deleteLayer(int index) {
        if (index < 0 || index >= layers.size() || layers.size() == 1) {
            return;
        }

        layers.remove(index);

        // Adjust current layer index if needed
        if (currentLayerIndex >= layers.size()) {
            currentLayerIndex = layers.size() - 1;
        } else if (currentLayerIndex > index) {
            // If we deleted a layer above the current one, adjust the index
            currentLayerIndex--;
        }

        canvas.repaint();
    }

    /**
     * Moves a layer from one index to another.
     *
     * @param fromIndex the index of the layer to move
     * @param toIndex   the index to move the layer to
     * @return true if the operation was successful, false otherwise
     */
    public boolean moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size() ||
                toIndex < 0 || toIndex >= layers.size() ||
                fromIndex == toIndex) {
            return false;
        }

        Layer layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);

        // Update current layer index if necessary
        if (currentLayerIndex == fromIndex) {
            currentLayerIndex = toIndex;
        } else if (fromIndex < currentLayerIndex && toIndex >= currentLayerIndex) {
            currentLayerIndex--;
        } else if (fromIndex > currentLayerIndex && toIndex <= currentLayerIndex) {
            currentLayerIndex++;
        }

        canvas.repaint();
        return true;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * Sets the layers and updates the current layer index if necessary.
     *
     * @param layers the new list of layers
     */
    public void setLayers(List<Layer> layers) {
        this.layers = layers;
        if (currentLayerIndex >= layers.size()) {
            currentLayerIndex = layers.size() - 1;
        }
        if (!layers.isEmpty()) {
            BufferedImage firstLayer = layers.get(0).getImage();
            createTransparencyBackground(firstLayer.getWidth(), firstLayer.getHeight());
        }
        notifyLayersChanged();
        canvas.repaint();
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }

    public void setCurrentLayerIndex(int index) {
        if (index >= 0 && index < layers.size()) {
            currentLayerIndex = index;
        }
    }

    public int getLayerCount() {
        return layers.size();
    }

    public BufferedImage getCurrentLayerImage() {
        return layers.get(currentLayerIndex).getImage();
    }

    /**
     * Merges the current layer down into the layer below it.
     *
     * @return true if the operation was successful, false otherwise
     */
    public boolean mergeCurrentLayerDown() {
        // Check if there is a layer below the current one
        if (currentLayerIndex <= 0) {
            return false;  // Cannot merge if current layer is the bottom layer
        }

        // Get the current layer and the layer below
        Layer currentLayer = layers.get(currentLayerIndex);
        Layer layerBelow = layers.get(currentLayerIndex - 1);

        // Create a graphics context for the layer below
        Graphics2D g2d = layerBelow.getImage().createGraphics();

        // Draw the current layer onto the layer below
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2d.drawImage(currentLayer.getImage(), 0, 0, null);
        g2d.dispose();

        layers.remove(currentLayerIndex);

        // Set the current layer index to the merged layer
        currentLayerIndex--;
        canvas.repaint();

        return true;
    }

    /**
     * Flattens all visible layers into a single layer.
     *
     * @return true if the operation was successful, false otherwise
     */
    public boolean flattenLayers() {
        // Check if there's only one layer already
        if (layers.size() <= 1) {
            return false;  // Nothing to flatten
        }

        // Create a new layer to hold the flattened image
        Layer flattenedLayer = new Layer(getCurrentLayerImage().getWidth(), getCurrentLayerImage().getHeight());
        flattenedLayer.setName("Flattened Layer");
        Graphics2D g2d = flattenedLayer.getImage().createGraphics();

        // Draw all visible layers from bottom to top
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }
        g2d.dispose();

        layers.clear();
        layers.add(flattenedLayer);
        currentLayerIndex = 0;

        canvas.repaint();

        return true;
    }

    private void createTransparencyBackground(int width, int height) {
        transparencyBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = transparencyBackground.createGraphics();

        for (int x = 0; x < width; x += CHECKERBOARD_SIZE) {
            for (int y = 0; y < height; y += CHECKERBOARD_SIZE) {
                if ((x / CHECKERBOARD_SIZE + y / CHECKERBOARD_SIZE) % 2 == 0) {
                    g2d.setColor(CHECKERBOARD_COLOR1);
                } else {
                    g2d.setColor(CHECKERBOARD_COLOR2);
                }
                g2d.fillRect(x, y, CHECKERBOARD_SIZE, CHECKERBOARD_SIZE);
            }
        }
        g2d.dispose();
    }

    @Override
    public void addLayerChangeListener(LayerChangeListener listener) {
        if (listener != null && !layerChangeListeners.contains(listener)) {
            layerChangeListeners.add(listener);
        }
    }

    @Override
    public void removeLayerChangeListener(LayerChangeListener listener) {
        layerChangeListeners.remove(listener);
    }

    @Override
    public void notifyLayersChanged() {
        for (LayerChangeListener listener : layerChangeListeners) {
            listener.onLayersChanged();
        }
    }
}
