package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
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

public class PencilToolTest {

    private static final Color PRIMARY = Color.RED;
    private static final Color SECONDARY = Color.BLUE;

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private PencilTool pencil;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getLineThickness()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(PRIMARY);
        when(canvas.getFillColor()).thenReturn(SECONDARY);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        pencil = new PencilTool(canvas);
    }

    private static MouseEvent press(int x, int y, int button) {
        int mask = button == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        return new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0L, mask, x, y, 1, false, button);
    }

    @Test
    public void leftClickPaintsPrimaryColorOnLayerAndSavesUndo() {
        pencil.mousePressed(press(5, 6, MouseEvent.BUTTON1));

        assertEquals(PRIMARY.getRGB(), layer.getRGB(5, 6));
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void rightClickPaintsSecondaryColor() {
        pencil.mousePressed(press(7, 7, MouseEvent.BUTTON3));

        assertEquals(SECONDARY.getRGB(), layer.getRGB(7, 7));
    }

    @Test
    public void clickOutsideActiveSelectionIsIgnored() {
        Selection selection = mock(Selection.class);
        when(selection.hasOutline()).thenReturn(true);
        when(selection.contains(any())).thenReturn(false);
        when(canvas.getSelection()).thenReturn(selection);

        pencil.mousePressed(press(5, 5, MouseEvent.BUTTON1));

        assertEquals(0, layer.getRGB(5, 5));
        verify(canvas, never()).saveToUndoStack();
    }
}
