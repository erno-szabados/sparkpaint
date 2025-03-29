package com.esgdev.sparkpaint.engine.selection;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.tools.ToolManager;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SelectionManagerTest {

    private SelectionManager selectionManager;
    private DrawingCanvas mockCanvas;
    private Selection mockSelection;

    @Before
    public void setUp() {
        mockCanvas = mock(DrawingCanvas.class);
        selectionManager = new SelectionManager(mockCanvas);
        mockSelection = mock(Selection.class);
    }

    @Test
    public void testInitialState() {
        assertNull("Selection should be null initially", selectionManager.getSelection());
    }

    @Test
    public void testSetSelection() {
        selectionManager.setSelection(mockSelection);
        assertEquals("Selection should be set", mockSelection, selectionManager.getSelection());
    }

    @Test
    public void testClearSelection() {
        selectionManager.setSelection(mockSelection);
        selectionManager.clearSelection();

        verify(mockSelection).clear();
        verify(mockCanvas).repaint();
    }

    @Test
    public void testClearSelectionWhenNull() {
        // Should not throw exception
        selectionManager.clearSelection();
    }

    @Test
    public void testSelectAll() {
        // Setup mocks
        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Layer mockLayer = new Layer(mockImage);
        List<Layer> layers = new ArrayList<>();
        layers.add(mockLayer);

        // Mock the canvas methods directly instead of accessing layer manager
        when(mockCanvas.getCurrentLayerImage()).thenReturn(mockImage);
        when(mockCanvas.getLayers()).thenReturn(layers);

        // Execute
        selectionManager.selectAll();

        // Verify
        verify(mockCanvas).setCurrentTool(ToolManager.Tool.RECTANGLE_SELECTION);
        verify(mockCanvas).notifyClipboardStateChanged();
        verify(mockCanvas).repaint();

        // Check selection was created
        assertNotNull(selectionManager.getSelection());
        assertEquals(0, selectionManager.getSelection().getBounds().x);
        assertEquals(0, selectionManager.getSelection().getBounds().y);
        assertEquals(100, selectionManager.getSelection().getBounds().width);
        assertEquals(100, selectionManager.getSelection().getBounds().height);
    }

    @Test
    public void testSelectAllWithNoCurrentLayer() {
        // Mock the canvas directly
        when(mockCanvas.getCurrentLayerImage()).thenReturn(null);

        selectionManager.selectAll();

        // No selection should be created
        assertNull(selectionManager.getSelection());
    }

    @Test
    public void testDeleteSelectionAreaFromCurrentLayer() {
        // Setup
        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Rectangle bounds = new Rectangle(10, 10, 50, 50);
        GeneralPath path = new GeneralPath(bounds);

        // Mock the canvas directly
        when(mockCanvas.getCurrentLayerImage()).thenReturn(mockImage);

        when(mockSelection.getBounds()).thenReturn(bounds);
        when(mockSelection.getPath()).thenReturn(path);

        selectionManager.setSelection(mockSelection);

        // Execute
        selectionManager.deleteSelectionAreaFromCurrentLayer();

        // Verify
        verify(mockCanvas).saveToUndoStack();
        verify(mockSelection).clear();
        verify(mockCanvas).notifyClipboardStateChanged();
        verify(mockCanvas).repaint();
    }

    @Test
    public void testGetDrawingGraphicsNoSelection() {
        // Setup
        Graphics2D mockGraphics = mock(Graphics2D.class);

        // Create a test image that returns our mock graphics
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB) {
            @Override
            public Graphics getGraphics() {
                return mockGraphics;
            }
        };

        when(mockCanvas.getCurrentLayerImage()).thenReturn(testImage);

        // Execute
        Graphics2D result = selectionManager.getDrawingGraphics(mockCanvas);

        // Verify
        assertEquals(mockGraphics, result);
    }

    @Test
    public void testGetDrawingGraphicsWithSelection() {
        // Create real objects for this test
        BufferedImage contentImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Rectangle bounds = new Rectangle(10, 10, 50, 50);
        GeneralPath path = new GeneralPath();
        path.append(new Rectangle(10, 10, 50, 50), false);

        // Setup mocks
        when(mockSelection.getContent()).thenReturn(contentImage);
        when(mockSelection.getBounds()).thenReturn(bounds);
        when(mockSelection.getPath()).thenReturn(path);
        when(mockSelection.hasOutline()).thenReturn(true);

        selectionManager.setSelection(mockSelection);

        // Execute
        Graphics2D result = selectionManager.getDrawingGraphics(mockCanvas);

        // Verify
        assertNotNull("Graphics context should not be null", result);
        assertNotNull("Clip should be set on graphics context", result.getClip());

        // Clean up
        result.dispose();
    }

    @Test
    public void testDeleteSelectionAreaFromCurrentLayerWhenNull() {
        selectionManager.deleteSelectionAreaFromCurrentLayer(); // Should not throw
        verify(mockCanvas, never()).saveToUndoStack();
    }

    @Test
    public void testRotateSelection() {
        // Setup
        BufferedImage content = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        when(mockSelection.getContent()).thenReturn(content);
        selectionManager.setSelection(mockSelection);

        // Execute
        selectionManager.rotateSelection(90);

        // Verify
        verify(mockSelection).rotate(90);
        verify(mockCanvas).repaint();
    }

    @Test
    public void testRotateSelectionWhenNull() {
        selectionManager.rotateSelection(90); // Should not throw
        verify(mockCanvas, never()).repaint();
    }

    @Test
    public void testFlipSelectionHorizontal() {
        // Setup
        BufferedImage content = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        when(mockSelection.getContent()).thenReturn(content);
        selectionManager.setSelection(mockSelection);

        // Execute
        selectionManager.flipSelection(true);

        // Verify
        verify(mockSelection).flipHorizontal();
        verify(mockCanvas).repaint();
    }

    @Test
    public void testFlipSelectionVertical() {
        // Setup
        BufferedImage content = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        when(mockSelection.getContent()).thenReturn(content);
        selectionManager.setSelection(mockSelection);

        // Execute
        selectionManager.flipSelection(false);

        // Verify
        verify(mockSelection).flipVertical();
        verify(mockCanvas).repaint();
    }

    @Test
    public void testGetDrawingCoordinatesNoSelection() {
        Point screenPoint = new Point(100, 100);
        float zoomFactor = 1.0f;

        Point result = selectionManager.getDrawingCoordinates(screenPoint, zoomFactor);

        assertEquals(screenPoint, result);
    }

    @Test
    public void testGetDrawingCoordinatesWithSelection() {
        // Setup
        Point screenPoint = new Point(100, 100);
        float zoomFactor = 1.0f;
        Rectangle bounds = new Rectangle(50, 50, 100, 100);

        when(mockSelection.contains(any(Point.class))).thenReturn(true);
        when(mockSelection.getBounds()).thenReturn(bounds);
        selectionManager.setSelection(mockSelection);

        // Execute
        Point result = selectionManager.getDrawingCoordinates(screenPoint, zoomFactor);

        // Verify - should translate to selection coordinates
        assertEquals(new Point(50, 50), result);
    }

    @Test
    public void testIsWithinSelectionTrue() {
        // Setup
        Point worldPoint = new Point(75, 75);

        when(mockSelection.contains(worldPoint)).thenReturn(true);
        when(mockSelection.hasOutline()).thenReturn(true);

        selectionManager.setSelection(mockSelection);

        // Execute & verify
        assertTrue(selectionManager.isWithinSelection(worldPoint));
    }

    @Test
    public void testIsWithinSelectionFalse() {
        // Setup
        Point worldPoint = new Point(25, 25); // Outside bounds

        when(mockSelection.contains(any(Point.class))).thenReturn(false);
        when(mockSelection.hasOutline()).thenReturn(true);

        selectionManager.setSelection(mockSelection);

        // Execute & verify
        assertFalse(selectionManager.isWithinSelection(worldPoint));
    }

    @Test
    public void testIsWithinSelectionWithNoSelection() {
        Point worldPoint = new Point(50, 50);
        assertFalse(selectionManager.isWithinSelection(worldPoint));
    }
}