package com.esgdev.sparkpaint.engine.tools;


import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FillToolSettings extends BaseToolSettings {
    private JSlider epsilonSlider;
    private JLabel epsilonValueLabel;
    private JSlider edgeThresholdSlider;
    private JLabel edgeThresholdValueLabel;
    private ButtonGroup fillModeGroup;
    private Map<FillTool.FillMode, JRadioButton> fillModeButtons;

    public FillToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Fill Mode selection
        JLabel fillModeLabel = new JLabel("Fill Type:");
        fillModeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel fillModePanel = new JPanel();
        fillModePanel.setLayout(new BoxLayout(fillModePanel, BoxLayout.Y_AXIS));
        fillModePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fillModePanel.setMaximumSize(new Dimension(200, 60));

        fillModeGroup = new ButtonGroup();
        fillModeButtons = new HashMap<>();

        // Create radio buttons for each fill mode
        for (FillTool.FillMode mode : FillTool.FillMode.values()) {
            JRadioButton radioButton = new JRadioButton(mode.getDisplayName());
            radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            radioButton.setActionCommand(mode.name());

            if (mode == ((FillTool)canvas.getTool(ToolManager.Tool.FILL)).getFillMode()) {
                radioButton.setSelected(true);
            }

            radioButton.addActionListener(e -> {
                FillTool.FillMode selectedMode = FillTool.FillMode.valueOf(e.getActionCommand());
                ((FillTool)canvas.getTool(ToolManager.Tool.FILL)).setFillMode(selectedMode);
            });

            fillModeGroup.add(radioButton);
            fillModeButtons.put(mode, radioButton);
            fillModePanel.add(radioButton);
        }

        panel.add(fillModeLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(fillModePanel);
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
        int mappedThreshold = (int)(edgeThresholdSlider.getValue() * 2.55);
        tool.setEdgeThreshold(mappedThreshold);
    }

 @Override
 public void resetToDefaults() {
     // Existing reset code
     epsilonSlider.setValue(FillTool.DEFAULT_FILL_EPSILON / 2);
     epsilonValueLabel.setText(String.valueOf(epsilonSlider.getValue()));
     edgeThresholdSlider.setValue((int)(FillTool.DEFAULT_EDGE_THRESHOLD / 2.55));
     edgeThresholdValueLabel.setText(String.valueOf(edgeThresholdSlider.getValue()));

     FillTool.FillMode defaultMode = FillTool.FillMode.SMART_FILL;
     fillModeButtons.get(defaultMode).setSelected(true);
     ((FillTool)canvas.getTool(ToolManager.Tool.FILL)).setFillMode(defaultMode);

     applySettings();
 }
}