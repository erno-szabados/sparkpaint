package com.esgdev.sparkpaint.engine.history;

public class CompressedLayer {
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
}
