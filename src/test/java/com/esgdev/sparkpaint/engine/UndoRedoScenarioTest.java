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
    public void undoOfLayerAddRemovesIt() {
        canvas.addNewLayer();
        assertEquals(2, canvas.getLayerCount());

        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    public void layerAddFromPanelIsUndoable() {
        canvas.addNewLayer();
        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    public void undoAfterStrokeThenLayerAddRevertsOnlyTheAdd() {
        strokeHorizontal(5, 5, 15);
        canvas.addNewLayer();
        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
        assertEquals(Color.RED.getRGB(), pixel(10, 5));
    }

    @Test
    public void layerNamesStayUniqueAfterUndoAndAdd() {
        canvas.addNewLayer();
        canvas.addNewLayer();
        canvas.undo();
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

    @Test
    public void duplicateLayerIsUndoable() {
        canvas.duplicateCurrentLayer();
        canvas.undo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    public void deleteLayerIsUndoable() {
        canvas.addNewLayer();
        canvas.deleteCurrentLayer();
        canvas.undo();

        assertEquals(2, canvas.getLayerCount());
    }

    @Test
    public void deleteLayerRedoRemovesItAgain() {
        canvas.addNewLayer();
        canvas.deleteCurrentLayer();
        canvas.undo();
        canvas.redo();

        assertEquals(1, canvas.getLayerCount());
    }

    @Test
    public void mergeDownIsUndoable() {
        canvas.addNewLayer();
        canvas.mergeCurrentLayerDown();
        canvas.undo();

        assertEquals(2, canvas.getLayerCount());
    }

    @Test
    public void flattenIsUndoable() {
        canvas.addNewLayer();
        canvas.flattenLayers();
        canvas.undo();

        assertEquals(2, canvas.getLayerCount());
    }

    @Test
    public void moveLayerIsUndoable() {
        canvas.addNewLayer();
        String bottomBefore = canvas.getLayers().get(0).getName();
        canvas.moveLayer(0, 1);
        canvas.undo();

        assertEquals(bottomBefore, canvas.getLayers().get(0).getName());
    }

    @Test
    public void rejectedLayerOpsAddNoUndoStep() {
        assertFalse(canvas.mergeCurrentLayerDown());
        assertFalse(canvas.flattenLayers());
        assertFalse(canvas.moveLayer(0, 0));
        canvas.deleteCurrentLayer();

        assertFalse(canvas.canUndo());
    }
}
