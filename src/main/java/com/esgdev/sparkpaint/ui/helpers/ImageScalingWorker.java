package com.esgdev.sparkpaint.ui.helpers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImageScalingWorker extends SwingWorker<List<Layer>, Integer> {
    private final DrawingCanvas canvas;
    private final List<Layer> originalLayers;
    private final int newWidth;
    private final int newHeight;
    private final JProgressBar progressBar;
    private final JDialog progressDialog;

    public ImageScalingWorker(JFrame parent, DrawingCanvas canvas, int newWidth, int newHeight) {
        this.canvas = canvas;
        this.originalLayers = canvas.getLayers();
        this.newWidth = newWidth;
        this.newHeight = newHeight;

        // Create progress dialog
        progressDialog = new JDialog(parent, "Scaling Image...", false); // Changed to non-modal
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Scaling image. Please wait..."), BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);

        progressDialog.add(panel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setResizable(false);
    }

    @Override
    protected List<Layer> doInBackground() {
        List<Layer> scaledLayers = new ArrayList<>();
        int totalLayers = originalLayers.size();

        for (int i = 0; i < totalLayers; i++) {
            Layer layer = originalLayers.get(i);

            // Create a new scaled layer
            Layer newLayer = new Layer(newWidth, newHeight);
            newLayer.setVisible(layer.isVisible());
            newLayer.setName(layer.getName());

            BufferedImage originalImage = layer.getImage();
            BufferedImage scaledImage = newLayer.getImage();

            // Scale the image using bilinear interpolation
            Graphics2D g = scaledImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            scaledLayers.add(newLayer);

            // Update progress
            int progress = (i + 1) * 100 / totalLayers;
            setProgress(progress);
            publish(progress);
        }

        return scaledLayers;
    }

    @Override
    protected void process(List<Integer> chunks) {
        // Update progress bar with the latest progress value
        int latestProgress = chunks.get(chunks.size() - 1);
        progressBar.setValue(latestProgress);
        progressBar.setString(latestProgress + "%");
    }

    @Override
    protected void done() {
        try {
            List<Layer> scaledLayers = get();
            // Update canvas with new layers
            canvas.saveToUndoStack();
            canvas.setLayers(scaledLayers);
            canvas.setPreferredSize(new Dimension(newWidth, newHeight));
            canvas.revalidate();
            canvas.repaint();
        } catch (InterruptedException | ExecutionException e) {
            JOptionPane.showMessageDialog(progressDialog,
                    "Error scaling image: " + e.getMessage(),
                    "Scaling Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            progressDialog.dispose();
        }
    }

    public void start() {
        // Show dialog first, then execute worker
        progressDialog.setVisible(true);
        execute();
    }
}