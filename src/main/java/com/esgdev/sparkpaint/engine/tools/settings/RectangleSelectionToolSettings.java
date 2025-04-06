package com.esgdev.sparkpaint.engine.tools.settings;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.RectangleSelectionTool;
import com.esgdev.sparkpaint.engine.tools.ToolManager;

import javax.swing.*;

/**
 * Settings panel for the Rectangle Selection Tool.
 */
public class RectangleSelectionToolSettings extends AbstractSelectionToolSettings {

    public RectangleSelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        RectangleSelectionTool rectangleSelectionTool = (RectangleSelectionTool) canvas.getTool(ToolManager.Tool.RECTANGLE_SELECTION);
        configureTransparencyCheckbox(rectangleSelectionTool::setTransparencyEnabled);
    }

    @Override
    public JComponent createSettingsPanel() {
        return createBaseSettingsPanel();
    }

    @Override
    public void applySettings() {
        // Implementation if needed
    }

    @Override
    public void resetToDefaults() {
        // Implementation if needed
    }
}