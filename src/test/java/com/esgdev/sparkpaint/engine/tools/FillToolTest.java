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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class FillToolTest {

    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color RED = Color.RED;

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private FillTool fillTool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                // Vertical black wall at x=5 splits the canvas into two regions
                layer.setRGB(x, y, x == 5 ? BLACK.getRGB() : WHITE.getRGB());
            }
        }
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(RED);
        when(canvas.getFillColor()).thenReturn(RED);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        fillTool = new FillTool(canvas);
        fillTool.setFillMode(FillTool.FillMode.SMART_FILL);
    }

    private static MouseEvent leftPress(int x, int y) {
        return new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0L,
                InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    public void colorDistanceIsZeroForIdenticalColors() {
        assertEquals(0.0, FillTool.colorDistance(RED.getRGB(), RED.getRGB()), 0.0);
    }

    @Test
    public void colorDistanceIsSymmetricAndLargeForContrastingColors() {
        int white = WHITE.getRGB();
        int black = BLACK.getRGB();
        double forward = FillTool.colorDistance(white, black);

        assertEquals(forward, FillTool.colorDistance(black, white), 1e-9);
        assertTrue("black/white should exceed the default epsilon",
                forward > FillTool.DEFAULT_FILL_EPSILON);
    }

    @Test
    public void smartFillStopsAtContrastingBoundary() {
        fillTool.mousePressed(leftPress(2, 2));

        assertEquals(RED.getRGB(), layer.getRGB(2, 2));
        assertEquals(RED.getRGB(), layer.getRGB(0, 9));
        // Opposite side of the wall keeps its original color
        assertEquals(WHITE.getRGB(), layer.getRGB(8, 2));
        verify(canvas).saveToUndoStack();
    }
}
