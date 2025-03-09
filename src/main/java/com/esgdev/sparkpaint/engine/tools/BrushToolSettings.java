package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class BrushToolSettings extends BaseToolSettings {
    private JSlider sizeSlider;
    private JLabel sizeValueLabel;
    private JComboBox<BrushTool.BrushShape> shapeComboBox;
    private final BrushTool brushTool;

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

        sizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 1);
        sizeSlider.setMaximumSize(new Dimension(150, 25));
        sizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setMajorTickSpacing(5);
        sizeSlider.setMinorTickSpacing(1);

        sizeValueLabel = new JLabel("1");
        sizeValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sizeSlider.addChangeListener(e -> {
            sizeValueLabel.setText(String.valueOf(sizeSlider.getValue()));
            applySettings();
        });

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

        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == DrawingCanvas.Tool.BRUSH) {
            brushTool.setSize(sizeSlider.getValue());
            brushTool.setShape((BrushTool.BrushShape) shapeComboBox.getSelectedItem());
        }
    }

    @Override
    public void resetToDefaults() {
        sizeSlider.setValue(1);
        sizeValueLabel.setText("1");
        shapeComboBox.setSelectedItem(BrushTool.BrushShape.PIXEL);
        applySettings();
    }
}