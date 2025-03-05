package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
        // TODO: Show dialog with image information (dimensions, color depth, file size if saved)
        JOptionPane.showMessageDialog(mainFrame,
                String.format("Image Size: %dx%d pixels", canvas.getWidth(), canvas.getHeight()),
                "Image Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleResize(ActionEvent e) {
        // TODO: Show resize dialog with current dimensions, allowing user to enter new dimensions
        // This should resize the canvas while maintaining the content
    }

    private void handleScale(ActionEvent e) {
        // TODO: Show scale dialog allowing user to enter scale percentage or new dimensions
        // This should scale the image content
    }
}