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

public class EllipseToolTest {

    private static final int RED_RGB = Color.RED.getRGB();
    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private EllipseTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getToolCanvas()).thenReturn(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getLineThickness()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(Color.RED);
        when(canvas.getFillColor()).thenReturn(Color.RED);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new EllipseTool(canvas);
    }

    private static MouseEvent event(int id, int x, int y, int modifiers) {
        return new MouseEvent(new JPanel(), id, 0L, modifiers, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void filledEllipseCommitsInteriorToLayer() {
        tool.setFilled(true);
        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, 2, 2, LEFT));
        tool.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 18, 18, LEFT));

        assertEquals(RED_RGB, layer.getRGB(10, 10));
        // Corner of the bounding box lies outside the ellipse
        assertEquals(0, layer.getRGB(3, 3));
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void centerBasedDragUsesStartAsCenter() {
        tool.setFilled(true);
        tool.setCenterBased(true);
        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, 10, 10, LEFT));
        // Radius 5 around (10,10) covers (10,6) but not (10,3)
        tool.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 10, 15, LEFT));

        assertEquals(RED_RGB, layer.getRGB(10, 8));
        assertEquals(0, layer.getRGB(10, 3));
    }

    @Test
    public void outlineEllipseLeavesCenterUnpainted() {
        tool.setFilled(false);
        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, 2, 2, LEFT));
        tool.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 18, 18, LEFT));

        assertEquals(0, layer.getRGB(10, 10));
    }

    @Test
    public void releaseWithoutPressDoesNothing() {
        tool.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 5, 5, LEFT));

        verify(canvas, never()).saveToUndoStack();
        assertEquals(0, layer.getRGB(5, 5));
    }
}
