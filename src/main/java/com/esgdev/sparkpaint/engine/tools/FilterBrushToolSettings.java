package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class FilterBrushToolSettings extends BaseToolSettings {
    private JSlider sizeSlider;
    private JLabel sizeValueLabel;
    private JComboBox<FilterBrushTool.FilterType> filterTypeComboBox;
    private final FilterBrushTool filterBrushTool;
    private JSlider strengthSlider;
    private JLabel strengthValueLabel;
    private JCheckBox antiAliasingCheckbox;
    private boolean useAntiAliasing = true;  // Default value

    public FilterBrushToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.filterBrushTool = (FilterBrushTool) canvas.getTool(ToolManager.Tool.FILTER_BRUSH);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Filter type selector
        JLabel filterTypeLabel = new JLabel("Filter Type:");
        filterTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterTypeComboBox = new JComboBox<>(FilterBrushTool.FilterType.values());
        filterTypeComboBox.setMaximumSize(new Dimension(150, 25));
        filterTypeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterTypeComboBox.addActionListener(e -> applySettings());

        // Size slider
        JLabel sizeLabel = new JLabel("Brush Size:");
        sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, FilterBrushTool.DEFAULT_SIZE);
        sizeSlider.setMaximumSize(new Dimension(150, 25));
        sizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setMajorTickSpacing(25);
        sizeSlider.setMinorTickSpacing(5);

        sizeValueLabel = new JLabel(String.valueOf(FilterBrushTool.DEFAULT_SIZE));
        sizeValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sizeSlider.addChangeListener(e -> {
            sizeValueLabel.setText(String.valueOf(sizeSlider.getValue()));
            applySettings();
        });

        // Strength slider
        JLabel strengthLabel = new JLabel("Filter Strength:");
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        strengthSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, FilterBrushTool.DEFAULT_STRENGTH);
        strengthSlider.setMaximumSize(new Dimension(150, 25));
        strengthSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        strengthSlider.setPaintTicks(true);
        strengthSlider.setMajorTickSpacing(25);
        strengthSlider.setMinorTickSpacing(5);

        strengthValueLabel = new JLabel(String.valueOf(FilterBrushTool.DEFAULT_STRENGTH));
        strengthValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        strengthSlider.addChangeListener(e -> {
            strengthValueLabel.setText(String.valueOf(strengthSlider.getValue()));
            applySettings();
        });

        // Anti-aliasing checkbox
        antiAliasingCheckbox = new JCheckBox("Anti-aliasing", useAntiAliasing);
        antiAliasingCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        antiAliasingCheckbox.addActionListener(e -> applySettings());

        panel.add(filterTypeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(filterTypeComboBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sizeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sizeSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sizeValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(strengthLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(strengthSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(strengthValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(antiAliasingCheckbox);
        panel.add(Box.createVerticalGlue());

        resetToDefaults();
        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == ToolManager.Tool.FILTER_BRUSH) {
            filterBrushTool.setSize(sizeSlider.getValue());
            filterBrushTool.setFilterType((FilterBrushTool.FilterType) filterTypeComboBox.getSelectedItem());
            filterBrushTool.setStrength(strengthSlider.getValue() / 100f);
            useAntiAliasing = antiAliasingCheckbox.isSelected();
            filterBrushTool.setAntiAliasing(useAntiAliasing);
        }
    }

    @Override
    public void resetToDefaults() {
        sizeSlider.setValue(FilterBrushTool.DEFAULT_SIZE);
        sizeValueLabel.setText(String.valueOf(FilterBrushTool.DEFAULT_SIZE));
        filterTypeComboBox.setSelectedItem(FilterBrushTool.FilterType.BLUR);
        strengthSlider.setValue(FilterBrushTool.DEFAULT_STRENGTH);
        strengthValueLabel.setText(String.valueOf(FilterBrushTool.DEFAULT_STRENGTH));
        antiAliasingCheckbox.setSelected(true);
        applySettings();
    }
}