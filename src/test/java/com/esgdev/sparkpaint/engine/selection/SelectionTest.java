package com.esgdev.sparkpaint.engine.selection;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class SelectionTest {

    private Selection rectangleSelection;
    private Selection pathSelection;
    private BufferedImage testImage;
    private Rectangle testRect;
    private GeneralPath testPath;

    @Before
    public void setUp() {
        // Create test image
        testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        // Create test rectangle
        testRect = new Rectangle(10, 10, 80, 80);

        // Create test path
        testPath = new GeneralPath();
        testPath.moveTo(20, 20);
        testPath.lineTo(80, 20);
        testPath.lineTo(80, 80);
        testPath.lineTo(20, 80);
        testPath.closePath();

        // Create selection objects
        rectangleSelection = new Selection(testRect, testImage);
        pathSelection = new Selection(testPath, testImage);
    }

    @Test
    public void testRectangleConstructor() {
        assertNotNull("Path should be initialized", rectangleSelection.getPath());
        assertNotNull("Content should be set", rectangleSelection.getContent());
        assertEquals("Content should match provided image", testImage, rectangleSelection.getContent());

        Rectangle bounds = rectangleSelection.getBounds();
        assertEquals("X coordinate should match", testRect.x, bounds.x);
        assertEquals("Y coordinate should match", testRect.y, bounds.y);
        assertEquals("Width should match", testRect.width, bounds.width);
        assertEquals("Height should match", testRect.height, bounds.height);
    }

    @Test
    public void testPathConstructor() {
        assertNotNull("Path should be initialized", pathSelection.getPath());
        assertNotNull("Content should be set", pathSelection.getContent());
        assertEquals("Content should match provided image", testImage, pathSelection.getContent());

        Rectangle bounds = pathSelection.getBounds();
        assertEquals("Path bounds should match", testPath.getBounds(), bounds);
    }

    @Test
    public void testContains() {
        // Point inside the rectangle
        Point insidePoint = new Point(50, 50);
        assertTrue("Selection should contain point inside bounds",
                rectangleSelection.contains(insidePoint));

        // Point outside the rectangle
        Point outsidePoint = new Point(5, 5);
        assertFalse("Selection should not contain point outside bounds",
                rectangleSelection.contains(outsidePoint));
    }

    @Test
    public void testClear() {
        rectangleSelection.clear();
        assertNull("Path should be null after clear", rectangleSelection.getPath());
        assertNull("Content should be null after clear", rectangleSelection.getContent());
    }

    @Test
    public void testClearOutline() {
        rectangleSelection.clearOutline();
        assertNull("Path should be null after clearOutline", rectangleSelection.getPath());
        assertNotNull("Content should remain after clearOutline", rectangleSelection.getContent());
    }

    @Test
    public void testHasOutline() {
        assertTrue("Should have outline initially", rectangleSelection.hasOutline());

        rectangleSelection.clearOutline();
        assertFalse("Should not have outline after clearOutline", rectangleSelection.hasOutline());
    }

    @Test
    public void testTransparentFlag() {
        // Default should be false
        assertFalse("Selection should not be transparent by default", rectangleSelection.isTransparent());

        // Set to true
        rectangleSelection.setTransparent(true);
        assertTrue("Selection should be transparent after setting", rectangleSelection.isTransparent());
    }
}