package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class CircleToolSettings extends BaseToolSettings {
    private JCheckBox filledCheckBox;
    private JSlider thicknessSlider;
    private JLabel thicknessValueLabel;
    private JCheckBox antiAliasingCheckbox;
    private JRadioButton centerBasedButton;
    private boolean useAntiAliasing = true;  // Default value

    public CircleToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Draw Mode panel
        ButtonGroup drawModeGroup = new ButtonGroup();
        JPanel drawModePanel = new JPanel();
        drawModePanel.setLayout(new BoxLayout(drawModePanel, BoxLayout.Y_AXIS));
        drawModePanel.setBorder(BorderFactory.createTitledBorder("Draw Mode"));
        drawModePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Make the panel fill the horizontal space
        drawModePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, drawModePanel.getMaximumSize().height));

        JRadioButton cornerBasedButton = new JRadioButton("Ellipse", true);
        cornerBasedButton.setToolTipText("Draws an ellipse based on the corner points");
        centerBasedButton = new JRadioButton("Circle");
        centerBasedButton.setToolTipText("Draws a circle based on the center point");

        cornerBasedButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerBasedButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        drawModeGroup.add(cornerBasedButton);
        drawModeGroup.add(centerBasedButton);

        cornerBasedButton.addActionListener(e -> applySettings());
        centerBasedButton.addActionListener(e -> applySettings());

        drawModePanel.add(cornerBasedButton);
        drawModePanel.add(centerBasedButton);

        // Fill checkbox
        filledCheckBox = new JCheckBox("Filled");
        filledCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        filledCheckBox.addActionListener(e -> applySettings());

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
        // Try setting minimum size for the checkbox
        antiAliasingCheckbox.setMinimumSize(new Dimension(150, 25));
        antiAliasingCheckbox.setPreferredSize(new Dimension(150, 25));


        // Add components directly to the panel
        panel.add(drawModePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(filledCheckBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(thicknessLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(thicknessValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(antiAliasingCheckbox);

// Add some extra space at the bottom of the panel
        panel.add(Box.createVerticalGlue());


        return panel;
    }

    // Anti-aliasing checkbox

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == ToolManager.Tool.CIRCLE) {
            if (canvas.getActiveTool() instanceof EllipseTool) {
                EllipseTool tool = (EllipseTool) canvas.getActiveTool();
                tool.setFilled(filledCheckBox.isSelected());
                tool.setCenterBased(centerBasedButton.isSelected());
                useAntiAliasing = antiAliasingCheckbox.isSelected();
                canvas.setLineThickness(thicknessSlider.getValue());
                tool.setAntiAliasing(useAntiAliasing);
            }
        }
    }

    @Override
    public void resetToDefaults() {
        filledCheckBox.setSelected(false);
        thicknessSlider.setValue(2);
        thicknessValueLabel.setText("2");
        antiAliasingCheckbox.setSelected(true);
        applySettings();
    }
}