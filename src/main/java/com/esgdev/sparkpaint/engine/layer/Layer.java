package com.esgdev.sparkpaint.engine.layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;

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

    // Layer adjustment methods

    /**
     * Adjusts the brightness of the layer's image
     * @param value Value between -100 and 100
     */
    public void adjustBrightness(int value) {
        if (value == 0) return;

        // Convert from -100,100 to usable scaling factor
        float factor = 1.0f + (value / 100.0f);

        BufferedImage original = getImage();
        BufferedImage adjusted = new BufferedImage(
                original.getWidth(), original.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        // Apply the brightness adjustment
        float[] scales = {factor, factor, factor, 1.0f}; // Leave alpha untouched
        float[] offsets = {0, 0, 0, 0};
        BufferedImageOp op = new RescaleOp(scales, offsets, null);
        op.filter(original, adjusted);

        setImage(adjusted);
    }

    /**
     * Adjusts the contrast of the layer's image
     * @param value Value between -100 and 100
     */
    public void adjustContrast(int value) {
        if (value == 0) return;

        // Convert from -100,100 to usable scaling factor
        float factor = (value > 0) ?
                1.0f + (value / 50.0f) :
                (100.0f + value) / 100.0f;

        BufferedImage original = getImage();
        BufferedImage adjusted = new BufferedImage(
                original.getWidth(), original.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        // Apply contrast adjustment
        float[] scales = {factor, factor, factor, 1.0f}; // Leave alpha untouched
        float[] offsets = {128 * (1 - factor), 128 * (1 - factor), 128 * (1 - factor), 0};
        BufferedImageOp op = new RescaleOp(scales, offsets, null);
        op.filter(original, adjusted);

        setImage(adjusted);
    }

    /**
     * Adjusts the saturation of the layer's image
     * @param value Value between -100 and 100
     */
    public void adjustSaturation(int value) {
        if (value == 0) return;

        BufferedImage original = getImage();
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage adjusted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Manual saturation adjustment
        float saturationFactor = 1.0f + (value / 100.0f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                hsb[1] = Math.max(0, Math.min(1, hsb[1] * saturationFactor));

                int newRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                // Preserve alpha
                newRGB = (a << 24) | (newRGB & 0x00ffffff);
                adjusted.setRGB(x, y, newRGB);
            }
        }

        setImage(adjusted);
    }

    /**
     * Adjusts the opacity of the layer's image
     * @param value Value between 0 and 100, representing opacity percentage
     */
    public void adjustOpacity(int value) {
        if (value < 0 || value > 100) {
            return;
        }

        BufferedImage original = getImage();
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage adjusted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Convert percentage to alpha value (0-255)
        float alphaFactor = value / 100.0f;

        // Apply opacity adjustment by modifying alpha channel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Apply opacity factor to existing alpha
                int newAlpha = Math.round(a * alphaFactor);

                // Compose the new ARGB value
                int newRgb = (newAlpha << 24) | (r << 16) | (g << 8) | b;
                adjusted.setRGB(x, y, newRgb);
            }
        }

        setImage(adjusted);
    }
}