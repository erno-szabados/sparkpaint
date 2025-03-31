package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.ui.ToolChangeListener;

import java.awt.image.BufferedImage;

public interface ToolManagement {
    void setToolCanvas(BufferedImage toolCanvas);

    BufferedImage getToolCanvas();

    void addToolChangeListener(ToolChangeListener listener);

    Tool getCurrentTool();

    DrawingTool getActiveTool();

    DrawingTool getTool(Tool tool);

    void setCurrentTool(Tool tool);

    void initTools();

    boolean isShowBrushCursor();

    public enum Tool {
        BRUSH,
        PENCIL,
        LINE,
        RECTANGLE,
        CIRCLE,
        RECTANGLE_SELECTION,
        FREEHAND_SELECTION,
        FILL,
        TEXT,
        EYEDROPPER
    }
}
