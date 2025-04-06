package com.esgdev.sparkpaint.engine.tools.settings;

    import com.esgdev.sparkpaint.engine.DrawingCanvas;
    import com.esgdev.sparkpaint.engine.tools.FreeHandSelectionTool;
    import com.esgdev.sparkpaint.engine.tools.ToolManager;

    import javax.swing.*;

    /**
     * Settings panel for the Freehand Selection Tool.
     */
    public class FreehandSelectionToolSettings extends AbstractSelectionToolSettings {

        public FreehandSelectionToolSettings(DrawingCanvas canvas) {
            super(canvas);
            FreeHandSelectionTool freehandSelectionTool = (FreeHandSelectionTool) canvas.getTool(ToolManager.Tool.FREEHAND_SELECTION);
            configureTransparencyCheckbox(freehandSelectionTool::setTransparencyEnabled);
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