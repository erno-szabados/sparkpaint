package com.esgdev.sparkpaint.engine.tools.settings;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.MagicWandSelectionTool;
import com.esgdev.sparkpaint.engine.tools.ToolManager;

import javax.swing.*;
import java.awt.*;

public class MagicWandSelectionToolSettings extends AbstractSelectionToolSettings {
    private JSlider toleranceSlider;
    private JLabel toleranceValueLabel;

    public MagicWandSelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        MagicWandSelectionTool magicWandTool =
            (MagicWandSelectionTool) canvas.getTool(ToolManager.Tool.MAGIC_WAND_SELECTION);
        configureTransparencyCheckbox(magicWandTool::setTransparencyEnabled);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = createBaseSettingsPanel();

        // Add tolerance slider
        JLabel toleranceLabel = new JLabel("Color Tolerance:");
        toleranceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        MagicWandSelectionTool tool =
            (MagicWandSelectionTool) canvas.getTool(ToolManager.Tool.MAGIC_WAND_SELECTION);
        int currentTolerance = tool.getTolerance();

        toleranceSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, currentTolerance);
        toleranceSlider.setMaximumSize(new Dimension(150, 25));
        toleranceSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        toleranceSlider.setPaintTicks(true);
        toleranceSlider.setMajorTickSpacing(50);
        toleranceSlider.setMinorTickSpacing(10);

        toleranceValueLabel = new JLabel(String.valueOf(currentTolerance));
        toleranceValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        toleranceSlider.addChangeListener(e -> {
            int value = toleranceSlider.getValue();
            toleranceValueLabel.setText(String.valueOf(value));
            applySettings();
        });

        // Add components
        panel.add(Box.createVerticalStrut(10));
        panel.add(toleranceLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(toleranceSlider);
        panel.add(Box.createVerticalStrut(2));
        panel.add(toleranceValueLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == ToolManager.Tool.MAGIC_WAND_SELECTION) {
            MagicWandSelectionTool tool =
                (MagicWandSelectionTool) canvas.getTool(ToolManager.Tool.MAGIC_WAND_SELECTION);
            tool.setTolerance(toleranceSlider.getValue());
        }
    }

    @Override
    public void resetToDefaults() {
        toleranceSlider.setValue(32);
        toleranceValueLabel.setText("32");
        // TODO verify if we can eliminate this
        //setTransparencyCheckbox(true);
        applySettings();
    }
}