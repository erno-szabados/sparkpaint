package com.esgdev.sparkpaint.engine.filters;

import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SobelFilterTest {

    @Test
    public void testEmptyImage() {
        // Create a solid color image
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 10, 10);
        g2d.dispose();

        // Apply Sobel filter
        BufferedImage result = SobelFilter.apply(image);

        // Result should not be null
        assertNotNull("Result should not be null", result);
        assertEquals("Width should match original", image.getWidth(), result.getWidth());
        assertEquals("Height should match original", image.getHeight(), result.getHeight());

        // Check center pixel - should be mostly black (edges are at the borders)
        int centerRGB = result.getRGB(5, 5);
        int centerRed = (centerRGB >> 16) & 0xFF;
        int centerGreen = (centerRGB >> 8) & 0xFF;
        int centerBlue = centerRGB & 0xFF;

        // Center should have low values (close to black) since there are no edges
        assertTrue("Center red value should be low", centerRed < 50);
        assertTrue("Center green value should be low", centerGreen < 50);
        assertTrue("Center blue value should be low", centerBlue < 50);
    }

    @Test
    public void testEdgeDetection() {
        // Create an image with a vertical edge
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 5, 10);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(5, 0, 5, 10);
        g2d.dispose();

        // Apply Sobel filter
        BufferedImage result = SobelFilter.apply(image);

        // Check edge pixel (at x=5, y=5)
        int edgeRGB = result.getRGB(5, 5);
        int edgeRed = (edgeRGB >> 16) & 0xFF;
        int edgeGreen = (edgeRGB >> 8) & 0xFF;
        int edgeBlue = edgeRGB & 0xFF;

        // Edge should have high values (close to white)
        assertTrue("Edge red value should be high", edgeRed > 200);
        assertTrue("Edge green value should be high", edgeGreen > 200);
        assertTrue("Edge blue value should be high", edgeBlue > 200);

        // Check non-edge pixel (at x=2, y=5)
        int nonEdgeRGB = result.getRGB(2, 5);
        int nonEdgeRed = (nonEdgeRGB >> 16) & 0xFF;
        int nonEdgeGreen = (nonEdgeRGB >> 8) & 0xFF;
        int nonEdgeBlue = nonEdgeRGB & 0xFF;

        // Non-edge should have low values (close to black)
        assertTrue("Non-edge red value should be low", nonEdgeRed < 50);
        assertTrue("Non-edge green value should be low", nonEdgeGreen < 50);
        assertTrue("Non-edge blue value should be low", nonEdgeBlue < 50);
    }
}