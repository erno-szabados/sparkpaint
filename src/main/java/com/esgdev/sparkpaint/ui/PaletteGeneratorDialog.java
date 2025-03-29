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
        setPreferredSize(new Dimension(400, 300));

        // Options panel
        JPanel optionsPanel = new JPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
        optionsPanel.add(new JLabel("Number of colors:"));
        colorCountSpinner = new JSpinner(new SpinnerNumberModel(16, 2, 64, 1));
        optionsPanel.add(colorCountSpinner);

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(this::generatePalette);
        optionsPanel.add(generateButton);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        // Preview panel
        palettePreviewPanel = new JPanel();
        palettePreviewPanel.setBorder(BorderFactory.createTitledBorder("Generated Palette"));
        palettePreviewPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

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

        // Assemble dialog
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(optionsPanel, BorderLayout.NORTH);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(palettePreviewPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getParent());
    }

    private void generatePalette(ActionEvent e) {
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
                return generator.generatePaletteFromCanvas(canvas, colorCount);
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
            for (Color color : generatedPalette) {
                JPanel swatch = new JPanel();
                swatch.setPreferredSize(new Dimension(30, 30));
                swatch.setBackground(color);
                swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                palettePreviewPanel.add(swatch);
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