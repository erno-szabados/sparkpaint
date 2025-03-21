package com.esgdev.sparkpaint.engine;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LayerManager {
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = 0;
    private final DrawingCanvas canvas;

    public LayerManager(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    // Initialize with at least one layer
    public void initializeLayers(int width, int height) {
        layers.clear();
        layers.add(new Layer(width, height));
        currentLayerIndex = 0;
    }

    public void addNewLayer() {
        layers.add(new Layer(canvas.getWidth(), canvas.getHeight()));
        currentLayerIndex = layers.size() - 1;
        canvas.repaint();
    }

    public void deleteCurrentLayer() {
        if (layers.size() <= 1) return;

        layers.remove(currentLayerIndex);
        if (currentLayerIndex >= layers.size()) {
            currentLayerIndex = layers.size() - 1;
        }
        canvas.repaint();
    }

    public boolean moveCurrentLayerUp() {
        if (currentLayerIndex >= layers.size() - 1) return false;

        Layer layer = layers.get(currentLayerIndex);
        layers.remove(currentLayerIndex);
        layers.add(currentLayerIndex + 1, layer);
        currentLayerIndex++;
        canvas.repaint();
        return true;
    }

    public boolean moveCurrentLayerDown() {
        if (currentLayerIndex <= 0) return false;

        Layer layer = layers.get(currentLayerIndex);
        layers.remove(currentLayerIndex);
        layers.add(currentLayerIndex - 1, layer);
        currentLayerIndex--;
        canvas.repaint();
        return true;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
        if (currentLayerIndex >= layers.size()) {
            currentLayerIndex = layers.size() - 1;
        }
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
}
