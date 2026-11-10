package com.esgdev.sparkpaint.ui.layer;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import static org.junit.Assert.assertNotEquals;

public class LayerPanelKeyBindingTest {

    @Test
    public void ctrlAIsNotConsumedByLayerList() throws Exception {
        DrawingCanvas canvas = DrawingCanvas.create();
        canvas.createNewCanvas(10, 10, Color.WHITE);
        LayerPanel panel = new LayerPanel(canvas, message -> { });

        Field field = LayerPanel.class.getDeclaredField("layerList");
        field.setAccessible(true);
        JList<?> layerList = (JList<?>) field.get(panel);

        KeyStroke ctrlA = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
        Object action = layerList.getInputMap(JComponent.WHEN_FOCUSED).get(ctrlA);

        assertNotEquals("selectAll", action);
    }
}
