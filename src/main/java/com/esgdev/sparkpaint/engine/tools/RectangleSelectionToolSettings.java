package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.IconLoader;

import javax.swing.*;
import java.awt.*;

/**
 * RectangleSelectionToolSettings is a class that provides the settings panel for the Rectangle Selection Tool.
 * It allows users to configure the tool's settings, including rotation and mirroring options.
 */
public class RectangleSelectionToolSettings extends BaseToolSettings {
    private static final int ICON_SIZE = 16;
    private final RectangleSelectionTool rectangleSelectionTool;
    private final JCheckBox transparencyCheckbox;


    /**
     * Constructor for RectangleSelectionToolSettings.
     *
     * @param canvas The drawing canvas where the tool is applied.
     */
    public RectangleSelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.rectangleSelectionTool = (RectangleSelectionTool) canvas.getTool(DrawingCanvas.Tool.RECTANGLE_SELECTION);
        this.transparencyCheckbox = new JCheckBox("Transparent Background");
        this.transparencyCheckbox.setToolTipText("Make selection background transparent");
    }

    /**
     * Creates the settings panel for the Rectangle Selection Tool.
     *
     * @return A JPanel containing the settings components.
     */
    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Transformation buttons panel (use GridLayout for 2x2 grid)
        JButton rotateLeftButton = new JButton();
        rotateLeftButton.setIcon(IconLoader.loadAndScaleIcon("rotate-left.png", ICON_SIZE, ICON_SIZE));
        rotateLeftButton.setToolTipText("Rotate Left 90°");
        rotateLeftButton.addActionListener(e -> canvas.getSelectionManager().rotateSelection(-90));

        JButton rotateRightButton = new JButton();
        rotateRightButton.setIcon(IconLoader.loadAndScaleIcon("rotate-right.png", ICON_SIZE, ICON_SIZE));
        rotateRightButton.setToolTipText("Rotate Right 90°");
        rotateRightButton.addActionListener(e -> canvas.getSelectionManager().rotateSelection(90));

        JButton horizontalMirrorButton = new JButton();
        horizontalMirrorButton.setIcon(IconLoader.loadAndScaleIcon("horizontal-mirror.png", ICON_SIZE, ICON_SIZE));
        horizontalMirrorButton.setToolTipText("Mirror Horizontally");
        horizontalMirrorButton.addActionListener(e -> canvas.getSelectionManager().flipSelection(true));

        JButton verticalMirrorButton = new JButton();
        verticalMirrorButton.setIcon(IconLoader.loadAndScaleIcon("vertical-mirror.png", ICON_SIZE, ICON_SIZE));
        verticalMirrorButton.setToolTipText("Mirror Vertically");
        verticalMirrorButton.addActionListener(e -> canvas.getSelectionManager().flipSelection(false));

        JPanel transformationPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        transformationPanel.add(rotateLeftButton);
        transformationPanel.add(rotateRightButton);
        transformationPanel.add(horizontalMirrorButton);
        transformationPanel.add(verticalMirrorButton);
        transformationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Configure transparency checkbox
        transparencyCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        transparencyCheckbox.addActionListener(e ->
                rectangleSelectionTool.setTransparencyEnabled(transparencyCheckbox.isSelected())
        );

        // Add components to panel with proper spacing
        panel.add(transformationPanel);
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