package com.esgdev.sparkpaint.engine.tools.renderers;

import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;

public class FillRendererTest {

    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color RED = Color.RED;

    private static GeneralPath fullArea(int width, int height) {
        GeneralPath path = new GeneralPath();
        path.append(new Rectangle(0, 0, width, height), false);
        return path;
    }

    @Test
    public void canvasFillReplacesEveryPixelInsideClip() {
        BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                image.setRGB(x, y, BLACK.getRGB());
            }
        }

        new FillRenderer(null).canvasFill(image, RED, fullArea(6, 6));

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                assertEquals("pixel " + x + "," + y, RED.getRGB(), image.getRGB(x, y));
            }
        }
    }

    @Test
    public void smartFillStaysWithinContrastingBoundary() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                image.setRGB(x, y, x == 5 ? BLACK.getRGB() : WHITE.getRGB());
            }
        }

        new FillRenderer(null).smartFill(image, 1, 1, WHITE, RED, 10, fullArea(10, 10));

        assertEquals(RED.getRGB(), image.getRGB(1, 1));
        assertEquals(RED.getRGB(), image.getRGB(4, 8));
        assertEquals(BLACK.getRGB(), image.getRGB(5, 1));
        assertEquals(WHITE.getRGB(), image.getRGB(8, 1));
    }
}
