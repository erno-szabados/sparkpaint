package com.esgdev.sparkpaint.engine;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ImageLimitsTest {

    @Test
    public void acceptsMaximumDimension() throws Exception {
        ImageLimits.verifyDimensions(ImageLimits.MAX_DIMENSION, ImageLimits.MAX_DIMENSION);
    }

    @Test
    public void rejectsDimensionAboveMaximum() {
        assertThrows(ImageLimits.ImageSizeException.class,
                () -> ImageLimits.verifyDimensions(ImageLimits.MAX_DIMENSION + 1, 100));
        assertThrows(ImageLimits.ImageSizeException.class,
                () -> ImageLimits.verifyDimensions(100, ImageLimits.MAX_DIMENSION + 1));
    }

    @Test
    public void rejectsZeroDimension() {
        assertThrows(ImageLimits.ImageSizeException.class, () -> ImageLimits.verifyDimensions(0, 100));
    }

    @Test
    public void refusesWhenResidentImagesExceedAnyHeap() {
        // Far beyond any realistic heap, so the result does not depend on the test JVM's -Xmx.
        assertThrows(ImageLimits.ImageSizeException.class,
                () -> ImageLimits.verify(ImageLimits.MAX_DIMENSION, ImageLimits.MAX_DIMENSION, 1_000_000));
    }

}
