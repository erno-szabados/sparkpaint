package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class BrushToolSettings extends BaseToolSettings {
    private JSlider sizeSlider;
    private JLabel sizeValueLabel;
    private JComboBox<BrushTool.BrushShape> shapeComboBox;
    private final BrushTool brushTool;
    private JSlider sprayDensitySlider;
    private JLabel sprayDensityValueLabel;
    private JCheckBox antiAliasingCheckbox;
    private boolean useAntiAliasing = true;  // Default value
    private JSlider blendStrengthSlider;
    private JLabel blendStrengthValueLabel;

    public BrushToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.brushTool = (BrushTool) canvas.getTool(DrawingCanvas.Tool.BRUSH);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Shape selector
        JLabel shapeLabel = new JLabel("Brush Shape:");
        shapeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        shapeComboBox = new JComboBox<>(BrushTool.BrushShape.values());
        shapeComboBox.setMaximumSize(new Dimension(150, 25));
        shapeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        shapeComboBox.addActionListener(e -> applySettings());

        // Size slider
        JLabel sizeLabel = new JLabel("Brush Size:");
        sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, BrushTool.DEFAULT_SPRAY_SIZE);
        sizeSlider.setMaximumSize(new Dimension(150, 25));
        sizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setMajorTickSpacing(5);
        sizeSlider.setMinorTickSpacing(1);

        sizeValueLabel = new JLabel(String.valueOf(BrushTool.DEFAULT_SPRAY_SIZE));
        sizeValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sizeSlider.addChangeListener(e -> {
            sizeValueLabel.setText(String.valueOf(sizeSlider.getValue()));
            applySettings();
        });

        // Spray density slider
        JLabel sprayDensityLabel = new JLabel("Spray Density:");
        sprayDensityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sprayDensitySlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 20);
        sprayDensitySlider.setMaximumSize(new Dimension(150, 25));
        sprayDensitySlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        sprayDensitySlider.setPaintTicks(true);
        sprayDensitySlider.setMajorTickSpacing(20);
        sprayDensitySlider.setMinorTickSpacing(5);

        sprayDensityValueLabel = new JLabel(String.valueOf(BrushTool.DEFAULT_SPRAY_DENSITY));
        sprayDensityValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sprayDensitySlider.addChangeListener(e -> {
            sprayDensityValueLabel.setText(String.valueOf(sprayDensitySlider.getValue()));
            applySettings();
        });

        // Blend strength slider
        JLabel blendStrengthLabel = new JLabel("Blend Strength:");
        blendStrengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        blendStrengthSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 10);  // 1-100 maps to 0.01-1.0
        blendStrengthSlider.setMaximumSize(new Dimension(150, 25));
        blendStrengthSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        blendStrengthSlider.setPaintTicks(true);
        blendStrengthSlider.setMajorTickSpacing(20);
        blendStrengthSlider.setMinorTickSpacing(5);

        blendStrengthValueLabel = new JLabel("0.10");
        blendStrengthValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        blendStrengthSlider.addChangeListener(e -> {
            float value = blendStrengthSlider.getValue() / 100f;
            blendStrengthValueLabel.setText(String.format("%.2f", value));
            applySettings();
        });

        // Anti-aliasing checkbox
        antiAliasingCheckbox = new JCheckBox("Anti-aliasing", useAntiAliasing);
        antiAliasingCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        antiAliasingCheckbox.addActionListener(e -> applySettings());

        panel.add(shapeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(shapeComboBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sizeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sizeSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sizeValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sprayDensityLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sprayDensitySlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(sprayDensityValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(blendStrengthLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(blendStrengthSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(blendStrengthValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(antiAliasingCheckbox);
        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == DrawingCanvas.Tool.BRUSH) {
            brushTool.setSize(sizeSlider.getValue());
            brushTool.setShape((BrushTool.BrushShape) shapeComboBox.getSelectedItem());
            brushTool.setSprayDensity(sprayDensitySlider.getValue());
            brushTool.setMaxBlendStrength(blendStrengthSlider.getValue() / 100f);
            useAntiAliasing = antiAliasingCheckbox.isSelected();
            brushTool.setAntiAliasing(useAntiAliasing);
        }
    }

    @Override
    public void resetToDefaults() {
        sizeSlider.setValue(BrushTool.DEFAULT_SPRAY_SIZE);
        sizeValueLabel.setText(String.valueOf(BrushTool.DEFAULT_SPRAY_SIZE));
        shapeComboBox.setSelectedItem(BrushTool.BrushShape.SQUARE);
        sprayDensitySlider.setValue(BrushTool.DEFAULT_SPRAY_DENSITY);
        sprayDensityValueLabel.setText(String.valueOf(BrushTool.DEFAULT_SPRAY_DENSITY));
        blendStrengthSlider.setValue(10);  // 0.10 default
        blendStrengthValueLabel.setText("0.10");
        antiAliasingCheckbox.setSelected(true);
        applySettings();
    }
}