package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.FilterBrushTool;
import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FilterBrushRendererTest {

    private static final int GRAY = 100;

    private static BufferedImage grayImage() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                image.setRGB(x, y, new Color(GRAY, GRAY, GRAY).getRGB());
            }
        }
        return image;
    }

    @Test
    public void brightenFilterRaisesLuminanceInsideBrushArea() {
        BufferedImage image = grayImage();
        FilterBrushRenderer renderer = new FilterBrushRenderer(mock(DrawingCanvas.class));

        renderer.applyFilter(image, FilterBrushTool.FilterType.BRIGHTEN,
                10, 10, 8, 1.0f, new Rectangle(0, 0, 20, 20));

        Color center = new Color(image.getRGB(10, 10));
        assertTrue("brightened center should exceed original gray, was " + center.getRed(),
                center.getRed() > GRAY);
    }

    @Test
    public void darkenFilterLowersLuminanceInsideBrushArea() {
        BufferedImage image = grayImage();
        FilterBrushRenderer renderer = new FilterBrushRenderer(mock(DrawingCanvas.class));

        renderer.applyFilter(image, FilterBrushTool.FilterType.DARKEN,
                10, 10, 8, 1.0f, new Rectangle(0, 0, 20, 20));

        Color center = new Color(image.getRGB(10, 10));
        assertTrue("darkened center should be below original gray, was " + center.getRed(),
                center.getRed() < GRAY);
    }

    @Test
    public void filterDoesNotTouchPixelsOutsideClip() {
        BufferedImage image = grayImage();
        FilterBrushRenderer renderer = new FilterBrushRenderer(mock(DrawingCanvas.class));
        int untouched = image.getRGB(0, 0);

        renderer.applyFilter(image, FilterBrushTool.FilterType.BRIGHTEN,
                10, 10, 8, 1.0f, new Rectangle(8, 8, 4, 4));

        assertEquals("pixel outside clip must be unchanged", untouched, image.getRGB(0, 0));
    }
}
