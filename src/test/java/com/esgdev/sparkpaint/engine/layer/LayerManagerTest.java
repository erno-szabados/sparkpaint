package com.esgdev.sparkpaint.engine.layer;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LayerManagerTest {

    private LayerManager layerManager;
    private DrawingCanvas mockCanvas;
    private static final int TEST_WIDTH = 100;
    private static final int TEST_HEIGHT = 80;

    @Before
    public void setUp() {
        mockCanvas = mock(DrawingCanvas.class);
        when(mockCanvas.getWidth()).thenReturn(TEST_WIDTH);
        when(mockCanvas.getHeight()).thenReturn(TEST_HEIGHT);

        layerManager = new LayerManager(mockCanvas);
        layerManager.initializeLayers(TEST_WIDTH, TEST_HEIGHT);
    }

    @Test
    public void testInitializeLayers() {
        // Verify initialization
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
        assertNotNull(layerManager.getTransparencyBackground());
        assertEquals(TEST_WIDTH, layerManager.getCurrentLayerImage().getWidth());
        assertEquals(TEST_HEIGHT, layerManager.getCurrentLayerImage().getHeight());
    }

    @Test
    public void testGetCanvas() {
        assertEquals(mockCanvas, layerManager.getCanvas());
    }

    @Test
    public void testTransparencyVisualization() {
        // Default should be enabled
        assertTrue(layerManager.isTransparencyVisualizationEnabled());

        // Test disabling
        layerManager.setTransparencyVisualizationEnabled(false);
        assertFalse(layerManager.isTransparencyVisualizationEnabled());
        verify(mockCanvas).repaint();

        // Test enabling
        layerManager.setTransparencyVisualizationEnabled(true);
        assertTrue(layerManager.isTransparencyVisualizationEnabled());
        verify(mockCanvas, times(2)).repaint();
    }

    @Test
    public void testAddNewLayer() {
        layerManager.addNewLayer();

        assertEquals(2, layerManager.getLayers().size());
        assertEquals(1, layerManager.getCurrentLayerIndex());
        verify(mockCanvas).repaint();
    }

    @Test
    public void testDuplicateCurrentLayer() {
        // Create a fresh instance to ensure isolation
        mockCanvas = mock(DrawingCanvas.class);
        when(mockCanvas.getWidth()).thenReturn(TEST_WIDTH);
        when(mockCanvas.getHeight()).thenReturn(TEST_HEIGHT);
        layerManager = new LayerManager(mockCanvas);
        layerManager.initializeLayers(TEST_WIDTH, TEST_HEIGHT);

        // Setup: Create a pattern in first layer to verify duplication
        BufferedImage img = layerManager.getCurrentLayerImage();
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(10, 10, 20, 20);
        g2d.dispose();

        // Get the name before duplication to make assertion more robust
        String expectedName = layerManager.getLayers().get(0).getName() + " (Copy)";

        // Duplicate the layer
        boolean result = layerManager.duplicateCurrentLayer();

        // Verify
        assertTrue(result);
        assertEquals(2, layerManager.getLayers().size());
        assertEquals(1, layerManager.getCurrentLayerIndex());
        verify(mockCanvas).repaint();

        // Verify content was duplicated
        BufferedImage duplicatedImg = layerManager.getCurrentLayerImage();
        assertEquals(Color.RED.getRGB(), duplicatedImg.getRGB(15, 15));
        assertEquals(expectedName, layerManager.getLayers().get(1).getName());
    }

    @Test
    public void testDeleteCurrentLayer() {
        // Add a second layer
        layerManager.addNewLayer();
        assertEquals(1, layerManager.getCurrentLayerIndex());

        // Delete it
        layerManager.deleteCurrentLayer();

        // Verify
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
        verify(mockCanvas, times(2)).repaint();
    }

    @Test
    public void testDeleteCurrentLayerWhenOnlyOneLayer() {
        // Try to delete when there's only one layer
        layerManager.deleteCurrentLayer();

        // Verify that nothing changed
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testDeleteLayer() {
        // Add two more layers
        layerManager.addNewLayer();
        layerManager.addNewLayer();
        assertEquals(3, layerManager.getLayers().size());
        assertEquals(2, layerManager.getCurrentLayerIndex());

        // Delete middle layer
        layerManager.deleteLayer(1);

        // Verify
        assertEquals(2, layerManager.getLayers().size());
        assertEquals(1, layerManager.getCurrentLayerIndex());
        verify(mockCanvas, times(3)).repaint();
    }

    @Test
    public void testDeleteLayerCurrentLayerAboveDeleted() {
        // Add two layers and set current to middle
        layerManager.addNewLayer();
        layerManager.addNewLayer();
        layerManager.setCurrentLayerIndex(1);

        // Delete top layer
        layerManager.deleteLayer(2);

        // Current index shouldn't change
        assertEquals(2, layerManager.getLayers().size());
        assertEquals(1, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testDeleteLayerInvalidIndex() {
        // Try to delete with invalid index
        layerManager.deleteLayer(-1);
        layerManager.deleteLayer(5);

        // Verify that nothing changed
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testMoveLayer() {
        // Add two more layers
        layerManager.addNewLayer();
        layerManager.addNewLayer();

        // Move bottom layer to top
        boolean result = layerManager.moveLayer(0, 2);

        // Verify
        assertTrue(result);
        assertEquals(3, layerManager.getLayers().size());
        verify(mockCanvas, times(3)).repaint();
    }

    @Test
    public void testMoveLayerWithCurrentLayerBeingMoved() {
        // Add two more layers
        layerManager.addNewLayer();
        layerManager.addNewLayer();
        layerManager.setCurrentLayerIndex(1); // Set middle as current

        // Move current layer to top
        layerManager.moveLayer(1, 2);

        // Current index should follow the layer
        assertEquals(2, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testMoveLayerWithCurrentLayerAffected() {
        // Add two more layers
        layerManager.addNewLayer();
        layerManager.addNewLayer();
        layerManager.setCurrentLayerIndex(1); // Set middle as current

        // Move bottom layer above current
        layerManager.moveLayer(0, 2);

        // Current index should be adjusted
        assertEquals(0, layerManager.getCurrentLayerIndex());

        // Move top layer below current
        layerManager.moveLayer(2, 0);

        // Current index should be adjusted
        assertEquals(1, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testMoveLayerInvalidIndices() {
        // Try to move with invalid indices
        assertFalse(layerManager.moveLayer(-1, 0));
        assertFalse(layerManager.moveLayer(0, -1));
        assertFalse(layerManager.moveLayer(0, 5));
        assertFalse(layerManager.moveLayer(5, 0));
        assertFalse(layerManager.moveLayer(0, 0)); // Same index
    }

    @Test
    public void testSetLayers() {
        // Create new layers list
        List<Layer> newLayers = new ArrayList<>();
        newLayers.add(new Layer(TEST_WIDTH, TEST_HEIGHT));
        newLayers.add(new Layer(TEST_WIDTH, TEST_HEIGHT));

        // Set layers
        layerManager.setLayers(newLayers);

        // Verify
        assertEquals(2, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
        verify(mockCanvas).repaint();
    }

    @Test
    public void testSetLayersAdjustsCurrentLayerIndex() {
        // Create new layers list
        List<Layer> newLayers = new ArrayList<>();
        newLayers.add(new Layer(TEST_WIDTH, TEST_HEIGHT));

        // Set current layer index to something that will be out of bounds
        layerManager.addNewLayer();
        layerManager.setCurrentLayerIndex(1);

        // Set smaller layers list
        layerManager.setLayers(newLayers);

        // Current index should be adjusted to be within bounds
        assertEquals(0, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testSetCurrentLayerIndex() {
        layerManager.addNewLayer();
        layerManager.addNewLayer();

        layerManager.setCurrentLayerIndex(1);
        assertEquals(1, layerManager.getCurrentLayerIndex());

        // Invalid indices should be ignored
        layerManager.setCurrentLayerIndex(-1);
        assertEquals(1, layerManager.getCurrentLayerIndex());

        layerManager.setCurrentLayerIndex(5);
        assertEquals(1, layerManager.getCurrentLayerIndex());
    }

    @Test
    public void testMergeCurrentLayerDown() {
        // Setup: Create patterns in layers
        layerManager.addNewLayer();
        BufferedImage bottomImg = layerManager.getLayers().get(0).getImage();
        BufferedImage topImg = layerManager.getCurrentLayerImage();

        Graphics2D g2d1 = bottomImg.createGraphics();
        g2d1.setColor(Color.RED);
        g2d1.fillRect(10, 10, 20, 20);
        g2d1.dispose();

        Graphics2D g2d2 = topImg.createGraphics();
        g2d2.setColor(Color.BLUE);
        g2d2.fillRect(15, 15, 20, 20);
        g2d2.dispose();

        // Merge down
        boolean result = layerManager.mergeCurrentLayerDown();

        // Verify
        assertTrue(result);
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
        verify(mockCanvas, times(2)).repaint();

        // Check the merged image contains both colors
        BufferedImage mergedImg = layerManager.getCurrentLayerImage();
        assertEquals(Color.RED.getRGB(), mergedImg.getRGB(12, 12)); // Only bottom layer
        assertEquals(Color.BLUE.getRGB(), mergedImg.getRGB(25, 25)); // Only top layer
    }

    @Test
    public void testMergeCurrentLayerDownWhenBottomLayer() {
        // Try to merge when already at bottom layer
        boolean result = layerManager.mergeCurrentLayerDown();

        // Should fail
        assertFalse(result);
        assertEquals(1, layerManager.getLayers().size());
    }

    @Test
    public void testFlattenLayers() {
        // Create three layers with different content
        layerManager.addNewLayer();
        layerManager.addNewLayer();

        // Bottom layer: red square
        BufferedImage img1 = layerManager.getLayers().get(0).getImage();
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(10, 10, 20, 20);
        g1.dispose();

        // Middle layer: green square
        BufferedImage img2 = layerManager.getLayers().get(1).getImage();
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.GREEN);
        g2.fillRect(20, 20, 20, 20);
        g2.dispose();

        // Top layer: blue square
        BufferedImage img3 = layerManager.getLayers().get(2).getImage();
        Graphics2D g3 = img3.createGraphics();
        g3.setColor(Color.BLUE);
        g3.fillRect(30, 30, 20, 20);
        g3.dispose();

        // Flatten
        boolean result = layerManager.flattenLayers();

        // Verify
        assertTrue(result);
        assertEquals(1, layerManager.getLayers().size());
        assertEquals(0, layerManager.getCurrentLayerIndex());
        assertEquals("Flattened Layer", layerManager.getLayers().get(0).getName());
        verify(mockCanvas, times(3)).repaint();

        // Verify flattened image contains all three colors
        BufferedImage flattenedImg = layerManager.getCurrentLayerImage();
        assertEquals(Color.RED.getRGB(), flattenedImg.getRGB(15, 15));
        assertEquals(Color.GREEN.getRGB(), flattenedImg.getRGB(25, 25));
        assertEquals(Color.BLUE.getRGB(), flattenedImg.getRGB(35, 35));
    }

    @Test
    public void testFlattenLayersWithInvisibleLayer() {
        // Create two layers with different content
        layerManager.addNewLayer();

        // Bottom layer: red square
        BufferedImage img1 = layerManager.getLayers().get(0).getImage();
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(10, 10, 20, 20);
        g1.dispose();

        // Top layer: blue square (but invisible)
        BufferedImage img2 = layerManager.getLayers().get(1).getImage();
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(15, 15, 20, 20);
        g2.dispose();
        layerManager.getLayers().get(1).setVisible(false);

        // Flatten
        layerManager.flattenLayers();

        // Verify flattened image only contains the visible red color
        BufferedImage flattenedImg = layerManager.getCurrentLayerImage();
        assertEquals(Color.RED.getRGB(), flattenedImg.getRGB(15, 15));

        // The blue pixel should not be in the flattened image
        int pixel = flattenedImg.getRGB(25, 25);
        assertNotEquals(Color.BLUE.getRGB(), pixel);
    }

    @Test
    public void testFlattenLayersSingleLayer() {
        // Try to flatten with just one layer
        boolean result = layerManager.flattenLayers();

        // Should not change anything
        assertFalse(result);
        assertEquals(1, layerManager.getLayers().size());
    }

    @Test
    public void testGetLayerCount() {
        assertEquals(1, layerManager.getLayerCount());
        layerManager.addNewLayer();
        assertEquals(2, layerManager.getLayerCount());
    }
}