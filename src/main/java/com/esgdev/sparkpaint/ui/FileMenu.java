package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class FileMenu extends JMenu {
    private static final FileNameExtensionFilter IMAGE_FILTER =
            new FileNameExtensionFilter("Image files (*.png, *.jpg, *.jpeg, *.bmp)",
                    "png", "jpg", "jpeg", "bmp");

    private final MainFrame mainFrame;

    public FileMenu(MainFrame mainFrame) {
        super("File");
        this.mainFrame = mainFrame;
        // ... rest of constructor remains the same ...

        setMnemonic(KeyEvent.VK_F);  // Alt+F shortcut

        // Create menu items
        JMenuItem newFile = new JMenuItem("New", KeyEvent.VK_N);
        newFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));

        JMenuItem open = new JMenuItem("Open...", KeyEvent.VK_O);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));

        JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));

        JMenuItem saveAs = new JMenuItem("Save As...", KeyEvent.VK_A);
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

        JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));

        // Add menu items to the menu
        add(newFile);
        add(open);
        addSeparator();
        add(save);
        add(saveAs);
        addSeparator();
        add(exit);

        // Add action listeners (to be implemented later)
        newFile.addActionListener(e -> handleNewFile());
        open.addActionListener(e -> handleOpen());
        save.addActionListener(e -> handleSave());
        saveAs.addActionListener(e -> handleSaveAs());
        exit.addActionListener(e -> handleExit());
    }

    private void handleNewFile() {
        NewImageDialog dialog = new NewImageDialog(mainFrame);
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            mainFrame.getCanvas().createNewCanvas(
                    dialog.getImageWidth(),
                    dialog.getImageHeight(),
                    dialog.getBackgroundColor()
            );
            mainFrame.getCanvas().resetCurrentFilePath();
            mainFrame.pack();  // Adjust frame size to new canvas size

            mainFrame.setStatusMessage(String.format("New image created (%dx%d)",
                    dialog.getImageWidth(), dialog.getImageHeight()));

        }
    }

    private void handleOpen() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(IMAGE_FILTER);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                mainFrame.getCanvas().loadFromFile(file);
                mainFrame.pack(); // Adjust frame size to the loaded image
                mainFrame.setStatusMessage("Opened: " + file.getAbsolutePath());
            } catch (IOException ex) {
                mainFrame.setStatusMessage("Error opening file!");
                JOptionPane.showMessageDialog(mainFrame,
                        "Error opening file: " + ex.getMessage(),
                        "Open Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            mainFrame.setStatusMessage("Open cancelled");
        }
    }


    private void handleSave() {
        DrawingCanvas canvas = mainFrame.getCanvas();
        String currentPath = canvas.getCurrentFilePath();

        if (currentPath == null) {
            handleSaveAs();
        } else {
            try {
                canvas.saveToFile(new File(currentPath));
                mainFrame.setStatusMessage("Image saved to: " + currentPath);
            } catch (IOException ex) {
                mainFrame.setStatusMessage("Error saving file!");
                JOptionPane.showMessageDialog(mainFrame,
                        "Error saving file: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void handleSaveAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(IMAGE_FILTER);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String fileName = file.getName().toLowerCase();

            // Add extension if not present
            if (!hasImageExtension(fileName)) {
                file = new File(file.getAbsolutePath() + ".png"); // Default to PNG
            }

            // Check if file exists
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(mainFrame,
                        "File already exists. Do you want to replace it?",
                        "Confirm Save As",
                        JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    mainFrame.setStatusMessage("Save cancelled");
                    return;
                }
            }

            try {
                mainFrame.getCanvas().saveToFile(file);
                mainFrame.setStatusMessage("Image saved as: " + file.getAbsolutePath());
            } catch (IOException ex) {
                mainFrame.setStatusMessage("Error saving file!");
                JOptionPane.showMessageDialog(mainFrame,
                        "Error saving file: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            mainFrame.setStatusMessage("Save cancelled");
        }
    }

    private boolean hasImageExtension(String fileName) {
        return fileName.endsWith(".png") ||
                fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".bmp");
    }


    private void handleExit() {
        System.exit(0);  // Basic implementation - might want to add save prompts later
    }
}