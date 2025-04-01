package com.esgdev.sparkpaint.ui.layer;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * LayerPropertiesDialog allows users to adjust properties of a selected layer.
 * It provides sliders for opacity, brightness, contrast, and saturation adjustments.
 */
public class LayerPropertiesDialog extends JDialog {
    private final DrawingCanvas canvas;
    private final Layer layer;

    private final JSlider opacitySlider;
    private final JSlider brightnessSlider;
    private final JSlider contrastSlider;
    private final JSlider saturationSlider;

    // Store original values for reset and cancel
    private final BufferedImage originalImage;
    private final BufferedImage previewImage;

    /**
     * Constructor for LayerPropertiesDialog.
     *
     * @param owner The parent frame
     * @param canvas The drawing canvas containing the layer
     */
    public LayerPropertiesDialog(Frame owner, DrawingCanvas canvas) {
        super(owner, "Adjust Layer: " + canvas.getLayers().get(canvas.getCurrentLayerIndex()).getName(), true);
        this.canvas = canvas;
        this.layer = canvas.getLayers().get(canvas.getCurrentLayerIndex());

        // Store original values
        this.originalImage = deepCopyImage(layer.getImage());
        this.previewImage = deepCopyImage(originalImage);

        // Initialize sliders
        opacitySlider = createSlider(0, 100, 100);
        brightnessSlider = createSlider(-100, 100, 0);
        contrastSlider = createSlider(-100, 100, 0);
        saturationSlider = createSlider(-100, 100, 0);

        setLayout(new BorderLayout());

        // Create the adjustments panel
        JPanel adjustmentsPanel = createAdjustmentsPanel();
        add(adjustmentsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetAdjustments());

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applyChanges());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            cancelChanges();
            dispose();
        });

        buttonsPanel.add(resetButton);
        buttonsPanel.add(applyButton);
        buttonsPanel.add(cancelButton);
        add(buttonsPanel, BorderLayout.SOUTH);

        // Real-time preview
        opacitySlider.addChangeListener(e -> updatePreview());
        brightnessSlider.addChangeListener(e -> updatePreview());
        contrastSlider.addChangeListener(e -> updatePreview());
        saturationSlider.addChangeListener(e -> updatePreview());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cancelChanges();
            }
        });

        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private JPanel createAdjustmentsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Add all controls with proper spacing
        panel.add(createControlPanel("Opacity", opacitySlider), gbc);

        gbc.gridy++;
        panel.add(createControlPanel("Brightness", brightnessSlider), gbc);

        gbc.gridy++;
        panel.add(createControlPanel("Contrast", contrastSlider), gbc);

        gbc.gridy++;
        panel.add(createControlPanel("Saturation", saturationSlider), gbc);

        // Add some padding at the bottom
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalStrut(5), gbc);

        return panel;
    }

    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, value);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JPanel createControlPanel(String labelText, JSlider slider) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Label configuration
        JLabel label = new JLabel(labelText);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 5);  // Right padding
        gbc.weightx = 0.0;
        panel.add(label, gbc);

        // Slider configuration
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 5);  // Right padding
        panel.add(slider, gbc);

        // Value field configuration
        JTextField valueField = new JTextField(4);
        valueField.setText(String.valueOf(slider.getValue()));
        valueField.setHorizontalAlignment(SwingConstants.RIGHT);

        // Synchronize text field with slider
        slider.addChangeListener(e -> valueField.setText(String.valueOf(slider.getValue())));

        // Allow text field to control slider
        valueField.addActionListener(e -> {
            try {
                int value = Integer.parseInt(valueField.getText());
                value = Math.max(slider.getMinimum(), Math.min(slider.getMaximum(), value));
                slider.setValue(value);
            } catch (NumberFormatException ex) {
                valueField.setText(String.valueOf(slider.getValue()));
            }
        });

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 0, 0, 0);  // No padding on the right
        panel.add(valueField, gbc);

        return panel;
    }

    /**
     * Updates the preview image based on the current slider values.
     * Uses the toolCanvas to show the preview.
     */
    private void updatePreview() {
        // Get or create toolCanvas
        BufferedImage toolCanvas = canvas.getToolCanvas();
        if (toolCanvas == null) {
            // Create a new toolCanvas if it doesn't exist
            int width = layer.getImage().getWidth();
            int height = layer.getImage().getHeight();
            toolCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            canvas.setToolCanvas(toolCanvas);
        }

        // Clear the toolCanvas
        Graphics2D g2d = toolCanvas.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, toolCanvas.getWidth(), toolCanvas.getHeight());
        g2d.dispose();

        // Reset preview image to original
        Graphics2D previewG2d = previewImage.createGraphics();
        previewG2d.drawImage(originalImage, 0, 0, null);
        previewG2d.dispose();

        // Apply adjustments to the preview image
        if (brightnessSlider.getValue() != 0) {
            adjustBrightness(previewImage, brightnessSlider.getValue());
        }

        if (contrastSlider.getValue() != 0) {
            adjustContrast(previewImage, contrastSlider.getValue());
        }

        if (saturationSlider.getValue() != 0) {
            adjustSaturation(previewImage, saturationSlider.getValue());
        }

        // Apply opacity
        adjustOpacity(previewImage, opacitySlider.getValue());

        // Get current layer index
        int currentLayerIndex = canvas.getCurrentLayerIndex();
        List<Layer> layers = canvas.getLayers();

        // Store visibility of the current layer
        boolean wasVisible = layer.isVisible();

        // Temporarily hide the current layer so we can show the preview in its place
        layer.setVisible(false);

        // Copy the state of other layers to the toolCanvas first
        // This gives us a composite of all layers except the current one
        g2d = toolCanvas.createGraphics();

        // Draw all visible layers up to the current layer
        for (int i = 0; i < currentLayerIndex; i++) {
            Layer l = layers.get(i);
            if (l.isVisible()) {
                g2d.drawImage(l.getImage(), 0, 0, null);
            }
        }

        // Draw the preview image in place of the current layer
        g2d.drawImage(previewImage, 0, 0, null);

        // Draw all layers above current layer
        for (int i = currentLayerIndex + 1; i < layers.size(); i++) {
            Layer l = layers.get(i);
            if (l.isVisible()) {
                g2d.drawImage(l.getImage(), 0, 0, null);
            }
        }

        g2d.dispose();

        // Restore the visibility of the current layer
        layer.setVisible(wasVisible);

        // Trigger repaint to show preview
        canvas.repaint();
    }

    /**
     * Resets all adjustments to their default values.
     */
    private void resetAdjustments() {
        opacitySlider.setValue(100);
        brightnessSlider.setValue(0);
        contrastSlider.setValue(0);
        saturationSlider.setValue(0);
        updatePreview();
    }

    /**
     * Applies the changes made in the dialog to the layer.
     * Saves the current state to the undo stack before applying.
     */
    private void applyChanges() {
        // Save current state to undo stack
        canvas.saveToUndoStack();

        // Apply the preview changes to the actual layer
        Graphics2D g2d = layer.getImage().createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, layer.getImage().getWidth(), layer.getImage().getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(previewImage, 0, 0, null);
        g2d.dispose();

        // Clear the toolCanvas
        canvas.setToolCanvas(null);

        canvas.repaint();
        canvas.notifyLayersChanged();
        dispose();
    }

    /**
     * Cancels the changes made in the dialog and restores the original image.
     */
    private void cancelChanges() {
        // Clear toolCanvas
        canvas.setToolCanvas(null);
        canvas.repaint();
    }

    /**
     * Adjusts the brightness of the image.
     *
     * @param image The image to adjust
     * @param brightness The brightness value (-100 to 100)
     */
    private void adjustBrightness(BufferedImage image, int brightness) {
        float factor = 1.0f + (brightness / 100.0f);
        applyRGBFilter(image, (r, g, b, a) -> {
            int newR = clamp((int) (r * factor));
            int newG = clamp((int) (g * factor));
            int newB = clamp((int) (b * factor));
            return (a << 24) | (newR << 16) | (newG << 8) | newB;
        });
    }

    /**
     * Adjusts the contrast of the image.
     *
     * @param image The image to adjust
     * @param contrast The contrast value (-100 to 100)
     */
    private void adjustContrast(BufferedImage image, int contrast) {
        float factor = (259f * (contrast + 255)) / (255f * (259 - contrast));
        applyRGBFilter(image, (r, g, b, a) -> {
            int newR = clamp((int) (factor * (r - 128) + 128));
            int newG = clamp((int) (factor * (g - 128) + 128));
            int newB = clamp((int) (factor * (b - 128) + 128));
            return (a << 24) | (newR << 16) | (newG << 8) | newB;
        });
    }

    /**
     * Adjusts the saturation of the image.
     *
     * @param image The image to adjust
     * @param saturation The saturation value (-100 to 100)
     */
    private void adjustSaturation(BufferedImage image, int saturation) {
        float factor = 1.0f + (saturation / 100.0f);
        applyRGBFilter(image, (r, g, b, a) -> {
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            hsb[1] = clamp(hsb[1] * factor, 0f, 1f); // Adjust saturation
            int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            return (a << 24) | (rgb & 0x00FFFFFF);
        });
    }

    /**
     * Adjusts the opacity of the image.
     *
     * @param image The image to adjust
     * @param opacity The opacity value (0 to 100)
     */
    private void adjustOpacity(BufferedImage image, int opacity) {
        float factor = opacity / 100.0f;
        applyRGBFilter(image, (r, g, b, a) -> {
            // If pixel was already completely transparent, keep it that way
            if (a == 0) return 0;

            int newA = (int) (a * factor);
            return (newA << 24) | (r << 16) | (g << 8) | b;
        });
    }

    /**
     * Applies a filter to each pixel of the image.
     *
     * @param image The image to filter
     * @param filter The filter to apply
     */
    private void applyRGBFilter(BufferedImage image, PixelFilter filter) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;

                // Apply the filter to all pixels, even transparent ones
                // This allows partially transparent pixels to be processed correctly
                if (a > 0) { // Only process pixels with some visibility
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    image.setRGB(x, y, filter.apply(r, g, b, a));
                }
                // Completely transparent pixels (a = 0) remain unchanged
            }
        }
    }

    private interface PixelFilter {
        int apply(int r, int g, int b, int a);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Creates a deep copy of the given image.
     *
     * @param source The source image to copy
     * @return A new BufferedImage that is a deep copy of the source
     */
    private BufferedImage deepCopyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}