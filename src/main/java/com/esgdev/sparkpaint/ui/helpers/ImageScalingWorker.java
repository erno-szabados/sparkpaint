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
    private final int blockHeight = 240; // Height of each processing block

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
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setResizable(false);
    }

    @Override
    protected List<Layer> doInBackground() {
        List<Layer> scaledLayers = new ArrayList<>();
        int totalLayers = originalLayers.size();

        for (int i = 0; i < totalLayers; i++) {
            Layer layer = originalLayers.get(i);
            Layer newLayer = new Layer(newWidth, newHeight);
            newLayer.setVisible(layer.isVisible());
            newLayer.setName(layer.getName());

            BufferedImage originalImage = layer.getImage();
            BufferedImage scaledImage = newLayer.getImage();
            Graphics2D g = scaledImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int numBlocks = (int) Math.ceil((double) originalImage.getHeight() / blockHeight);

            for (int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
                int yStart = blockIndex * blockHeight;
                int yEnd = Math.min(yStart + blockHeight, originalImage.getHeight());

                // Calculate the corresponding scaled y coordinates
                int scaledYStart = (int) Math.round((double) yStart * newHeight / originalImage.getHeight());
                int scaledYEnd = (int) Math.round((double) yEnd * newHeight / originalImage.getHeight());

                int blockWidth = originalImage.getWidth();
                int scaledBlockWidth = newWidth;
                int scaledBlockHeight = scaledYEnd - scaledYStart;

                // Extract the block from the original image
                BufferedImage originalBlock = originalImage.getSubimage(0, yStart, blockWidth, yEnd - yStart);

                // Scale the block
                Image scaledBlockImage = originalBlock.getScaledInstance(scaledBlockWidth, scaledBlockHeight, Image.SCALE_SMOOTH);
                BufferedImage scaledBlock = new BufferedImage(scaledBlockWidth, scaledBlockHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D scaledG = scaledBlock.createGraphics();
                scaledG.drawImage(scaledBlockImage, 0, 0, null);
                scaledG.dispose();

                // Draw the scaled block to the new image
                g.drawImage(scaledBlock, 0, scaledYStart, null);

                // Update progress
                int progress = (int) ((double) (i * numBlocks + blockIndex + 1) / (totalLayers * numBlocks) * 100);
                setProgress(progress);
                publish(progress);
            }

            g.dispose();
            scaledLayers.add(newLayer);
        }

        return scaledLayers;
    }

    @Override
    protected void process(List<Integer> chunks) {
        int latestProgress = chunks.get(chunks.size() - 1);
        progressBar.setValue(latestProgress);
        progressBar.setString(latestProgress + "%");
    }

    @Override
    protected void done() {
        try {
            List<Layer> scaledLayers = get();
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
        progressDialog.setVisible(true);
        execute();
    }
}