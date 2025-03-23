package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.tools.ToolManager;

public interface ToolChangeListener {
    void onToolChanged(ToolManager.Tool newTool);
}