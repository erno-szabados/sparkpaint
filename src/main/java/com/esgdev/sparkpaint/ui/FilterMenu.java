package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.filters.OrderedDitheringFilter;
import com.esgdev.sparkpaint.engine.layer.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * FilterMenu provides image processing filters that can be applied to the current layer.
 */
public class FilterMenu extends JMenu {
    private final DrawingCanvas canvas;
    private final MainFrame mainFrame;

    /**
     * Constructor for FilterMenu.
     *
     * @param mainFrame The main frame of the application.
     */
    public FilterMenu(MainFrame mainFrame) {
        super("Filter");
        this.mainFrame = mainFrame;
        this.canvas = mainFrame.getCanvas();

        setMnemonic('F');

        // Ordered Dithering filter
        JMenuItem ditheringItem = new JMenuItem("Ordered Dithering");
        ditheringItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
        ditheringItem.addActionListener(this::handleOrderedDithering);
        add(ditheringItem);

        // Add more filter items here as they are implemented
    }

    /**
     * Handles the ordered dithering filter operation.
     */
    private void handleOrderedDithering(ActionEvent e) {
        List<Layer> layers = canvas.getLayers();
        int currentLayerIndex = canvas.getCurrentLayerIndex();

        if (layers == null || layers.isEmpty() || currentLayerIndex < 0 || currentLayerIndex >= layers.size()) {
            JOptionPane.showMessageDialog(mainFrame, "No active layer to apply filter to.",
                    "Ordered Dithering", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Layer currentLayer = layers.get(currentLayerIndex);

        // Check if we have a palette
        List<Color> palette = canvas.getPaletteManager().getActivePalette();
        if (palette == null || palette.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(mainFrame,
                    "No color palette is currently active. Would you like to use basic palette (black and white)?",
                    "Ordered Dithering", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                // Create a basic black and white palette
                palette = Arrays.asList(Color.BLACK, Color.WHITE);
            } else {
                mainFrame.setStatusMessage("Dithering cancelled - no palette available");
                return;
            }
        }

        // Save state for undo
        canvas.saveToUndoStack();

        // Apply the dithering filter
        BufferedImage originalImage = currentLayer.getImage();
        BufferedImage ditheredImage = OrderedDitheringFilter.apply(originalImage, palette);

        // Create a new layer with the dithered image
        Layer newLayer = new Layer(ditheredImage.getWidth(), ditheredImage.getHeight());
        Graphics2D g2d = newLayer.getImage().createGraphics();
        g2d.drawImage(ditheredImage, 0, 0, null);
        g2d.dispose();

        // Replace the current layer with the new layer
        newLayer.setVisible(currentLayer.isVisible());
        layers.set(currentLayerIndex, newLayer);

        // Update canvas
        canvas.setLayers(layers);
        canvas.repaint();
        mainFrame.setStatusMessage("Applied ordered dithering to current layer");
    }
}