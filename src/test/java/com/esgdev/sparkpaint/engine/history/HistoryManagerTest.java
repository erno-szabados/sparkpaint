package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HistoryManagerTest {

    private HistoryManager historyManager;
    private List<Layer> layers;
    private UndoRedoChangeListener listener;

    @Before
    public void setUp() {
        historyManager = new HistoryManager();
        layers = new ArrayList<>();

        // Create test layers
        Layer layer1 = createTestLayer(100, 100, "Layer 1");
        Layer layer2 = createTestLayer(100, 100, "Layer 2");
        layers.add(layer1);
        layers.add(layer2);

        // Set up mock listener
        listener = mock(UndoRedoChangeListener.class);
        historyManager.addUndoRedoChangeListener(listener);
    }

    @Test
    public void initialStateHasNoUndoRedo() {
        assertFalse(historyManager.canUndo());
        assertFalse(historyManager.canRedo());
    }

    @Test
    public void saveToUndoStackEnablesUndo() {
        historyManager.saveToUndoStack(layers, 0);
        assertTrue(historyManager.canUndo());
        assertFalse(historyManager.canRedo());
        verify(listener).undoRedoStateChanged(true, false);
    }

    @Test
    public void undoEnablesRedo() {
        historyManager.saveToUndoStack(layers, 0);
        // Modify layers to simulate a change
        layers.get(0).setName("Modified Layer");

        LayerState result = historyManager.undo(layers, 0);

        assertTrue(historyManager.canRedo());
        assertFalse(historyManager.canUndo());
        assertEquals("Layer 1", result.getLayers().get(0).getName());
        verify(listener, times(2)).undoRedoStateChanged(anyBoolean(), anyBoolean());

        // Verify the last call had canUndo=false, canRedo=true
        ArgumentCaptor<Boolean> undoCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> redoCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(listener, atLeastOnce()).undoRedoStateChanged(undoCaptor.capture(), redoCaptor.capture());
        assertEquals(false, undoCaptor.getValue());
        assertEquals(true, redoCaptor.getValue());
    }

    /**
     * Test that undo restores the previous state of the layers
     */
   @Test
    public void redoRestoresFutureState() {
        // Create clone of layers to preserve initial state
        List<Layer> initialLayers = new ArrayList<>();
        initialLayers.add(createTestLayer(100, 100, "Layer 1"));
        initialLayers.add(createTestLayer(100, 100, "Layer 2"));

        // Save initial state
        historyManager.saveToUndoStack(initialLayers, 0);

        // Modify and save second state
        List<Layer> modifiedLayers = new ArrayList<>();
        modifiedLayers.add(createTestLayer(100, 100, "Modified Layer"));
        modifiedLayers.add(createTestLayer(100, 100, "Layer 2"));
        historyManager.saveToUndoStack(modifiedLayers, 0);

        // Undo to first state
        LayerState undoState = historyManager.undo(modifiedLayers, 0);
        assertEquals("Modified Layer", undoState.getLayers().get(0).getName());

        // Redo to second state
        LayerState redoState = historyManager.redo(undoState.getLayers(), undoState.getCurrentLayerIndex());
        assertEquals("Modified Layer", redoState.getLayers().get(0).getName());
    }

    @Test
    public void clearHistoryResetsUndoRedoState() {
        historyManager.saveToUndoStack(layers, 0);
        historyManager.undo(layers, 0);

        reset(listener);
        historyManager.clearHistory();

        assertFalse(historyManager.canUndo());
        assertFalse(historyManager.canRedo());
        verify(listener).undoRedoStateChanged(false, false);
    }

    @Test
    public void historyMaintainsMaxSize() {
        // Fill the history beyond its capacity (MAX_HISTORY_SIZE = 16)
        for (int i = 0; i < 20; i++) {
            layers.get(0).setName("State " + i);
            historyManager.saveToUndoStack(layers, 0);
        }

        // We should have 16 states in the stack
        int undoCount = 0;
        while (historyManager.canUndo()) {
            historyManager.undo(layers, 0);
            undoCount++;
        }

        assertEquals("Should have maintained maximum of 16 history states", 16, undoCount);
    }

    @Test
    public void compressDecompressImagePreservesContent() {
        // Create a test image with specific content
        BufferedImage original = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = original.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(10, 10, 80, 80);
        g2d.dispose();

        // Compress and decompress
        byte[] compressed = HistoryManager.compressImage(original);
        BufferedImage decompressed = HistoryManager.decompressImage(compressed, 100, 100);

        // Check a sample pixel
        assertEquals("Pixel color should be preserved after compression/decompression",
                original.getRGB(50, 50), decompressed.getRGB(50, 50));
    }

    @Test
    public void emptyLayerListDoesNotChangeHistory() {
        // Initial state
        assertFalse(historyManager.canUndo());

        // Try to save empty list
        historyManager.saveToUndoStack(new ArrayList<>(), 0);

        // Should not change
        assertFalse(historyManager.canUndo());
    }

    @Test
    public void undoWithEmptyStackReturnsCurrentState() {
        LayerState result = historyManager.undo(layers, 0);

        assertEquals(layers.size(), result.getLayers().size());
        assertEquals(0, result.getCurrentLayerIndex());
    }

    @Test
    public void redoWithEmptyStackReturnsCurrentState() {
        LayerState result = historyManager.redo(layers, 0);

        assertEquals(layers.size(), result.getLayers().size());
        assertEquals(0, result.getCurrentLayerIndex());
    }

    @Test
    public void multipleUndoRedoCyclesWorkCorrectly() {
        // Initial state - capture name first
        String initialName = "State 1";
        layers.get(0).setName(initialName);
        historyManager.saveToUndoStack(layers, 0);

        // Create and save second state
        String secondStateName = "State 2";
        layers.get(0).setName(secondStateName);
        historyManager.saveToUndoStack(layers, 0);

        // Create and save third state
        String thirdStateName = "State 3";
        layers.get(0).setName(thirdStateName);
        historyManager.saveToUndoStack(layers, 0);

        // Undo to third state
        LayerState state = historyManager.undo(layers, 0);
        assertEquals(thirdStateName, state.getLayers().get(0).getName());

        // Undo to second state
        state = historyManager.undo(state.getLayers(), state.getCurrentLayerIndex());
        assertEquals(secondStateName, state.getLayers().get(0).getName());

        // Redo to second state
        state = historyManager.redo(state.getLayers(), state.getCurrentLayerIndex());
        assertEquals(thirdStateName, state.getLayers().get(0).getName());

        // Redo to third state
        state = historyManager.redo(state.getLayers(), state.getCurrentLayerIndex());
        assertEquals(thirdStateName, state.getLayers().get(0).getName());
    }

    /**
     * Helper method to create a test layer with simple content
     */
    private Layer createTestLayer(int width, int height, String name) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        Layer layer = new Layer(image);
        layer.setName(name);
        return layer;
    }
}