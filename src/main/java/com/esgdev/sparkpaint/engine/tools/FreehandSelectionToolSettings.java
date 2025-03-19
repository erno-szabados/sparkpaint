package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.IconLoader;

import javax.swing.*;
import java.awt.*;

public class FreehandSelectionToolSettings extends BaseToolSettings {
    private static final int ICON_SIZE = 16;
    private final FreeHandSelectionTool freehandSelectionTool;
    private final JCheckBox transparencyCheckbox;


    public FreehandSelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.freehandSelectionTool = (FreeHandSelectionTool) canvas.getTool(DrawingCanvas.Tool.FREEHAND_SELECTION);
        this.transparencyCheckbox = new JCheckBox("Transparent Background");
        this.transparencyCheckbox.setToolTipText("Make selection background transparent");

    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Rotation buttons
        JButton rotateLeftButton = new JButton();
        rotateLeftButton.setIcon(IconLoader.loadAndScaleIcon("rotate-left.png", ICON_SIZE, ICON_SIZE));
        rotateLeftButton.setToolTipText("Rotate Left 90°");
        rotateLeftButton.addActionListener(e -> canvas.getSelectionManager().rotateSelection(-90));

        JButton rotateRightButton = new JButton();
        rotateRightButton.setIcon(IconLoader.loadAndScaleIcon("rotate-right.png", ICON_SIZE, ICON_SIZE));
        rotateRightButton.setToolTipText("Rotate Right 90°");
        rotateRightButton.addActionListener(e -> canvas.getSelectionManager().rotateSelection(90));

        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rotationPanel.add(rotateLeftButton);
        rotationPanel.add(rotateRightButton);

        // Add transparency checkbox
        transparencyCheckbox.addActionListener(e ->
                freehandSelectionTool.setTransparencyEnabled(transparencyCheckbox.isSelected())
        );

        panel.add(rotationPanel);
        panel.add(transparencyCheckbox);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    @Override
    public void applySettings() {

    }

    @Override
    public void resetToDefaults() {

    }
}