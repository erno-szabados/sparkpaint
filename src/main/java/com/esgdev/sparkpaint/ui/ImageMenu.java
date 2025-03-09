package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class ImageMenu extends JMenu {
    private final DrawingCanvas canvas;
    private final MainFrame mainFrame;
    private final JMenuItem infoItem;
    private final JMenuItem resizeItem;
    private final JMenuItem scaleItem;

    public ImageMenu(MainFrame mainFrame) {
        super("Image");
        this.mainFrame = mainFrame;
        this.canvas = mainFrame.getCanvas();

        // Create and add Info item
        infoItem = new JMenuItem("Image Info");
        infoItem.setMnemonic('I');
        infoItem.addActionListener(this::handleInfo);
        infoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(infoItem);

        // Add separator
        addSeparator();

        // Create and add Resize item
        resizeItem = new JMenuItem("Resize...");
        resizeItem.setMnemonic('R');
        resizeItem.addActionListener(this::handleResize);
        resizeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(resizeItem);

        // Create and add Scale item
        scaleItem = new JMenuItem("Scale...");
        scaleItem.setMnemonic('S');
        scaleItem.addActionListener(this::handleScale);
        scaleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        add(scaleItem);
    }

    private void handleInfo(ActionEvent e) {
        BufferedImage image = (BufferedImage) canvas.getImage();
        String filePath = canvas.getCurrentFilePath();

        if (image != null) {
            String message = String.format("Image Size: %dx%d pixels\nFile Path: %s",
                                            image.getWidth(), image.getHeight(),
                                            filePath != null ? filePath : "Unsaved");
            JOptionPane.showMessageDialog(mainFrame, message, "Image Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Image Information", JOptionPane.WARNING_MESSAGE);
        }
    }

   private void handleResize(ActionEvent e) {
        BufferedImage currentImage = (BufferedImage) canvas.getImage();
        if (currentImage == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Resize", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create dialog panel with width and height inputs
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Width input
        JPanel widthPanel = new JPanel();
        JLabel widthLabel = new JLabel("Width:");
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(currentImage.getWidth(), 1, 10000, 1));
        widthPanel.add(widthLabel);
        widthPanel.add(widthSpinner);

        // Height input
        JPanel heightPanel = new JPanel();
        JLabel heightLabel = new JLabel("Height:");
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(currentImage.getHeight(), 1, 10000, 1));
        heightPanel.add(heightLabel);
        heightPanel.add(heightSpinner);

        panel.add(widthPanel);
        panel.add(heightPanel);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel,
                "Resize Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int newWidth = (Integer) widthSpinner.getValue();
            int newHeight = (Integer) heightSpinner.getValue();

            // Create new canvas with current background
            canvas.createNewCanvas(newWidth, newHeight, canvas.getCanvasBackground());

            // Draw the old image centered on the new canvas
            Graphics2D g = (Graphics2D) canvas.getImage().getGraphics();
            int x = (newWidth - currentImage.getWidth()) / 2;
            int y = (newHeight - currentImage.getHeight()) / 2;
            g.drawImage(currentImage, x, y, null);
            g.dispose();

            canvas.repaint();
            canvas.saveToUndoStack();
        }
    }

    private void handleScale(ActionEvent e) {
        BufferedImage currentImage = (BufferedImage) canvas.getImage();
        if (currentImage == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Scale", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create dialog panel with scale percentage input
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Scale percentage input
        JPanel scalePanel = new JPanel();
        JLabel scaleLabel = new JLabel("Scale (%):");
        JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        scalePanel.add(scaleLabel);
        scalePanel.add(scaleSpinner);

        // Preview dimensions
        JPanel previewPanel = new JPanel();
        JLabel dimensionsLabel = new JLabel(String.format("Current: %dx%d", currentImage.getWidth(), currentImage.getHeight()));
        previewPanel.add(dimensionsLabel);

        // Update preview when scale changes
        scaleSpinner.addChangeListener(change -> {
            int scale = (Integer) scaleSpinner.getValue();
            int newWidth = currentImage.getWidth() * scale / 100;
            int newHeight = currentImage.getHeight() * scale / 100;
            dimensionsLabel.setText(String.format("New: %dx%d", newWidth, newHeight));
        });

        panel.add(scalePanel);
        panel.add(previewPanel);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel,
                "Scale Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int scale = (Integer) scaleSpinner.getValue();
            int newWidth = currentImage.getWidth() * scale / 100;
            int newHeight = currentImage.getHeight() * scale / 100;

            // Create new canvas with scaled dimensions
            canvas.createNewCanvas(newWidth, newHeight, canvas.getCanvasBackground());

            // Scale and draw the image
            Graphics2D g = (Graphics2D) canvas.getImage().getGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(currentImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            canvas.repaint();
            canvas.saveToUndoStack();
        }
    }
}