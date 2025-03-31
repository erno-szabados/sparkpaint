package com.esgdev.sparkpaint.io;

import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;

import static org.junit.Assert.*;

import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageSelectionTest {

    private BufferedImage testImage;
    private ImageSelection imageSelection;

    @Before
    public void setUp() throws IOException {
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
    assertEquals(ImageSelection.PNG_FLAVOR, flavors[0]);
}

@Test
public void testIsDataFlavorSupported() {
    assertTrue(imageSelection.isDataFlavorSupported(ImageSelection.PNG_FLAVOR));
    assertFalse(imageSelection.isDataFlavorSupported(DataFlavor.stringFlavor));
}

@Test
public void testImageCopy() throws IOException, UnsupportedFlavorException {
    // Ensure the constructor makes a defensive copy
    BufferedImage original = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = original.createGraphics();
    g.fillRect(0, 0, 10, 10);
    g.dispose();

    ImageSelection selection = new ImageSelection(original);

    // Now we need to get PNG data and convert back to image
    ByteArrayInputStream bais = (ByteArrayInputStream) selection.getTransferData(ImageSelection.PNG_FLAVOR);
    BufferedImage copied = ImageIO.read(bais);

    // Modify the original
    Graphics2D g2 = original.createGraphics();
    g2.clearRect(0, 0, 10, 10);
    g2.dispose();

    // Check a pixel to ensure copy is unchanged
    assertNotEquals(original.getRGB(5, 5), copied.getRGB(5, 5));
}
    @Test
    public void testGetTransferData() throws IOException, UnsupportedFlavorException {
        // Test with supported flavor
        Object data = imageSelection.getTransferData(ImageSelection.PNG_FLAVOR);
        assertNotNull(data);
        assertTrue(data instanceof ByteArrayInputStream);

        // Test with unsupported flavor - should throw UnsupportedFlavorException
        try {
            imageSelection.getTransferData(DataFlavor.stringFlavor);
            fail("Expected UnsupportedFlavorException was not thrown");
        } catch (UnsupportedFlavorException e) {
            // Expected exception
        }
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

        // Don't check type since PNG conversion might change it
        // (TYPE_INT_RGB=1 becomes TYPE_INT_ARGB=5)

        // Instead, check that the content is preserved
        for (int x = 0; x < testImage.getWidth(); x++) {
            for (int y = 0; y < testImage.getHeight(); y++) {
                assertEquals(testImage.getRGB(x, y) & 0xFFFFFF, pastedImage.getRGB(x, y) & 0xFFFFFF);
            }
        }
    }
}