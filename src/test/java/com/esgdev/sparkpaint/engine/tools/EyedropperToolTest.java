package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

public class EyedropperToolTest {

    private DrawingCanvas canvas;
    private List<Layer> layers;
    private EyedropperTool tool;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        layers = new ArrayList<>();
        when(canvas.getLayers()).thenReturn(layers);
        when(canvas.getCurrentLayerIndex()).thenReturn(0);
        when(canvas.getZoomFactor()).thenReturn(1f);
        when(canvas.getDrawingCoordinates(any(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(0));
        tool = new EyedropperTool(canvas);
    }

    private Layer addLayer(Color fill, boolean visible) {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        if (fill != null) {
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    img.setRGB(x, y, fill.getRGB());
                }
            }
        }
        Layer layer = new Layer(img);
        layer.setVisible(visible);
        layers.add(layer);
        return layer;
    }

    private static MouseEvent event(int id, int modifiers, int button) {
        return new MouseEvent(new JPanel(), id, 0L, modifiers, 5, 5, 1, false, button);
    }

    @Test
    public void leftPressPicksColorIntoDrawingColor() {
        addLayer(Color.BLUE, true);

        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1));

        verify(canvas).setDrawingColor(new Color(0, 0, 255));
    }

    @Test
    public void rightPressPicksColorIntoFillColor() {
        addLayer(Color.GREEN, true);

        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON3_DOWN_MASK, MouseEvent.BUTTON3));

        verify(canvas).setFillColor(new Color(0, 255, 0));
    }

    @Test
    public void transparentCurrentLayerFallsBackToVisibleLayerAbove() {
        addLayer(null, true);                  // current layer: transparent at the pick point
        addLayer(Color.ORANGE, false);         // hidden: must be skipped
        addLayer(Color.RED, true);             // visible: top-most match

        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1));

        verify(canvas).setDrawingColor(new Color(255, 0, 0));
        verify(canvas, never()).setDrawingColor(Color.ORANGE);
    }

    @Test
    public void fullyTransparentPickLeavesColorsUnchanged() {
        addLayer(null, true);

        tool.mousePressed(event(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1));

        verify(canvas, never()).setDrawingColor(any());
    }
}
