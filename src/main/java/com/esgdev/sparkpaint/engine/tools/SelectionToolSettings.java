package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.IconLoader;

import javax.swing.*;
import java.awt.*;

public class SelectionToolSettings extends BaseToolSettings {
    private static final int ICON_SIZE = 16;
    private final RectangleSelectionTool rectangleSelectionTool;
    private final JCheckBox transparencyCheckbox;


    public SelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.rectangleSelectionTool = (RectangleSelectionTool) canvas.getTool(DrawingCanvas.Tool.RECTANGLE_SELECTION);
        this.transparencyCheckbox = new JCheckBox("Transparent Background");
        this.transparencyCheckbox.setToolTipText("Make selection background transparent");

    }

   @Override
    public JComponent createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Rotation buttons panel (keep as FlowLayout)
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
        rotationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Configure transparency checkbox
        transparencyCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        transparencyCheckbox.addActionListener(e ->
                rectangleSelectionTool.setTransparencyEnabled(transparencyCheckbox.isSelected())
        );

        // Add components to panel with proper spacing
        panel.add(rotationPanel);
        panel.add(Box.createVerticalStrut(5));
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