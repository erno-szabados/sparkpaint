package com.esgdev.sparkpaint.engine;

import java.io.IOException;

/**
 * Size limits and heap budget checks applied before allocating full-size images.
 */
public final class ImageLimits {
    /** Largest accepted width or height, in pixels. */
    public static final int MAX_DIMENSION = 4000;

    /** Bytes per pixel for the TYPE_INT_ARGB images used throughout the application. */
    private static final long BYTES_PER_PIXEL = 4;

    /** Full-size buffers held transiently while compressing, copying, or decoding a layer. */
    private static final int TRANSIENT_COPIES = 3;

    private ImageLimits() {
    }

    /**
     * Verifies that the dimensions are within the supported range.
     *
     * @throws ImageSizeException if either dimension is below 1 or above {@link #MAX_DIMENSION}
     */
    public static void verifyDimensions(int width, int height) throws ImageSizeException {
        if (width < 1 || height < 1) {
            throw new ImageSizeException("Image width and height must be at least 1 pixel.");
        }
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new ImageSizeException(String.format(
                    "Image dimensions cannot exceed %dx%d pixels (requested %dx%d).",
                    MAX_DIMENSION, MAX_DIMENSION, width, height));
        }
    }

    /**
     * Verifies the dimensions and that the heap can hold the requested images.
     *
     * @param width          image width in pixels
     * @param height         image height in pixels
     * @param residentImages number of full-size images held at the same time after the operation,
     *                       not counting transient copies
     * @throws ImageSizeException if the dimensions are out of range or the estimated memory is not available
     */
    public static void verify(int width, int height, int residentImages) throws ImageSizeException {
        verifyDimensions(width, height);
        long required = (long) width * height * BYTES_PER_PIXEL * (residentImages + TRANSIENT_COPIES);
        if (required <= availableHeapBytes()) {
            return;
        }
        // Used heap may include unreachable objects, so reclaim them once before refusing.
        System.gc();
        if (required > availableHeapBytes()) {
            throw new ImageSizeException(String.format(
                    "Not enough memory for a %dx%d image. Try a smaller size.", width, height));
        }
    }

    private static long availableHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }

    /**
     * Thrown when an image is too large for the supported limits or the available memory.
     * Extends {@link IOException} so existing file and clipboard error handling reports it.
     */
    public static class ImageSizeException extends IOException {
        private static final long serialVersionUID = 1L;

        public ImageSizeException(String message) {
            super(message);
        }
    }
}
