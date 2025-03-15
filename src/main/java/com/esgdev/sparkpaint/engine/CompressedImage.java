package com.esgdev.sparkpaint.engine;

public class CompressedImage {
    private final byte[] compressedData;
    private final int width;
    private final int height;

    public CompressedImage(byte[] compressedData, int width, int height) {
        this.compressedData = compressedData;
        this.width = width;
        this.height = height;
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
}