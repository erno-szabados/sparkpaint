package com.esgdev.sparkpaint.engine.tools.settings;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.IconLoader;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for all selection tool settings panels.
 * Provides common transformation controls for selections.
 */
public abstract class AbstractSelectionToolSettings extends BaseToolSettings {
    protected static final int ICON_SIZE = 16;
    protected final JCheckBox transparencyCheckbox;

    public AbstractSelectionToolSettings(DrawingCanvas canvas) {
        super(canvas);
        this.transparencyCheckbox = new JCheckBox("Transparent Background");
        this.transparencyCheckbox.setToolTipText("Make selection background transparent");
    }

    /**
     * Creates the common transformation panel for selection tools.
     *
     * @return A panel with rotation and mirroring buttons
     */
    protected JPanel createTransformationPanel() {
        JButton rotateLeftButton = new JButton();
        rotateLeftButton.setIcon(IconLoader.loadAndScaleIcon("rotate-left.png", ICON_SIZE, ICON_SIZE));
        rotateLeftButton.setToolTipText("Rotate Left 90°");
        rotateLeftButton.addActionListener(e -> canvas.rotateSelection(-90));

        JButton rotateRightButton = new JButton();
        rotateRightButton.setIcon(IconLoader.loadAndScaleIcon("rotate-right.png", ICON_SIZE, ICON_SIZE));
        rotateRightButton.setToolTipText("Rotate Right 90°");
        rotateRightButton.addActionListener(e -> canvas.rotateSelection(90));

        JButton horizontalMirrorButton = new JButton();
        horizontalMirrorButton.setIcon(IconLoader.loadAndScaleIcon("horizontal-mirror.png", ICON_SIZE, ICON_SIZE));
        horizontalMirrorButton.setToolTipText("Mirror Horizontally");
        horizontalMirrorButton.addActionListener(e -> canvas.flipSelection(true));

        JButton verticalMirrorButton = new JButton();
        verticalMirrorButton.setIcon(IconLoader.loadAndScaleIcon("vertical-mirror.png", ICON_SIZE, ICON_SIZE));
        verticalMirrorButton.setToolTipText("Mirror Vertically");
        verticalMirrorButton.addActionListener(e -> canvas.flipSelection(false));

        JPanel transformationPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        transformationPanel.add(rotateLeftButton);
        transformationPanel.add(rotateRightButton);
        transformationPanel.add(horizontalMirrorButton);
        transformationPanel.add(verticalMirrorButton);
        transformationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return transformationPanel;
    }

    /**
     * Creates the base settings panel including transformation controls.
     *
     * @return A panel with common selection tool settings
     */
    protected JPanel createBaseSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createTransformationPanel());
        panel.add(Box.createVerticalStrut(5));

        transparencyCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(transparencyCheckbox);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Configure transparency checkbox with the appropriate listener
     *
     * @param transparencyEnabled callback to set transparency state
     */
    protected void configureTransparencyCheckbox(java.util.function.Consumer<Boolean> transparencyEnabled) {
        transparencyCheckbox.addActionListener(e ->
                transparencyEnabled.accept(transparencyCheckbox.isSelected())
        );
    }
}