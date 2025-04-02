package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.PaletteGenerator;
import com.esgdev.sparkpaint.io.PaletteManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class PaletteGeneratorDialog extends JDialog implements PaletteGenerator.PaletteGenerationProgressListener {
    private final DrawingCanvas canvas;
    private JSpinner colorCountSpinner;
    private JProgressBar progressBar;
    private JPanel palettePreviewPanel;
    private List<Color> generatedPalette;
    private JButton applyButton;

    public PaletteGeneratorDialog(JFrame parent, DrawingCanvas canvas) {
        super(parent, "Generate Palette from Image", true);
        this.canvas = canvas;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Options panel with flow layout for all controls in a single line
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        // Color count controls
        optionsPanel.add(new JLabel("Number of colors:"));
        colorCountSpinner = new JSpinner(new SpinnerNumberModel(16, 2, 64, 1));
        optionsPanel.add(colorCountSpinner);

        // Add some spacing
        optionsPanel.add(Box.createHorizontalStrut(10));

        // Palette style controls
        optionsPanel.add(new JLabel("Palette style:"));
        JComboBox<PaletteGenerator.PaletteStyle> styleComboBox = new JComboBox<>(
                PaletteGenerator.PaletteStyle.values());
        styleComboBox.setSelectedItem(PaletteGenerator.PaletteStyle.BALANCED);
        optionsPanel.add(styleComboBox);

        // Add some spacing
        optionsPanel.add(Box.createHorizontalStrut(10));

        // Generate button directly in the options panel
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e ->
                generatePalette(e, (PaletteGenerator.PaletteStyle) styleComboBox.getSelectedItem()));
        optionsPanel.add(generateButton);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        // Preview panel
        palettePreviewPanel = new JPanel();
        palettePreviewPanel.setBorder(BorderFactory.createTitledBorder("Generated Palette"));
        palettePreviewPanel.setLayout(new GridBagLayout());

        // Button panel
        JPanel buttonPanel = new JPanel();
        applyButton = new JButton("Apply");
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> {
            applyPalette();
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        // Assemble dialog - now with a simplified top section
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(optionsPanel, BorderLayout.NORTH);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(palettePreviewPanel);
        scrollPane.setPreferredSize(new Dimension(0, 160));
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    private void generatePalette(ActionEvent e, PaletteGenerator.PaletteStyle style) {
        palettePreviewPanel.removeAll();
        applyButton.setEnabled(false);

        int colorCount = (int) colorCountSpinner.getValue();
        progressBar.setValue(0);
        progressBar.setString("Processing...");

        SwingWorker<List<Color>, Void> worker = new SwingWorker<List<Color>, Void>() {
            @Override
            protected List<Color> doInBackground() {
                PaletteGenerator generator = new PaletteGenerator();
                generator.setProgressListener(PaletteGeneratorDialog.this);
                return generator.generatePaletteFromCanvas(canvas, colorCount, style);
            }

            @Override
            protected void done() {
                try {
                    generatedPalette = get();
                    updatePalettePreview();
                    progressBar.setString("Complete");
                    applyButton.setEnabled(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PaletteGeneratorDialog.this,
                            "Error generating palette: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    progressBar.setString("Error");
                }
            }
        };

        worker.execute();
    }

    private void updatePalettePreview() {
        palettePreviewPanel.removeAll();

        if (generatedPalette != null && !generatedPalette.isEmpty()) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.BOTH;

            // Calculate appropriate number of columns based on panel width
            int columns = Math.max(1, palettePreviewPanel.getWidth() / 40);

            for (int i = 0; i < generatedPalette.size(); i++) {
                Color color = generatedPalette.get(i);
                JPanel swatch = new JPanel();
                swatch.setPreferredSize(new Dimension(30, 30));
                swatch.setBackground(color);
                swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                String rgbHex = String.format("#%02X%02X%02X",
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue());
                swatch.setToolTipText(rgbHex);

                // Set GridBagConstraints
                gbc.gridx = i % columns;
                gbc.gridy = i / columns;

                palettePreviewPanel.add(swatch, gbc);
            }
        } else {
            palettePreviewPanel.add(new JLabel("No colors generated."));
        }

        palettePreviewPanel.revalidate();
        palettePreviewPanel.repaint();
    }

    private void applyPalette() {
        if (generatedPalette != null && !generatedPalette.isEmpty()) {
            try {
                canvas.setActivePalette(generatedPalette);
                JOptionPane.showMessageDialog(this,
                        "Palette applied successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error applying palette: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onProgressUpdate(int progress, int max) {
        SwingUtilities.invokeLater(() -> {
            int percentage = (int) ((double) progress / max * 100);
            progressBar.setValue(percentage);
            progressBar.setString(progress < max ?
                    String.format("Processing... %d%%", percentage) : "Complete");
        });
    }
}