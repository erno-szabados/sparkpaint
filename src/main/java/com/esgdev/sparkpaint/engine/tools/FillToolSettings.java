package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class FillToolSettings extends BaseToolSettings {
    private JSlider epsilonSlider;
    private JLabel epsilonValueLabel;
    private JSlider edgeThresholdSlider;
    private JLabel edgeThresholdValueLabel;
    private JComboBox<FillModeOption> fillModeComboBox;

    public FillToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Fill Mode selection
        JLabel fillModeLabel = new JLabel("Fill Type:");
        fillModeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create combo box for fill modes
        fillModeComboBox = new JComboBox<>();
        fillModeComboBox.setMaximumSize(new Dimension(200, 25));
        fillModeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Current selected fill mode
        FillTool.FillMode currentMode = ((FillTool) canvas.getTool(ToolManager.Tool.FILL)).getFillMode();

        // Add all fill modes to the combo box
        for (FillTool.FillMode mode : FillTool.FillMode.values()) {
            FillModeOption option = new FillModeOption(mode);
            fillModeComboBox.addItem(option);

            // Set the current selection
            if (mode == currentMode) {
                fillModeComboBox.setSelectedItem(option);
            }
        }

        // Add action listener
        fillModeComboBox.addActionListener(e -> {
            FillModeOption selected = (FillModeOption) fillModeComboBox.getSelectedItem();
            if (selected != null) {
                ((FillTool) canvas.getTool(ToolManager.Tool.FILL)).setFillMode(selected.getMode());
            }
        });

        panel.add(fillModeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(fillModeComboBox);
        panel.add(Box.createVerticalStrut(10));

        // Epsilon slider
        JLabel epsilonLabel = new JLabel("Color Tolerance:");
        epsilonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int currentEpsilon = FillTool.DEFAULT_FILL_EPSILON;

        epsilonSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, currentEpsilon / 2);
        epsilonSlider.setMaximumSize(new Dimension(150, 25));
        epsilonSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        epsilonSlider.setPaintTicks(true);
        epsilonSlider.setMajorTickSpacing(20);
        epsilonSlider.setMinorTickSpacing(5);

        epsilonValueLabel = new JLabel(String.valueOf(currentEpsilon));
        epsilonValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        epsilonSlider.addChangeListener(e -> {
            epsilonValueLabel.setText(String.valueOf(epsilonSlider.getValue()));
            applySettings();
        });

        // Edge threshold slider
        JLabel edgeThresholdLabel = new JLabel("Edge Detection:");
        edgeThresholdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int currentEdgeThreshold = FillTool.DEFAULT_EDGE_THRESHOLD;

        edgeThresholdSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, (int) (currentEdgeThreshold / 2.55));
        edgeThresholdSlider.setMaximumSize(new Dimension(150, 25));
        edgeThresholdSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        edgeThresholdSlider.setPaintTicks(true);
        edgeThresholdSlider.setMajorTickSpacing(50);
        edgeThresholdSlider.setMinorTickSpacing(10);

        edgeThresholdValueLabel = new JLabel(String.valueOf(currentEdgeThreshold));
        edgeThresholdValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        edgeThresholdSlider.addChangeListener(e -> {
            edgeThresholdValueLabel.setText(String.valueOf(edgeThresholdSlider.getValue()));
            applySettings();
        });

        // Add components
        panel.add(epsilonLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(epsilonSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(epsilonValueLabel);
        panel.add(Box.createVerticalStrut(10));

        panel.add(edgeThresholdLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(edgeThresholdSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(edgeThresholdValueLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    @Override
    public void applySettings() {
        FillTool tool = (FillTool) canvas.getTool(ToolManager.Tool.FILL);
        int mappedEpsilon = epsilonSlider.getValue() * 2;
        tool.setEpsilon(mappedEpsilon);
        // Map 0-100 UI range to 0-255 technical range for edge threshold
        int mappedThreshold = (int) (edgeThresholdSlider.getValue() * 2.55);
        tool.setEdgeThreshold(mappedThreshold);
    }

    @Override
    public void resetToDefaults() {
        // Reset sliders
        epsilonSlider.setValue(FillTool.DEFAULT_FILL_EPSILON / 2);
        epsilonValueLabel.setText(String.valueOf(epsilonSlider.getValue()));
        edgeThresholdSlider.setValue((int) (FillTool.DEFAULT_EDGE_THRESHOLD / 2.55));
        edgeThresholdValueLabel.setText(String.valueOf(edgeThresholdSlider.getValue()));

        // Reset fill mode to default
        FillTool.FillMode defaultMode = FillTool.FillMode.SMART_FILL;
        for (int i = 0; i < fillModeComboBox.getItemCount(); i++) {
            FillModeOption option = fillModeComboBox.getItemAt(i);
            if (option.getMode() == defaultMode) {
                fillModeComboBox.setSelectedIndex(i);
                break;
            }
        }
        ((FillTool) canvas.getTool(ToolManager.Tool.FILL)).setFillMode(defaultMode);

        applySettings();
    }

    // Wrapper class for fill mode items in combo box
    private static class FillModeOption {
        private final FillTool.FillMode mode;

        public FillModeOption(FillTool.FillMode mode) {
            this.mode = mode;
        }

        public FillTool.FillMode getMode() {
            return mode;
        }

        @Override
        public String toString() {
            return mode.getDisplayName();
        }
    }
}