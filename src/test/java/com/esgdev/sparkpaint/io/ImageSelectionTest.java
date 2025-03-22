package com.esgdev.sparkpaint.io;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageSelectionTest {

    private BufferedImage testImage;
    private ImageSelection imageSelection;

    @Before
    public void setUp() {
        // Create a simple test image
        testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        // Create ImageSelection instance with the test image
        imageSelection = new ImageSelection(testImage);
    }

    @Test
    public void testGetTransferDataFlavors() {
        DataFlavor[] flavors = imageSelection.getTransferDataFlavors();
        assertEquals(1, flavors.length);
        assertEquals(DataFlavor.imageFlavor, flavors[0]);
    }

    @Test
    public void testIsDataFlavorSupported() {
        assertTrue(imageSelection.isDataFlavorSupported(DataFlavor.imageFlavor));
        assertFalse(imageSelection.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Test
    public void testGetTransferData() {
        Object data = imageSelection.getTransferData(DataFlavor.imageFlavor);
        assertNotNull(data);
        assertTrue(data instanceof BufferedImage);

        // Test with unsupported flavor
        assertNull(imageSelection.getTransferData(DataFlavor.stringFlavor));
    }

    @Test
    public void testCopyAndPasteImage() throws UnsupportedFlavorException, IOException {
        // Note: This test interacts with system clipboard
        // It might fail in environments where clipboard access is restricted

        // Copy the image to clipboard
        ImageSelection.copyImage(testImage);

        // Try to paste it back
        BufferedImage pastedImage = ImageSelection.pasteImage();

        // Verify we got an image back
        assertNotNull(pastedImage);
        assertEquals(testImage.getWidth(), pastedImage.getWidth());
        assertEquals(testImage.getHeight(), pastedImage.getHeight());
        assertEquals(testImage.getType(), pastedImage.getType());
    }

    @Test
    public void testImageCopy() {
        // Ensure the constructor makes a defensive copy
        BufferedImage original = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = original.createGraphics();
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        ImageSelection selection = new ImageSelection(original);

        BufferedImage copied = (BufferedImage) selection.getTransferData(DataFlavor.imageFlavor);

        // Modify the original
        Graphics2D g2 = original.createGraphics();
        g2.clearRect(0, 0, 10, 10);
        g2.dispose();

        // Check a pixel to ensure copy is unchanged
        assertNotEquals(original.getRGB(5, 5), copied.getRGB(5, 5));
    }
}