package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;

/**
 * CompressedLayer represents a layer with compressed image data.
 * It provides methods to create a CompressedLayer from a Layer and to convert it back to a Layer.
 */
public class CompressedLayer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] compressedData;
    private final int width;
    private final int height;
    private final boolean visible;
    private final String name;

    /**
     * Constructs a CompressedLayer with the given properties.
     *
     * @param compressedData The compressed image data
     * @param width The width of the layer
     * @param height The height of the layer
     * @param visible Whether the layer is visible
     * @param name The name of the layer
     */
    public CompressedLayer(byte[] compressedData, int width, int height, boolean visible, String name) {
        this.compressedData = compressedData;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.name = name;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getName() {
        return name;
    }

    public static CompressedLayer fromLayer(Layer layer) {
        BufferedImage image = layer.getImage();
        byte[] compressedData = HistoryManager.compressImage(image);

        return new CompressedLayer(
                compressedData,
                image.getWidth(),
                image.getHeight(),
                layer.isVisible(),
                layer.getName());
    }

    public Layer toLayer() throws IOException {
        try {
            BufferedImage image = HistoryManager.decompressImage(compressedData, width, height);

            Layer layer = new Layer(image);
            layer.setVisible(visible);
            layer.setName(name != null ? name : "Layer");

            return layer;
        } catch (Exception e) {
            throw new IOException("Failed to decompress layer image", e);
        }
    }
}