package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageMenu class represents the "Image" menu in the application.
 * It provides options to view image information, resize, scale, and crop images.
 */
public class ImageMenu extends JMenu {
    private final DrawingCanvas canvas;
    private final MainFrame mainFrame;

    /**
     * Constructor for ImageMenu.
     *
     * @param mainFrame The main frame of the application.
     */
    public ImageMenu(MainFrame mainFrame) {
        super("Image");
        this.mainFrame = mainFrame;
        this.canvas = mainFrame.getCanvas();

        // Create and add Info item
        JMenuItem infoItem = new JMenuItem("Image Info");
        infoItem.setMnemonic('I');
        infoItem.addActionListener(this::handleInfo);
        infoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(infoItem);

        // Add separator
        addSeparator();

        // Create and add Resize item
        JMenuItem resizeItem = new JMenuItem("Resize...");
        resizeItem.setMnemonic('R');
        resizeItem.addActionListener(this::handleResize);
        resizeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(resizeItem);

        // Create and add Scale item
        JMenuItem scaleItem = new JMenuItem("Scale...");
        scaleItem.setMnemonic('S');
        scaleItem.addActionListener(this::handleScale);
        scaleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(scaleItem);

        // Create and add Crop to Selection item
        JMenuItem cropItem = new JMenuItem("Crop to Selection");
        cropItem.setMnemonic('C');
        cropItem.addActionListener(this::handleCrop);
        cropItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(cropItem);
    }

