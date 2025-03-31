package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class LineToolSettings extends BaseToolSettings {
    private JSlider thicknessSlider;
    private JLabel thicknessValueLabel;
    private JCheckBox antiAliasingCheckbox;
    private JComboBox<LineModeOption> modeComboBox;
    private boolean useAntiAliasing = true;  // Default value

    public LineToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Line mode selection
        JLabel modeLabel = new JLabel("Line Type:");
        modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        modeComboBox = new JComboBox<>();
        modeComboBox.setMaximumSize(new Dimension(200, 25));
        modeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        LineTool.LineMode currentMode = ((LineTool) canvas.getTool(ToolManager.Tool.LINE)).getMode();

        for (LineTool.LineMode mode : LineTool.LineMode.values()) {
            LineModeOption option = new LineModeOption(mode);
            modeComboBox.addItem(option);

            if (mode == currentMode) {
                modeComboBox.setSelectedItem(option);
            }
        }

        // Curve tension slider
        JLabel tensionLabel = new JLabel("Curve Tension:");
        tensionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        float currentTension = ((LineTool) canvas.getTool(ToolManager.Tool.LINE)).getCurveTension();
        int tensionValue = Math.round(currentTension * 100);

        JSlider tensionSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, tensionValue);
        tensionSlider.setMaximumSize(new Dimension(150, 25));
        tensionSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        tensionSlider.setPaintTicks(true);
        tensionSlider.setMajorTickSpacing(25);
        tensionSlider.setMinorTickSpacing(5);

        JLabel tensionValueLabel = new JLabel(String.format("%d", tensionSlider.getValue()));
        tensionValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tensionSlider.addChangeListener(e -> {
            float tension = tensionSlider.getValue() / 100.0f;
            tensionValueLabel.setText(String.format("%d", tensionSlider.getValue()));
            ((LineTool) canvas.getTool(ToolManager.Tool.LINE)).setCurveTension(tension);
        });

        // Enable/disable tension controls based on mode
        boolean isCurveMode = currentMode == LineTool.LineMode.CURVE || currentMode == LineTool.LineMode.CLOSED_CURVE;
        tensionLabel.setEnabled(isCurveMode);
        tensionSlider.setEnabled(isCurveMode);
        tensionValueLabel.setEnabled(isCurveMode);

        modeComboBox.addActionListener(e -> {
            LineModeOption selected = (LineModeOption) modeComboBox.getSelectedItem();
            if (selected != null) {
                LineTool.LineMode mode = selected.getMode();
                ((LineTool) canvas.getTool(ToolManager.Tool.LINE)).setMode(mode);

                // Enable/disable tension controls
                boolean enableTension = mode == LineTool.LineMode.CURVE || mode == LineTool.LineMode.CLOSED_CURVE;
                tensionLabel.setEnabled(enableTension);
                tensionSlider.setEnabled(enableTension);
                tensionValueLabel.setEnabled(enableTension);
            }
        });

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

        // Anti-aliasing checkbox
        antiAliasingCheckbox = new JCheckBox("Anti-aliasing", useAntiAliasing);
        antiAliasingCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        antiAliasingCheckbox.addActionListener(e -> applySettings());

        // Add components
        panel.add(modeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(modeComboBox);
        panel.add(Box.createVerticalStrut(10));

        // Thickness controls
        panel.add(thicknessLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessValueLabel);
        panel.add(Box.createVerticalStrut(5));

        // Tension controls (always visible but possibly disabled)
        panel.add(tensionLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(tensionSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(tensionValueLabel);
        panel.add(Box.createVerticalStrut(10));

        panel.add(antiAliasingCheckbox);
        panel.add(Box.createVerticalGlue());

        return panel;
    }


    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == ToolManager.Tool.LINE) {
            canvas.setLineThickness(thicknessSlider.getValue());
            LineTool tool = (LineTool) canvas.getTool(ToolManager.Tool.LINE);
            useAntiAliasing = antiAliasingCheckbox.isSelected();
            tool.setAntiAliasing(useAntiAliasing);
        }
    }

    @Override
    public void resetToDefaults() {
        thicknessSlider.setValue(2);
        thicknessValueLabel.setText("2");
        antiAliasingCheckbox.setSelected(true);

        // Reset to single line mode
        for (int i = 0; i < modeComboBox.getItemCount(); i++) {
            LineModeOption option = modeComboBox.getItemAt(i);
            if (option.getMode() == LineTool.LineMode.SINGLE_LINE) {
                modeComboBox.setSelectedIndex(i);
                break;
            }
        }

        applySettings();
    }

    // Wrapper class for line mode items in combo box
    private static class LineModeOption {
        private final LineTool.LineMode mode;

        public LineModeOption(LineTool.LineMode mode) {
            this.mode = mode;
        }

        public LineTool.LineMode getMode() {
            return mode;
        }

        @Override
        public String toString() {
            return mode.getDisplayName();
        }
    }
}