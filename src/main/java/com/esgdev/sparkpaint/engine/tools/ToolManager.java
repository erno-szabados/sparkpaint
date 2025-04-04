package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * ToolManager is responsible for managing the current drawing tool and its associated settings.
 * It allows switching between different tools and notifies listeners when the tool changes.
 */
public class ToolManager implements ToolManagement {
    private final DrawingCanvas drawingCanvas;
    private Tool currentTool;
    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<ToolChangeListener>();
    private final EnumMap<Tool, DrawingTool> tools = new EnumMap<>(Tool.class);
    private boolean showBrushCursor = false;
    // Temporary canvas for drawing previews, tools will manage this, paintComponent will draw it
    public BufferedImage toolCanvas;

    public ToolManager(DrawingCanvas drawingCanvas) {
        this.drawingCanvas = drawingCanvas;
        this.currentTool = Tool.PENCIL;
    }


    @Override
    public boolean isShowBrushCursor() {
        return showBrushCursor;
    }

    @Override
    public void setToolCanvas(BufferedImage toolCanvas) {
        this.toolCanvas = toolCanvas;
    }

    @Override
    public BufferedImage getToolCanvas() {
        if (toolCanvas == null) {
            // Use the dimensions of the current layer
            BufferedImage currentLayer = drawingCanvas.getCurrentLayerImage();
            toolCanvas = new BufferedImage(
                    currentLayer.getWidth(),
                    currentLayer.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
        }
        return toolCanvas;
    }

    @Override
    public void addToolChangeListener(ToolChangeListener listener) {
        toolChangeListeners.add(listener);
    }

    @Override
    public Tool getCurrentTool() {
        return currentTool;
    }

    @Override
    public DrawingTool getActiveTool() {
        return tools.get(currentTool);
    }

    @Override
    public DrawingTool getTool(Tool tool) {
        return tools.get(tool);
    }

    @Override
    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
        getActiveTool().setCursor();
        showBrushCursor = tool == Tool.BRUSH;
        // Notify all listeners
        for (ToolChangeListener listener : toolChangeListeners) {
            listener.onToolChanged(tool);
        }
    }

    @Override
    public void initTools() {
        tools.put(Tool.LINE, new LineTool(drawingCanvas));
        tools.put(Tool.RECTANGLE, new RectangleTool(drawingCanvas));
        tools.put(Tool.CIRCLE, new EllipseTool(drawingCanvas));
        tools.put(Tool.FILL, new FillTool(drawingCanvas));
        tools.put(Tool.EYEDROPPER, new EyedropperTool(drawingCanvas));
        tools.put(Tool.PENCIL, new PencilTool(drawingCanvas));
        tools.put(Tool.BRUSH, new BrushTool(drawingCanvas));
        tools.put(Tool.RECTANGLE_SELECTION, new RectangleSelectionTool(drawingCanvas));
        tools.put(Tool.FREEHAND_SELECTION, new FreeHandSelectionTool(drawingCanvas));
        tools.put(Tool.TEXT, new TextTool(drawingCanvas));
    }

}