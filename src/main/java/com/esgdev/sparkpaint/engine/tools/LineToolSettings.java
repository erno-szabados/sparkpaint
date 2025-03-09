package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class LineToolSettings extends BaseToolSettings {
    private JSlider thicknessSlider;
    private JLabel thicknessValueLabel;

    public LineToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Line thickness slider
        JLabel thicknessLabel = new JLabel("Line Thickness:");
        thicknessLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int currentThickness = (int) canvas.getLineThickness();

        thicknessSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, currentThickness);
        thicknessSlider.setMaximumSize(new Dimension(150, 25));
        thicknessSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        thicknessSlider.setPaintTicks(true);
        thicknessSlider.setMajorTickSpacing(5);
        thicknessSlider.setMinorTickSpacing(1);

        thicknessValueLabel = new JLabel(String.valueOf(currentThickness));
        thicknessValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        thicknessSlider.addChangeListener(e -> {
            thicknessValueLabel.setText(String.valueOf(thicknessSlider.getValue()));
            applySettings();
        });

        // Add components
        panel.add(thicknessLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessValueLabel);
        panel.add(Box.createVerticalStrut(5));

        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == DrawingCanvas.Tool.LINE) {
            canvas.setLineThickness(thicknessSlider.getValue());
        }
    }

    @Override
    public void resetToDefaults() {
        thicknessSlider.setValue(2);
        thicknessValueLabel.setText("2");
        applySettings();
    }
}