package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class BrushToolTest {

    private static final int RED_RGB = Color.RED.getRGB();
    private static final int BLUE_RGB = Color.BLUE.getRGB();

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private BrushTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getToolCanvas()).thenReturn(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(Color.RED);
        when(canvas.getFillColor()).thenReturn(Color.BLUE);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new BrushTool(canvas);
        // Full-strength, deterministic square brush: no randomness, fully opaque pixels
        tool.setShape(BrushTool.BrushShape.SQUARE);
        tool.setSize(4);
        tool.setMaxBlendStrength(1f);
    }

    private static MouseEvent leftEvent(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private static MouseEvent rightEvent(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, InputEvent.BUTTON3_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON3);
    }

    @Test
    public void leftPressPaintsDrawingColorAtPoint() {
        JPanel src = new JPanel();
        tool.mousePressed(leftEvent(src, MouseEvent.MOUSE_PRESSED, 10, 10));

        assertEquals(RED_RGB, layer.getRGB(10, 10));
        assertEquals(0, layer.getRGB(0, 0));
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void rightPressPaintsFillColor() {
        JPanel src = new JPanel();
        tool.mousePressed(rightEvent(src, MouseEvent.MOUSE_PRESSED, 10, 10));

        assertEquals(BLUE_RGB, layer.getRGB(10, 10));
    }

    @Test
    public void dragPaintsAtCurrentPoint() {
        JPanel src = new JPanel();
        tool.mousePressed(leftEvent(src, MouseEvent.MOUSE_PRESSED, 4, 10));
        tool.mouseDragged(leftEvent(src, MouseEvent.MOUSE_DRAGGED, 20, 10));

        // Drag paints at the current pointer position
        assertEquals(RED_RGB, layer.getRGB(20, 10));
    }

    @Test
    public void pressOutsideSelectionDoesNothing() {
        // Selection with an outline that does not contain the press point
        Selection selection = mock(Selection.class);
        when(selection.hasOutline()).thenReturn(true);
        when(selection.contains(any(Point.class))).thenReturn(false);
        when(canvas.getSelection()).thenReturn(selection);

        tool.mousePressed(leftEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 10, 10));

        verify(canvas, never()).saveToUndoStack();
        assertEquals(0, layer.getRGB(10, 10));
    }
}
