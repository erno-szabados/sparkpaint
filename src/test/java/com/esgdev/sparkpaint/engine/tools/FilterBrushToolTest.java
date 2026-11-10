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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class FilterBrushToolTest {

    private static final int GRAY_RGB = new Color(100, 100, 100).getRGB();

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private FilterBrushTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                layer.setRGB(x, y, GRAY_RGB);
            }
        }
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new FilterBrushTool(canvas);
        tool.setFilterType(FilterBrushTool.FilterType.BRIGHTEN);
        tool.setSize(6);
        tool.setStrength(1f);
    }

    @Test
    public void pressBrightensPixelsUnderBrushAndSavesUndo() {
        JPanel src = new JPanel();
        tool.mousePressed(new MouseEvent(src, MouseEvent.MOUSE_PRESSED, 0L,
                InputEvent.BUTTON1_DOWN_MASK, 15, 15, 1, false, MouseEvent.BUTTON1));

        assertTrue(brightness(layer.getRGB(15, 15)) > brightness(GRAY_RGB));
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void pressOutsideSelectionDoesNothing() {
        Selection selection = mock(Selection.class);
        when(selection.hasOutline()).thenReturn(true);
        when(selection.contains(any(Point.class))).thenReturn(false);
        when(canvas.getSelection()).thenReturn(selection);

        tool.mousePressed(new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0L,
                InputEvent.BUTTON1_DOWN_MASK, 15, 15, 1, false, MouseEvent.BUTTON1));

        verify(canvas, never()).saveToUndoStack();
        assertEquals(GRAY_RGB, layer.getRGB(15, 15));
    }

    private static int brightness(int rgb) {
        Color c = new Color(rgb, true);
        return c.getRed() + c.getGreen() + c.getBlue();
    }
}
