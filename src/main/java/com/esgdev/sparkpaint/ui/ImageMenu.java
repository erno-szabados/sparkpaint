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
                    filePath != null ? filePath : "Untitled");
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

        // Maintain aspect ratio checkbox
        JPanel ratioPanel = new JPanel();
        JCheckBox maintainRatioBox = new JCheckBox("Maintain aspect ratio");
        ratioPanel.add(maintainRatioBox);

        // Add change listeners for aspect ratio maintenance
        double aspectRatio = (double) currentImage.getWidth() / currentImage.getHeight();
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

            canvas.createNewCanvas(newWidth, newHeight, canvas.getCanvasBackground());

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

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Scale inputs
        JPanel scalePanel = new JPanel();
        JLabel scaleLabel = new JLabel("Scale (%):");
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
        JLabel dimensionsLabel = new JLabel(String.format("Current: %dx%d", currentImage.getWidth(), currentImage.getHeight()));
        previewPanel.add(dimensionsLabel);

        // Add change listeners for aspect ratio maintenance
        widthScaleSpinner.addChangeListener(change -> {
            if (maintainRatioBox.isSelected()) {
                heightScaleSpinner.setValue(widthScaleSpinner.getValue());
            }
            updateScalePreview(currentImage, dimensionsLabel, widthScaleSpinner, heightScaleSpinner);
        });

        heightScaleSpinner.addChangeListener(change -> {
            if (maintainRatioBox.isSelected()) {
                widthScaleSpinner.setValue(heightScaleSpinner.getValue());
            }
            updateScalePreview(currentImage, dimensionsLabel, widthScaleSpinner, heightScaleSpinner);
        });

        panel.add(scalePanel);
        panel.add(ratioPanel);
        panel.add(previewPanel);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel,
                "Scale Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int widthScale = (Integer) widthScaleSpinner.getValue();
            int heightScale = (Integer) heightScaleSpinner.getValue();
            int newWidth = currentImage.getWidth() * widthScale / 100;
            int newHeight = currentImage.getHeight() * heightScale / 100;

            canvas.createNewCanvas(newWidth, newHeight, canvas.getCanvasBackground());

            Graphics2D g = (Graphics2D) canvas.getImage().getGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(currentImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            canvas.repaint();
            canvas.saveToUndoStack();
        }
    }

    private void updateScalePreview(BufferedImage image, JLabel label, JSpinner widthSpinner, JSpinner heightSpinner) {
        int widthScale = (Integer) widthSpinner.getValue();
        int heightScale = (Integer) heightSpinner.getValue();
        int newWidth = image.getWidth() * widthScale / 100;
        int newHeight = image.getHeight() * heightScale / 100;
        label.setText(String.format("New: %dx%d", newWidth, newHeight));
    }
}