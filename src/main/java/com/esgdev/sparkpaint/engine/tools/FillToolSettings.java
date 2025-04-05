package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class FillToolSettings extends BaseToolSettings {
    private JSlider epsilonSlider;
    private JLabel epsilonValueLabel;
    private JComboBox<FillModeOption> fillModeComboBox;

    public FillToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Fill Mode selection (dropdown)
        JLabel fillModeLabel = new JLabel("Fill Type:");
        fillModeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        fillModeComboBox = new JComboBox<>();
        fillModeComboBox.setMaximumSize(new Dimension(200, 25));
        fillModeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        FillTool.FillMode currentMode = ((FillTool) canvas.getTool(ToolManager.Tool.FILL)).getFillMode();

        for (FillTool.FillMode mode : FillTool.FillMode.values()) {
            FillModeOption option = new FillModeOption(mode);
            fillModeComboBox.addItem(option);

            if (mode == currentMode) {
                fillModeComboBox.setSelectedItem(option);
            }
        }

        // Epsilon slider (color tolerance)
        JLabel epsilonLabel = new JLabel("Color Tolerance:");
        epsilonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int currentEpsilon = FillTool.DEFAULT_FILL_EPSILON;

        epsilonSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, currentEpsilon / 2);
        epsilonSlider.setMaximumSize(new Dimension(150, 25));
        epsilonSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        epsilonSlider.setPaintTicks(true);
        epsilonSlider.setMajorTickSpacing(20);
        epsilonSlider.setMinorTickSpacing(5);

        epsilonValueLabel = new JLabel(String.valueOf(currentEpsilon / 2));
        epsilonValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        epsilonSlider.addChangeListener(e -> {
            epsilonValueLabel.setText(String.valueOf(epsilonSlider.getValue()));
            applySettings();
        });

        // Set initial slider states based on current mode
        updateSliderEnabledStates(currentMode);

        // Add action listener to update slider states when fill mode changes
        fillModeComboBox.addActionListener(e -> {
            FillModeOption selected = (FillModeOption) fillModeComboBox.getSelectedItem();
            if (selected != null) {
                FillTool.FillMode mode = selected.getMode();
                ((FillTool) canvas.getTool(ToolManager.Tool.FILL)).setFillMode(mode);

                // Update slider enabled states based on selected mode
                updateSliderEnabledStates(mode);
            }
        });

        // Add components
        panel.add(fillModeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(fillModeComboBox);
        panel.add(Box.createVerticalStrut(10));

        panel.add(epsilonLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(epsilonSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(epsilonValueLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Updates the enabled state of sliders based on the selected fill mode
     *
     * @param mode The current fill mode
     */
    private void updateSliderEnabledStates(FillTool.FillMode mode) {
        boolean usesSmartSettings = mode == FillTool.FillMode.SMART_FILL ||
                mode == FillTool.FillMode.SMART_LINEAR ||
                mode == FillTool.FillMode.SMART_CIRCULAR;

        // Update epsilon slider (color tolerance)
        epsilonSlider.setEnabled(usesSmartSettings);
        epsilonValueLabel.setEnabled(usesSmartSettings);
    }

    @Override
    public void resetToDefaults() {
        // Reset sliders to display the default values in 0-100 range
        epsilonSlider.setValue(FillTool.DEFAULT_FILL_EPSILON / 2);
        epsilonValueLabel.setText(String.valueOf(epsilonSlider.getValue()));

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

        // Update slider enabled states based on default mode
        updateSliderEnabledStates(defaultMode);

        applySettings();
    }


    @Override
    public void applySettings() {
        FillTool tool = (FillTool) canvas.getTool(ToolManager.Tool.FILL);
        int mappedEpsilon = epsilonSlider.getValue() * 2;
        tool.setEpsilon(mappedEpsilon);
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