    private void handleInfo(ActionEvent e) {
        String filePath = canvas.getCurrentFilePath();
        List<Layer> layers = canvas.getLayers();
        int currentLayerIndex = canvas.getCurrentLayerIndex();

        if (layers != null && !layers.isEmpty()) {
            BufferedImage firstLayer = layers.get(0).getImage();
            String message = String.format("Image Size: %dx%d pixels\n", firstLayer.getWidth(), firstLayer.getHeight()) +
                    String.format("Number of Layers: %d\n", layers.size()) +
                    String.format("Active Layer: %d\n", currentLayerIndex + 1) +
                    String.format("File Path: %s", filePath != null ? filePath : "Untitled");

            JOptionPane.showMessageDialog(mainFrame, message, "Image Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Image Information", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleResize(ActionEvent e) {
        List<Layer> layers = canvas.getLayers();
        if (layers == null || layers.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Resize", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BufferedImage currentLayerImage = layers.get(0).getImage();
        int originalWidth = currentLayerImage.getWidth();
        int originalHeight = currentLayerImage.getHeight();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Width input
        JPanel widthPanel = new JPanel();
        JLabel widthLabel = new JLabel("Width:");
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(originalWidth, 1, 10000, 1));
        widthPanel.add(widthLabel);
        widthPanel.add(widthSpinner);

        // Height input
        JPanel heightPanel = new JPanel();
        JLabel heightLabel = new JLabel("Height:");
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(originalHeight, 1, 10000, 1));
        heightPanel.add(heightLabel);
        heightPanel.add(heightSpinner);

        // Maintain aspect ratio checkbox
        JPanel ratioPanel = new JPanel();
        JCheckBox maintainRatioBox = new JCheckBox("Maintain aspect ratio");
        ratioPanel.add(maintainRatioBox);

        // Add change listeners for aspect ratio maintenance
        double aspectRatio = (double) originalWidth / originalHeight;
        widthSpinner.addChangeListener(e1 -> {
            if (maintainRatioBox.isSelected()) {
                int width = (Integer) widthSpinner.getValue();
                heightSpinner.setValue((int) (width / aspectRatio));
            }
        });

        heightSpinner.addChangeListener(e1 -> {
            if (maintainRatioBox.isSelected()) {
                int height = (Integer) heightSpinner.getValue();
                widthSpinner.setValue((int) (height * aspectRatio));
            }
        });

        panel.add(widthPanel);
        panel.add(heightPanel);
        panel.add(ratioPanel);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel,
                "Resize Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int newWidth = (Integer) widthSpinner.getValue();
            int newHeight = (Integer) heightSpinner.getValue();

            // Save for undo
            canvas.saveToUndoStack();

            // Create resized layers
            List<Layer> resizedLayers = new ArrayList<>();
            for (Layer layer : layers) {
                Layer newLayer = new Layer(newWidth, newHeight);
                Graphics2D g = newLayer.getImage().createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // Center the original image in the new canvas
                int x = (newWidth - originalWidth) / 2;
                int y = (newHeight - originalHeight) / 2;
                g.drawImage(layer.getImage(), x, y, null);
                g.dispose();

                newLayer.setVisible(layer.isVisible());
                resizedLayers.add(newLayer);
            }

            // Update canvas with new layers
            canvas.setLayers(resizedLayers);
            canvas.saveToUndoStack();
            canvas.setPreferredSize(new Dimension(newWidth, newHeight));
            canvas.revalidate();
            canvas.repaint();
        }
    }

    private void handleScale(ActionEvent e) {
        List<Layer> layers = canvas.getLayers();
        if (layers == null || layers.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Scale", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BufferedImage currentLayerImage = layers.get(0).getImage();
        int originalWidth = currentLayerImage.getWidth();
        int originalHeight = currentLayerImage.getHeight();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Scale inputs
        JPanel scalePanel = new JPanel();
        JSpinner widthScaleSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        JSpinner heightScaleSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        scalePanel.add(new JLabel("Width %:"));
        scalePanel.add(widthScaleSpinner);
        scalePanel.add(new JLabel("Height %:"));
        scalePanel.add(heightScaleSpinner);

        // Maintain aspect ratio checkbox
        JPanel ratioPanel = new JPanel();
        JCheckBox maintainRatioBox = new JCheckBox("Maintain aspect ratio");
        maintainRatioBox.setSelected(true);
        ratioPanel.add(maintainRatioBox);

        // Preview dimensions
        JPanel previewPanel = new JPanel();
        JLabel dimensionsLabel = new JLabel(String.format("Current: %dx%d", originalWidth, originalHeight));
        previewPanel.add(dimensionsLabel);

        // Add change listeners for aspect ratio maintenance
        widthScaleSpinner.addChangeListener(change -> {
            if (maintainRatioBox.isSelected()) {
                heightScaleSpinner.setValue(widthScaleSpinner.getValue());
            }
            updateScalePreview(originalWidth, originalHeight, dimensionsLabel, widthScaleSpinner, heightScaleSpinner);
        });

        heightScaleSpinner.addChangeListener(change -> {
            if (maintainRatioBox.isSelected()) {
                widthScaleSpinner.setValue(heightScaleSpinner.getValue());
            }
            updateScalePreview(originalWidth, originalHeight, dimensionsLabel, widthScaleSpinner, heightScaleSpinner);
        });

        panel.add(scalePanel);
        panel.add(ratioPanel);
        panel.add(previewPanel);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel,
                "Scale Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int widthScale = (Integer) widthScaleSpinner.getValue();
            int heightScale = (Integer) heightScaleSpinner.getValue();
            int newWidth = originalWidth * widthScale / 100;
            int newHeight = originalHeight * heightScale / 100;

            // Save for undo
            canvas.saveToUndoStack();

            // Create scaled layers
            List<Layer> scaledLayers = new ArrayList<>();
            for (Layer layer : layers) {
                Layer newLayer = new Layer(newWidth, newHeight);
                Graphics2D g = newLayer.getImage().createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(layer.getImage(), 0, 0, newWidth, newHeight, null);
                g.dispose();

                newLayer.setVisible(layer.isVisible());
                scaledLayers.add(newLayer);
            }

            // Update canvas with new layers
            canvas.setLayers(scaledLayers);
            canvas.setPreferredSize(new Dimension(newWidth, newHeight));
            canvas.revalidate();
            canvas.repaint();
        }
    }

    private void handleCrop(ActionEvent e) {
        List<Layer> layers = canvas.getLayers();
        //SelectionManager selectionManager = canvas.getSelectionManager();

        if (canvas.getSelection() == null) {
            JOptionPane.showMessageDialog(mainFrame, "No selection available.", "Crop to Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Rectangle selection = canvas.getSelection().getBounds();

        if (layers == null || layers.isEmpty() || selection == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image or selection available.", "Crop to Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Save for undo
        canvas.saveToUndoStack();

        // Create cropped layers
        List<Layer> croppedLayers = new ArrayList<>();
        for (Layer layer : layers) {
            // Create a new layer with the selection dimensions
            Layer newLayer = new Layer(selection.width, selection.height);
            newLayer.setVisible(layer.isVisible());

            // Calculate the source coordinates (the part of the original image we're copying from)
            int srcX = Math.max(0, selection.x);
            int srcY = Math.max(0, selection.y);

            // Calculate the width and height to copy
            int copyWidth = Math.min(selection.width, layer.getImage().getWidth() - srcX);
            int copyHeight = Math.min(selection.height, layer.getImage().getHeight() - srcY);

            // Skip if there's nothing to copy
            if (copyWidth <= 0 || copyHeight <= 0) {
                // Just add the empty layer
                croppedLayers.add(newLayer);
                continue;
            }

            // Calculate the destination coordinates in the new layer
            // This accounts for selections that start outside the image (negative x,y)
            int dstX = Math.max(0, -selection.x);
            int dstY = Math.max(0, -selection.y);

            // Draw the cropped portion to the new layer
            Graphics2D g = newLayer.getImage().createGraphics();
            g.drawImage(
                    layer.getImage().getSubimage(srcX, srcY, copyWidth, copyHeight),
                    dstX, dstY, null
            );
            g.dispose();

            croppedLayers.add(newLayer);
        }

        // Update canvas with new layers
        canvas.setLayers(croppedLayers);
        canvas.clearSelection();
        canvas.setPreferredSize(new Dimension(selection.width, selection.height));
        canvas.revalidate();
        canvas.repaint();
    }

    private void updateScalePreview(int originalWidth, int originalHeight, JLabel label, JSpinner widthSpinner, JSpinner heightSpinner) {
        int widthScale = (Integer) widthSpinner.getValue();
        int heightScale = (Integer) heightSpinner.getValue();
        int newWidth = originalWidth * widthScale / 100;
        int newHeight = originalHeight * heightScale / 100;
        label.setText(String.format("New: %dx%d", newWidth, newHeight));
    }
}