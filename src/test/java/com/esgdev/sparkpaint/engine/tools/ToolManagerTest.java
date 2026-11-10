package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.ToolChangeListener;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ToolManagerTest {

    private DrawingCanvas canvas;
    private ToolManager toolManager;

    @Before
    public void setUp() {
        canvas = mock(DrawingCanvas.class);
        when(canvas.getCurrentLayerImage()).thenReturn(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB));
        toolManager = new ToolManager(canvas);
        toolManager.initTools();
    }

    @Test
    public void setCurrentToolSwitchesActiveToolAndNotifiesListeners() {
        ToolChangeListener listener = mock(ToolChangeListener.class);
        toolManager.addToolChangeListener(listener);

        toolManager.setCurrentTool(ToolManager.Tool.FILL);

        assertEquals(ToolManager.Tool.FILL, toolManager.getCurrentTool());
        assertSame(toolManager.getTool(ToolManager.Tool.FILL), toolManager.getActiveTool());
        verify(listener).onToolChanged(ToolManager.Tool.FILL);
    }

    @Test
    public void brushCursorShownOnlyForBrushTools() {
        toolManager.setCurrentTool(ToolManager.Tool.BRUSH);
        assertTrue(toolManager.isShowBrushCursor());

        toolManager.setCurrentTool(ToolManager.Tool.FILTER_BRUSH);
        assertTrue(toolManager.isShowBrushCursor());

        toolManager.setCurrentTool(ToolManager.Tool.PENCIL);
        assertFalse(toolManager.isShowBrushCursor());
    }

    @Test
    public void toolCanvasIsLazilyCreatedAtLayerSize() {
        BufferedImage toolCanvas = toolManager.getToolCanvas();

        assertNotNull(toolCanvas);
        assertEquals(40, toolCanvas.getWidth());
        assertEquals(30, toolCanvas.getHeight());
        assertSame(toolCanvas, toolManager.getToolCanvas());
    }
}
