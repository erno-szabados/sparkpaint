package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SparkPaintFileFormatTest {

    private List<Layer> testLayers;
    private File testFile;
    private int currentLayerIndex;

    @Before
    public void setUp() {
        testLayers = new ArrayList<>();

        // Create test layers with different content
        BufferedImage bgImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = bgImage.createGraphics();
        g1.setColor(Color.BLUE);
        g1.fillRect(0, 0, 100, 100);
        g1.dispose();
        Layer bgLayer = new Layer(bgImage);
        bgLayer.setName("Background");

        BufferedImage fgImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = fgImage.createGraphics();
        g2.setColor(Color.RED);
        g2.fillOval(25, 25, 50, 50);
        g2.dispose();
        Layer fgLayer = new Layer(fgImage);
        fgLayer.setName("Foreground");

        testLayers.add(bgLayer);
        testLayers.add(fgLayer);
        currentLayerIndex = 1;

        // Create temp file for testing
        testFile = new File("test-sparkpaint.spp");
    }

    @After
    public void tearDown() {
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void testSaveAndLoadFile() throws IOException, ClassNotFoundException {
        // Save layers to file
        SparkPaintFileFormat.saveToFile(testFile, testLayers, currentLayerIndex);

        // Verify file exists
        assertTrue("File should exist after saving", testFile.exists());
        assertTrue("File should have content", testFile.length() > 0);

        // Load layers from file
        LayerState loadedState = SparkPaintFileFormat.loadFromFile(testFile);

        // Verify loaded state
        assertNotNull("Loaded state should not be null", loadedState);
        List<Layer> loadedLayers = loadedState.getLayers();

        assertEquals("Should have same number of layers", testLayers.size(), loadedLayers.size());
        assertEquals("Current layer index should match", currentLayerIndex, loadedState.getCurrentLayerIndex());

        // Verify layer properties
        for (int i = 0; i < testLayers.size(); i++) {
            Layer originalLayer = testLayers.get(i);
            Layer loadedLayer = loadedLayers.get(i);

            assertEquals("Layer names should match", originalLayer.getName(), loadedLayer.getName());
            assertEquals("Layer visibility should match", originalLayer.isVisible(), loadedLayer.isVisible());
            assertEquals("Layer dimensions should match", originalLayer.getImage().getWidth(), loadedLayer.getImage().getWidth());
            assertEquals("Layer dimensions should match", originalLayer.getImage().getHeight(), loadedLayer.getImage().getHeight());

            // Sample some pixels to verify image content
            if (i == 0) { // Background layer
                Color centerColor = new Color(loadedLayer.getImage().getRGB(50, 50), true);
                assertEquals("Center pixel should be blue", Color.BLUE.getRGB(), centerColor.getRGB());
            } else if (i == 1) { // Foreground layer
                Color centerColor = new Color(loadedLayer.getImage().getRGB(50, 50), true);
                assertEquals("Center pixel should be red", Color.RED.getRGB(), centerColor.getRGB());

                Color cornerColor = new Color(loadedLayer.getImage().getRGB(10, 10), true);
                assertEquals("Corner should be transparent", 0, cornerColor.getAlpha());
            }
        }
    }

    @Test(expected = IOException.class)
    public void testLoadCorruptedFile() throws IOException, ClassNotFoundException {
        // Create file with invalid content
        Files.write(testFile.toPath(), "INVALID CONTENT".getBytes());

        // Should throw IOException due to invalid magic number
        SparkPaintFileFormat.loadFromFile(testFile);
    }

    @Test
    public void testLayerVisibilityPreservation() throws IOException, ClassNotFoundException {
        // Set second layer to invisible
        testLayers.get(1).setVisible(false);

        // Save and load
        SparkPaintFileFormat.saveToFile(testFile, testLayers, currentLayerIndex);
        LayerState loadedState = SparkPaintFileFormat.loadFromFile(testFile);

        // Verify visibility was preserved
        assertFalse("Layer should still be invisible", loadedState.getLayers().get(1).isVisible());
    }

    @Test
    public void testDataIntegrity() throws IOException, ClassNotFoundException {
        // Create layers with specific content for checking
        BufferedImage patternImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = patternImage.createGraphics();
        // Create a checkered pattern
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                g.setColor((x + y) % 2 == 0 ? Color.BLACK : Color.WHITE);
                g.fillRect(x * 10, y * 10, 10, 10);
            }
        }
        g.dispose();

        Layer patternLayer = new Layer(patternImage);
        patternLayer.setName("Pattern Layer");

        List<Layer> patternLayers = new ArrayList<>();
        patternLayers.add(patternLayer);

        // Save and load
        SparkPaintFileFormat.saveToFile(testFile, patternLayers, 0);
        LayerState loadedState = SparkPaintFileFormat.loadFromFile(testFile);

        BufferedImage loadedImage = loadedState.getLayers().get(0).getImage();

        // Check pattern integrity
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                Color expectedColor = (x + y) % 2 == 0 ? Color.BLACK : Color.WHITE;
                Color actualColor = new Color(loadedImage.getRGB(x * 10 + 5, y * 10 + 5), true);

                assertEquals("Pattern color should be preserved at " + x + "," + y,
                        expectedColor.getRGB(), actualColor.getRGB());
            }
        }
    }

    @Test(expected = IOException.class)
    public void testInvalidCRC() throws IOException, ClassNotFoundException {
        // Save file normally
        SparkPaintFileFormat.saveToFile(testFile, testLayers, currentLayerIndex);

        // Read the file content
        byte[] fileBytes = Files.readAllBytes(testFile.toPath());

        // Corrupt the file by modifying a byte in the middle of the data section
        // We need to find a position after the header but before the CRC
        // Header is 4 bytes (MAGIC) + 4 bytes (VERSION) + 4 bytes (dataLength)
        int headerSize = 12;
        // Let's corrupt a byte in the middle of the data
        int middleDataPosition = headerSize + fileBytes.length / 4;
        fileBytes[middleDataPosition] = (byte)(fileBytes[middleDataPosition] ^ 0xFF); // Flip bits

        // Write the corrupted file back
        Files.write(testFile.toPath(), fileBytes);

        // Should throw exception due to CRC mismatch
        SparkPaintFileFormat.loadFromFile(testFile);
    }
}