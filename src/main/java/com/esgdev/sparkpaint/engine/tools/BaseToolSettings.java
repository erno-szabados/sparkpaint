package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;

public abstract class BaseToolSettings implements ToolSettings {
    protected final DrawingCanvas canvas;

    protected BaseToolSettings(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}