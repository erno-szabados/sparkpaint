package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.ToolManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UndoRedoScenarioTest {

    private static final int LEFT = MouseEvent.BUTTON1_DOWN_MASK;
    private DrawingCanvas canvas;
    private final JPanel src = new JPanel();

    @Before
    public void setUp() {
        canvas = DrawingCanvas.create();
        canvas.createNewCanvas(40, 40, Color.WHITE);
        canvas.setCurrentTool(ToolManager.Tool.PENCIL);
        canvas.setDrawingColor(Color.RED);
    }

    private MouseEvent ev(int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private void strokeHorizontal(int y, int fromX, int toX) {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, fromX, y));
        canvas.getActiveTool().mouseDragged(ev(MouseEvent.MOUSE_DRAGGED, toX, y));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, toX, y));
    }

    private int pixel(int x, int y) {
        return canvas.getCurrentLayerImage().getRGB(x, y);
    }

    @Test
    public void strokeIsRestoredByUndo() {
        strokeHorizontal(5, 5, 15);
        assertEquals(Color.RED.getRGB(), pixel(10, 5));

        canvas.undo();

        assertEquals(Color.WHITE.getRGB(), pixel(10, 5));
    }

    @Test
    public void strokeIsRestoredByRedoAfterUndo() {
        strokeHorizontal(5, 5, 15);
        canvas.undo();
        canvas.redo();

        assertEquals(Color.RED.getRGB(), pixel(10, 5));
    }

    @Test
    public void newEditAfterUndoClearsRedo() {
        strokeHorizontal(5, 5, 15);
        canvas.undo();
        strokeHorizontal(20, 5, 15);

        assertFalse(canvas.canRedo());
    }

    @Test
    public void undoOfSavedLayerAddRemovesIt() {
        canvas.saveToUndoStack();
        canvas.addNewLayer();
        assertEquals(2, canvas.getLayerCount());

        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    @Ignore("Known bug: LayerPanel.addLayer calls addNewLayer without saving an undo state")
    public void layerAddFromPanelIsUndoable() {
        canvas.addNewLayer();
        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    @Ignore("Known bug: undo rolls back unrecorded layer changes made after the last snapshot")
    public void undoAfterUnrecordedLayerAddKeepsThatLayer() {
        strokeHorizontal(5, 5, 15);
        canvas.addNewLayer();
        canvas.undo();

        assertEquals(2, canvas.getLayerCount());
        assertEquals(Color.WHITE.getRGB(), pixel(10, 5));
    }

    @Test
    public void layerNamesStayUniqueAfterUndoAndAdd() {
        canvas.saveToUndoStack();
        canvas.addNewLayer();
        canvas.saveToUndoStack();
        canvas.addNewLayer();
        canvas.undo();
        canvas.saveToUndoStack();
        canvas.addNewLayer();

        Set<String> names = new HashSet<>();
        for (int i = 0; i < canvas.getLayerCount(); i++) {
            names.add(canvas.getLayers().get(i).getName());
        }
        assertEquals(canvas.getLayerCount(), names.size());
    }

    @Test
    public void strokeAfterUndoOfAddedLayerDrawsOnCurrentLayer() {
        canvas.addNewLayer();
        canvas.undo();
        strokeHorizontal(8, 5, 15);

        assertEquals(Color.RED.getRGB(), pixel(10, 8));
    }

    @Test
    public void historyIsLimitedTo16Steps() {
        for (int i = 0; i < 20; i++) {
            strokeHorizontal(i, 0, 1);
        }
        int undone = 0;
        while (canvas.canUndo()) {
            canvas.undo();
            undone++;
        }
        assertEquals(16, undone);
    }

    @Test
    @Ignore("Known behavior: pencil press saves an undo step even when nothing is painted")
    public void noOpPressCreatesUndoStep() {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, 5, 5));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, 5, 5));

        assertTrue("a click with no change should not add an undo step", !canvas.canUndo());
    }
}
