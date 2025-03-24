package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileManagerTest {

    private FileManager fileManager;
    private List<Layer> testLayers;
    private File pngFile;
    private File jpgFile;
    private File bmpFile;

    @Before
    public void setUp() {
        fileManager = new FileManager();
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

        // Create temp files for testing
        pngFile = new File("test-output.png");
        jpgFile = new File("test-output.jpg");
        bmpFile = new File("test-output.bmp");
    }

    @After
    public void tearDown() {
        // Clean up test files
        if (pngFile.exists()) pngFile.delete();
        if (jpgFile.exists()) jpgFile.delete();
        if (bmpFile.exists()) bmpFile.delete();
    }

    @Test
    public void testSaveAndLoadPNG() throws IOException {
        // Save to PNG
        fileManager.saveToFile(pngFile, testLayers);

        // Verify file was created
        assertTrue("PNG file should exist", pngFile.exists());
        assertEquals("Current file path should be set", pngFile.getAbsolutePath(), fileManager.getCurrentFilePath());

        // Load the file
        LayerState loadedState = fileManager.loadFromFile(pngFile);

        // Verify loaded state
        assertNotNull("Loaded state should not be null", loadedState);
        assertEquals("Should have one layer", 1, loadedState.getLayers().size());
        assertEquals("Layer name should be Background", "Background", loadedState.getLayers().get(0).getName());
        assertEquals("Current layer index should be 0", 0, loadedState.getCurrentLayerIndex());
    }

    @Test
    public void testSaveAndLoadJPG() throws IOException {
        // Save to JPG
        fileManager.saveToFile(jpgFile, testLayers);

        // Verify file was created
        assertTrue("JPG file should exist", jpgFile.exists());
        assertEquals("Current file path should be set", jpgFile.getAbsolutePath(), fileManager.getCurrentFilePath());

        // Load the file
        LayerState loadedState = fileManager.loadFromFile(jpgFile);

        // Verify loaded state
        assertNotNull("Loaded state should not be null", loadedState);
        assertEquals("Should have one layer", 1, loadedState.getLayers().size());
    }

    @Test
    public void testSaveAndLoadBMP() throws IOException {
        // Save to BMP
        fileManager.saveToFile(bmpFile, testLayers);

        // Verify file was created
        assertTrue("BMP file should exist", bmpFile.exists());
        assertEquals("Current file path should be set", bmpFile.getAbsolutePath(), fileManager.getCurrentFilePath());

        // Load the file
        LayerState loadedState = fileManager.loadFromFile(bmpFile);

        // Verify loaded state
        assertNotNull("Loaded state should not be null", loadedState);
        assertEquals("Should have one layer", 1, loadedState.getLayers().size());
    }

    @Test
    public void testLayerVisibility() throws IOException {
        // Make second layer invisible
        testLayers.get(1).setVisible(false);

        // Save file
        fileManager.saveToFile(pngFile, testLayers);

        // Load image directly to verify content
        BufferedImage loaded = ImageIO.read(pngFile);

        // Check a pixel that would be red if second layer were visible
        Color centerColor = new Color(loaded.getRGB(50, 50), true);

        // Should be blue (background) not red (foreground)
        assertEquals(Color.BLUE.getRed(), centerColor.getRed());
        assertEquals(Color.BLUE.getGreen(), centerColor.getGreen());
        assertEquals(Color.BLUE.getBlue(), centerColor.getBlue());
    }

    @Test(expected = IOException.class)
    public void testLoadNonExistentFile() throws IOException {
        File nonExistent = new File("does-not-exist.png");
        fileManager.loadFromFile(nonExistent);
    }

    @Test
    public void testSetAndGetCurrentFilePath() {
        String testPath = "/path/to/file.png";
        fileManager.setCurrentFilePath(testPath);
        assertEquals(testPath, fileManager.getCurrentFilePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFlattenedImageWithEmptyLayers() throws IOException {
        fileManager.saveToFile(pngFile, new ArrayList<>());
    }

    @Test
    public void testLoadInvalidImage() {
        // Create an empty file that's not an image
        try {
            pngFile.createNewFile();
            fileManager.loadFromFile(pngFile);
            fail("Should have thrown an IOException");
        } catch (IOException e) {
            // Expected behavior
            assertTrue(e.getMessage().contains("Failed to load image"));
        }
    }
}