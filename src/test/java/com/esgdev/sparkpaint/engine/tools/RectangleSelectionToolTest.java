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

public class RectangleSelectionToolTest {

    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private Layer layer;
    private Selection current;
    private RectangleSelectionTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new Layer(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));
        fill(layer.getImage(), Color.RED.getRGB());

        when(canvas.getLayers()).thenReturn(Collections.singletonList(layer));
        when(canvas.getCurrentLayerImage()).thenReturn(layer.getImage());
        when(canvas.getFillColor()).thenReturn(Color.WHITE);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Stateful selection so the tool sees what it set
        when(canvas.getSelection()).thenAnswer(inv -> current);
        doAnswer(inv -> { current = inv.getArgument(0); return null; })
                .when(canvas).setSelection(any(Selection.class));
        doAnswer(inv -> { current = null; return null; })
                .when(canvas).clearSelection();

        tool = new RectangleSelectionTool(canvas);
    }

    private static void fill(BufferedImage img, int rgb) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static MouseEvent event(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void dragCreatesSelectionWithDraggedBounds() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 2, 2));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 10, 8));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 10, 8));

        assertNotNull(current);
        assertEquals(new Rectangle(2, 2, 8, 6), current.getBounds());
        assertNotNull(current.getContent());
        assertEquals(8, current.getContent().getWidth());
        assertEquals(6, current.getContent().getHeight());
    }

    @Test
    public void tinyDragClearsSelection() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 5, 5));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 6, 6));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 6, 6));

        verify(canvas).clearSelection();
        assertNull(current);
    }

    @Test
    public void pressOutsideActiveSelectionCommitsContentToLayer() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 2, 2));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 8, 8));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 8, 8));
        current.setActive(true);

        // Lift the selection off the layer, then press outside it to drop it back
        fill(layer.getImage(), 0);
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 20, 20));

        assertEquals(Color.RED.getRGB(), layer.getImage().getRGB(4, 4));
        verify(canvas).saveToUndoStack();
        verify(canvas).clearSelection();
    }
}
