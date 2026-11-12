package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.ToolManager;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirtyStateTest {

    private static final int LEFT = MouseEvent.BUTTON1_DOWN_MASK;
    private DrawingCanvas canvas;
    private final JPanel src = new JPanel();
    private final List<Boolean> notifications = new ArrayList<>();

    @Before
    public void setUp() {
        canvas = DrawingCanvas.create();
        canvas.createNewCanvas(40, 40, Color.WHITE);
        canvas.setCurrentTool(ToolManager.Tool.PENCIL);
        canvas.setDrawingColor(Color.RED);
        canvas.addDirtyStateListener(notifications::add);
    }

    private MouseEvent ev(int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private void strokeHorizontal(int y, int fromX, int toX) {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, fromX, y));
        canvas.getActiveTool().mouseDragged(ev(MouseEvent.MOUSE_DRAGGED, toX, y));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, toX, y));
    }

    @Test
    public void newCanvasIsClean() {
        assertFalse(canvas.isDirty());
    }

    @Test
    public void editMarksDirtyAndNotifiesListener() {
        strokeHorizontal(5, 5, 15);

        assertTrue(canvas.isDirty());
        assertEquals(1, notifications.size());
        assertEquals(Boolean.TRUE, notifications.get(0));
    }

    @Test
    public void markCleanAfterEditClearsDirty() {
        strokeHorizontal(5, 5, 15);
        canvas.markClean();

        assertFalse(canvas.isDirty());
        assertEquals(Boolean.FALSE, notifications.get(notifications.size() - 1));
    }

    @Test
    public void undoAfterSaveStaysDirty() {
        strokeHorizontal(5, 5, 15);
        canvas.markClean();

        canvas.undo();

        assertTrue(canvas.isDirty());
    }

    @Test
    public void createNewCanvasResetsDirty() {
        strokeHorizontal(5, 5, 15);

        canvas.createNewCanvas(40, 40, Color.WHITE);

        assertFalse(canvas.isDirty());
    }
}
