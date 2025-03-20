package com.esgdev.sparkpaint.engine;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LayerManager {
    // Add to DrawingCanvas class
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = 0;
    private DrawingCanvas canvas;

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

    // TODO integrate this with the canvas paintComponent method
//    // Update the paint method to composite all visible layers
//    @Override
//    public void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        Graphics2D g2d = (Graphics2D) g;
//        g2d.setColor(Color.WHITE);
//        g2d.fillRect(0, 0, getWidth(), getHeight());
//
//        // Apply zoom transformation
//        g2d.scale(zoomFactor, zoomFactor);
//
//        // Draw all visible layers from bottom to top
//        for (Layer layer : layers) {
//            if (layer.isVisible()) {
//                g2d.drawImage(layer.getImage(), 0, 0, this);
//            }
//        }
//
//        // Draw other UI elements (selection, etc.)
//        // ...
//    }
}
