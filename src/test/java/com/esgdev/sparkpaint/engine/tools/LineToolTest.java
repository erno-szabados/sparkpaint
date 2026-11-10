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

public class LineToolTest {

    private static final int RED_RGB = Color.RED.getRGB();
    private static final int LEFT = InputEvent.BUTTON1_DOWN_MASK;

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private LineTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getToolCanvas()).thenReturn(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getLineThickness()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(Color.RED);
        when(canvas.getFillColor()).thenReturn(Color.RED);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new LineTool(canvas);
    }

    private static MouseEvent leftEvent(int id, int x, int y) {
        return new MouseEvent(new JPanel(), id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private static MouseEvent rightClick(int x, int y) {
        return new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0L,
                InputEvent.BUTTON3_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON3);
    }

    @Test
    public void singleLineDragCommitsStraightLineToLayer() {
        tool.setMode(LineTool.LineMode.SINGLE_LINE);
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 3, 15));
        tool.mouseReleased(leftEvent(MouseEvent.MOUSE_RELEASED, 25, 15));

        assertEquals(RED_RGB, layer.getRGB(14, 15));
        assertEquals(0, layer.getRGB(14, 5));
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void polylineFinalizesOnRightClickAndPaintsAllSegments() {
        tool.setMode(LineTool.LineMode.POLYLINE);
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 2, 2));
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 20, 2));
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 20, 20));
        tool.mousePressed(rightClick(20, 20));

        // Second segment, drawn vertically
        assertEquals(RED_RGB, layer.getRGB(20, 10));
        assertTrue("points should be cleared after finalizing", tool.getControlPoints().isEmpty());
    }

    @Test
    public void rightClickWithTooFewPointsCancelsWithoutPainting() {
        tool.setMode(LineTool.LineMode.POLYLINE);
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 2, 2));
        tool.mousePressed(rightClick(2, 2));

        assertTrue(tool.getControlPoints().isEmpty());
        assertEquals(0, layer.getRGB(2, 2));
    }

    @Test
    public void closedCurveNeedsThreePointsToFinalize() {
        tool.setMode(LineTool.LineMode.CLOSED_CURVE);
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 5, 5));
        tool.mousePressed(leftEvent(MouseEvent.MOUSE_PRESSED, 20, 5));
        tool.mousePressed(rightClick(20, 5));

        // Two points is below the minimum for a closed curve: cancelled
        assertTrue(tool.getControlPoints().isEmpty());
    }
}
