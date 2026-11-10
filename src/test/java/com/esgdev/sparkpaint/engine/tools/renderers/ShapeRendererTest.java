package com.esgdev.sparkpaint.engine.tools.renderers;

import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;

public class ShapeRendererTest {

    @Test
    public void rectangleCornersAreNormalizedRegardlessOfDragDirection() {
        RectangleToolRenderer renderer = new RectangleToolRenderer();

        assertEquals(new Rectangle(10, 5, 20, 15),
                renderer.calculateRectangleBounds(new Point(10, 20), new Point(30, 5), false));
    }

    @Test
    public void rectangleSquareModeUsesLargerSide() {
        RectangleToolRenderer renderer = new RectangleToolRenderer();

        assertEquals(new Rectangle(10, 10, 20, 20),
                renderer.calculateRectangleBounds(new Point(10, 10), new Point(30, 15), true));
    }

    @Test
    public void ellipseCenterBasedUsesDragDistanceAsRadius() {
        EllipseToolRenderer renderer = new EllipseToolRenderer();

        // Distance from (50,50) to (53,54) is 5
        assertEquals(new Rectangle(45, 45, 10, 10),
                renderer.calculateEllipseBounds(new Point(50, 50), new Point(53, 54), false, true));
    }

    @Test
    public void ellipseCornerBasedCircleUsesLargerSide() {
        EllipseToolRenderer renderer = new EllipseToolRenderer();

        assertEquals(new Rectangle(10, 10, 20, 20),
                renderer.calculateEllipseBounds(new Point(10, 10), new Point(30, 15), true, false));
    }

    @Test
    public void ellipseCornerBasedNonCircleKeepsAspectRatio() {
        EllipseToolRenderer renderer = new EllipseToolRenderer();

        assertEquals(new Rectangle(10, 10, 20, 5),
                renderer.calculateEllipseBounds(new Point(10, 10), new Point(30, 15), false, false));
    }

    @Test
    public void filledRectangleInteriorIsPaintedWithFillColor() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            new RectangleToolRenderer().drawRectangle(image, g2d, new Rectangle(2, 2, 12, 12),
                    Color.BLACK, Color.RED, 1f, true, false);
        } finally {
            g2d.dispose();
        }

        assertEquals(Color.RED.getRGB(), image.getRGB(8, 8));
        assertEquals(0, image.getRGB(0, 0));
    }

    @Test
    public void filledEllipseCenterIsPaintedWithFillColor() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            new EllipseToolRenderer().drawEllipse(image, g2d, new Rectangle(2, 2, 16, 16),
                    Color.BLACK, Color.RED, 1f, true, false);
        } finally {
            g2d.dispose();
        }

        assertEquals(Color.RED.getRGB(), image.getRGB(10, 10));
        assertEquals(0, image.getRGB(2, 2));
    }
}
