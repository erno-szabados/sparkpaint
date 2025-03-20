package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class TextToolSettings extends BaseToolSettings {
    private JTextField textField;
    private JComboBox<String> fontDropdown;
    private JSlider fontSizeSlider;
    private JLabel fontSizeValueLabel;
    private JCheckBox antiAliasingCheckbox;
    private boolean useAntiAliasing = true;

    public TextToolSettings(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    public JComponent createSettingsPanel() {
        JPanel panel = (JPanel) super.createSettingsPanel();

        // Text input field
        JLabel textLabel = new JLabel("Text:");
        textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField = new JTextField("Sample Text", 20);
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));

        // Add document listener to apply changes as user types
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySettings();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySettings();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySettings();
            }
        });

        // Font dropdown
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontDropdown = new JComboBox<>(fonts);
        fontDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, fontDropdown.getPreferredSize().height));
        fontDropdown.addActionListener(e -> applySettings());

        // Font size slider
        JLabel fontSizeLabel = new JLabel("Font Size:");
        fontSizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 8, 72, 24);
        fontSizeSlider.setMaximumSize(new Dimension(150, 25));
        fontSizeSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setMajorTickSpacing(16);
        fontSizeSlider.setMinorTickSpacing(4);
        fontSizeValueLabel = new JLabel("24");
        fontSizeValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSizeSlider.addChangeListener(e -> {
            fontSizeValueLabel.setText(String.valueOf(fontSizeSlider.getValue()));
            applySettings();
        });

        // Add anti-aliasing checkbox
        antiAliasingCheckbox = new JCheckBox("Anti-aliasing", useAntiAliasing);
        antiAliasingCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        antiAliasingCheckbox.addActionListener(e -> applySettings());

        // Add components
        panel.add(textLabel);
        panel.add(textField);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fontLabel);
        panel.add(fontDropdown);
        panel.add(Box.createVerticalStrut(5));
        panel.add(fontSizeLabel);
        panel.add(fontSizeSlider);
        panel.add(fontSizeValueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(antiAliasingCheckbox);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    @Override
    public void applySettings() {
        if (canvas.getCurrentTool() == DrawingCanvas.Tool.TEXT) {
            TextTool tool = (TextTool) canvas.getActiveTool();
            tool.setText(textField.getText());
            tool.setFont(new Font((String) fontDropdown.getSelectedItem(), Font.PLAIN, fontSizeSlider.getValue()));
            useAntiAliasing = antiAliasingCheckbox.isSelected();
            tool.setAntiAliasing(useAntiAliasing);
        }
    }

    @Override
    public void resetToDefaults() {
        textField.setText("Sample Text");
        fontDropdown.setSelectedItem("Arial");
        fontSizeSlider.setValue(24);
        fontSizeValueLabel.setText("24");
        antiAliasingCheckbox.setSelected(true);
        applySettings();
    }
}