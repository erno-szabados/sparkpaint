package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

public interface ToolChangeListener {
    void onToolChanged(DrawingCanvas.Tool newTool);
}