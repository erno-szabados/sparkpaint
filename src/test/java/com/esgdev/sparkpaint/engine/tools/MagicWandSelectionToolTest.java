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

public class MagicWandSelectionToolTest {

    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private BufferedImage image;
    private Selection current;
    private MagicWandSelectionTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);

        // White background with a solid red square at (5,5) size 10x10
        image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                boolean inSquare = x >= 5 && x < 15 && y >= 5 && y < 15;
                image.setRGB(x, y, inSquare ? Color.RED.getRGB() : Color.WHITE.getRGB());
            }
        }
        Layer layer = new Layer(image);

        when(canvas.getWidth()).thenReturn(30);
        when(canvas.getHeight()).thenReturn(30);
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

        tool = new MagicWandSelectionTool(canvas);
    }

    private static MouseEvent event(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void clickSelectsContiguousRegionOfSameColor() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 8, 8));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 8, 8));

        assertNotNull(current);
        // The contour runs through boundary pixel coordinates (5..14), so path bounds are max - min
        Rectangle bounds = current.getBounds();
        assertEquals(new Rectangle(5, 5, 9, 9), bounds);
    }

    @Test
    public void clickOutsideCanvasCreatesNoSelection() {
        tool.mousePressed(event(new JPanel(), MouseEvent.MOUSE_PRESSED, -1, -1));

        assertNull(current);
        verify(canvas, never()).setSelection(any(Selection.class));
    }

    @Test
    public void dragInsideActiveSelectionMovesIt() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 8, 8));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 8, 8));
        current.setActive(true);

        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 8, 8));
        tool.mouseDragged(event(src, MouseEvent.MOUSE_DRAGGED, 11, 10));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 11, 10));

        Rectangle bounds = current.getBounds();
        assertEquals(8, bounds.x);
        assertEquals(7, bounds.y);
    }
}
