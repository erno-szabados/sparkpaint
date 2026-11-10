package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.selection.Selection;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class FreeHandSelectionToolTest {

    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private Selection current;
    private FreeHandSelectionTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        Layer layer = new Layer(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));

        when(canvas.getLayers()).thenReturn(Collections.singletonList(layer));
        when(canvas.getFillColor()).thenReturn(Color.WHITE);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(canvas.getSelection()).thenAnswer(inv -> current);
        doAnswer(inv -> { current = inv.getArgument(0); return null; })
                .when(canvas).setSelection(any(Selection.class));
        doAnswer(inv -> { current = null; return null; })
                .when(canvas).clearSelection();

        tool = new FreeHandSelectionTool(canvas);
    }

    private static MouseEvent event(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void freehandPathCreatesSelectionWithPathBounds() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 2, 2));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 12, 2));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 2, 12));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 2, 12));

        assertNotNull(current);
        assertEquals(new Rectangle(2, 2, 10, 10), current.getBounds());
        assertNotNull(current.getContent());
    }

    @Test
    public void tinyFreehandPathClearsSelection() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 5, 5));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 6, 6));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 6, 6));

        verify(canvas).clearSelection();
        assertNull(current);
    }
}
