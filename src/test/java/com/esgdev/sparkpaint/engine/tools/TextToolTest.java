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

public class TextToolTest {

    private static final int RED_RGB = Color.RED.getRGB();

    private DrawingCanvas canvas;
    private BufferedImage layer;
    private BufferedImage toolCanvas;
    private TextTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layer = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
        toolCanvas = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
        when(canvas.getCurrentLayerImage()).thenReturn(layer);
        when(canvas.getToolCanvas()).thenReturn(toolCanvas);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingColor()).thenReturn(Color.RED);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new TextTool(canvas);
        tool.setText("MMM");
    }

    private static MouseEvent event(JPanel src, int id, int x, int y) {
        return new MouseEvent(src, id, 0L, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private static int countPixels(BufferedImage img, int rgb) {
        int count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) == rgb) count++;
            }
        }
        return count;
    }

    private static int countVisible(BufferedImage img) {
        int count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }

    @Test
    public void releaseCommitsTextToLayerAndSavesUndo() {
        JPanel src = new JPanel();
        tool.mousePressed(event(src, MouseEvent.MOUSE_PRESSED, 10, 40));
        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 10, 40));

        assertTrue("text should be committed in the drawing color", countPixels(layer, RED_RGB) > 0);
        verify(canvas).saveToUndoStack();
    }

    @Test
    public void releaseClearsPreview() {
        JPanel src = new JPanel();
        tool.mouseMoved(event(src, MouseEvent.MOUSE_MOVED, 10, 40));
        assertTrue("hovering should draw a preview", countVisible(toolCanvas) > 0);

        tool.mouseReleased(event(src, MouseEvent.MOUSE_RELEASED, 10, 40));

        assertEquals(0, countVisible(toolCanvas));
    }
}
