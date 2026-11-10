package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class RectangleToolTest {

    private static final int RED_RGB = Color.RED.getRGB();
    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private BufferedImage toolCanvas;
    private RectangleTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        toolCanvas = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getToolCanvas()).thenReturn(toolCanvas);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getLineThickness()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(Color.RED);
        when(canvas.getFillColor()).thenReturn(Color.RED);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new RectangleTool(canvas);
    }

    private static MouseEvent event(JPanel src, int id, int x, int y, int modifiers) {
        return new MouseEvent(src, id, 0L, modifiers, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void dragAndReleaseCommitsRectangleToLayer() {
        tool.setFilled(true);
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 2, 2, LEFT));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 10, 10, LEFT));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 10, 10, LEFT));

        // Filled rectangle from (2,2) to (10,10): interior is committed
        assertEquals(RED_RGB, layer.getRGB(6, 6));
        assertEquals(0, layer.getRGB(15, 15));
        verify(canvas).saveToUndoStack();
        verify(canvas).setToolCanvas(null);
    }

    @Test
    public void shiftDragForcesSquare() {
        tool.setFilled(true);
        JPanel src = new JPanel();
        int shift = LEFT | InputEvent.SHIFT_DOWN_MASK;
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 2, 2, LEFT));
        // Drag to (14, 6): unconstrained height is 4, shift forces 12x12
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 14, 6, shift));

        assertEquals("square extends below the drag point", RED_RGB, layer.getRGB(10, 12));
    }

    @Test
    public void releaseWithoutPressDoesNothing() {
        tool.mouseReleased(event(new JPanel(), MouseEvent.MOUSE_RELEASED, 5, 5, LEFT));

        verify(canvas, never()).saveToUndoStack();
        verify(canvas, never()).setToolCanvas(null);
        assertEquals(0, layer.getRGB(5, 5));
    }
}
