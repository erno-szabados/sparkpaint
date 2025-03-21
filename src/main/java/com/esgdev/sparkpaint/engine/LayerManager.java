package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LayerManager {
    private List<Layer> layers = new ArrayList<>();
    private int currentLayerIndex = 0;
    private final DrawingCanvas canvas;
    private BufferedImage transparencyBackground;
    private static final int CHECKERBOARD_SIZE = 8; // Size of each checkerboard square
    private static final Color CHECKERBOARD_COLOR1 = new Color(204, 204, 204); // Light gray
    private static final Color CHECKERBOARD_COLOR2 = new Color(255, 255, 255); // White
    private boolean transparencyVisualizationEnabled = true;


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

    public BufferedImage getTransparencyBackground() {
        return transparencyBackground;
    }

    public boolean isTransparencyVisualizationEnabled() {
        return transparencyVisualizationEnabled;
    }

    public void setTransparencyVisualizationEnabled(boolean enabled) {
        this.transparencyVisualizationEnabled = enabled;
        canvas.repaint();
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

    public void deleteLayer(int index) {
        if (index < 0 || index >= layers.size() || layers.size() <= 1) {
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
        // Notify change
        //notifyLayersChanged();
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
        if (!layers.isEmpty()) {
            BufferedImage firstLayer = layers.get(0).getImage();
            createTransparencyBackground(firstLayer.getWidth(), firstLayer.getHeight());
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
