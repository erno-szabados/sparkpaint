package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.ToolManager;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SelectAllLayerSwitchTest {

    private static final int LEFT = MouseEvent.BUTTON1_DOWN_MASK;
    private DrawingCanvas canvas;
    private final JPanel src = new JPanel();

    @Before
    public void setUp() {
        canvas = DrawingCanvas.create();
        canvas.createNewCanvas(100, 80, Color.WHITE);
    }

    private MouseEvent ev(int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private void stroke(int fromX, int fromY, int toX, int toY) {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, fromX, fromY));
        canvas.getActiveTool().mouseDragged(ev(MouseEvent.MOUSE_DRAGGED, toX, toY));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, toX, toY));
    }

    private void assertSelectAllShowsOutline(Selection selection) {
        assertNotNull("selection should exist", selection);
        assertTrue("select-all should have an outline", selection.hasOutline());
        Rectangle bounds = selection.getBounds();
        assertEquals(100, bounds.width);
        assertEquals(80, bounds.height);
    }

    @Test
    public void selectAllAfterClickClearOnOtherLayerShowsOutline() {
        canvas.addNewLayer();
        canvas.setCurrentTool(ToolManager.Tool.PENCIL);
        canvas.setDrawingColor(Color.RED);
        stroke(20, 20, 30, 20);

        canvas.setCurrentLayerIndex(0);
        canvas.setDrawingColor(Color.BLUE);
        stroke(60, 40, 70, 40);

        canvas.setCurrentTool(ToolManager.Tool.RECTANGLE_SELECTION);
        canvas.selectAll();
        assertSelectAllShowsOutline(canvas.getSelection());

        // Click inside the select-all to clear it
        stroke(50, 40, 50, 40);
        canvas.setCurrentLayerIndex(1);

        canvas.selectAll();
        assertSelectAllShowsOutline(canvas.getSelection());
    }

    @Test
    public void selectAllAfterLayerSwitchWithoutClickShowsOutline() {
        canvas.addNewLayer();
        canvas.setCurrentLayerIndex(0);
        canvas.setCurrentTool(ToolManager.Tool.RECTANGLE_SELECTION);
        canvas.selectAll();

        canvas.setCurrentLayerIndex(1);
        canvas.selectAll();
        assertSelectAllShowsOutline(canvas.getSelection());
    }
}
