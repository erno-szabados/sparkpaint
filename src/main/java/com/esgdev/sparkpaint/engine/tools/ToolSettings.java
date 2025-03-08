package com.esgdev.sparkpaint.engine.tools;

import javax.swing.*;

public interface ToolSettings {
    JComponent createSettingsPanel();
    void applySettings();
    void resetToDefaults();
}