package com.esgdev.sparkpaint.engine;

import java.awt.image.BufferedImage;

public class Layer {
    private BufferedImage image;
    private boolean visible;
    private String name;
    private static int layerCounter = 1;

    public Layer(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.visible = true;
        this.name = "Layer " + layerCounter++;
    }

    public Layer(BufferedImage image) {
        this.image = image;
        this.visible = true;
        this.name = "Layer " + layerCounter++;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Reset counter when loading a new project
    public static void resetCounter() {
        layerCounter = 1;
    }
}