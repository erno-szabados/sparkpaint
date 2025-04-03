package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.io.SparkPaintFileFormat;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileMenu extends JMenu {
    public static final String SPARKPAINT_FILE_EXTENSION = "spp";
    private static final FileNameExtensionFilter IMAGE_FILTER =
            new FileNameExtensionFilter("Image files (*.png, *.jpg, *.jpeg, *.bmp)",
                    "png", "jpg", "jpeg", "bmp");
    private static final FileNameExtensionFilter SPARKPAINT_FILTER =
            new FileNameExtensionFilter("SparkPaint Image with Layers (*." + SPARKPAINT_FILE_EXTENSION + ")", SPARKPAINT_FILE_EXTENSION);

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

        JMenuItem exportLayers = new JMenuItem("Export Layers as PNG...", KeyEvent.VK_E);
        exportLayers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));


        JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));

        // Add menu items to the menu
        add(newFile);
        add(open);
        addSeparator();
        add(save);
        add(saveAs);
        add(exportLayers);
        addSeparator();
        add(exit);

        // Add action listeners (to be implemented later)
        newFile.addActionListener(e -> handleNewFile());
        open.addActionListener(e -> handleOpen());
        save.addActionListener(e -> handleSave());
        saveAs.addActionListener(e -> handleSaveAs());
        exportLayers.addActionListener(e -> handleExportLayers());
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

        // Add both filters
        fileChooser.addChoosableFileFilter(IMAGE_FILTER);
        fileChooser.addChoosableFileFilter(SPARKPAINT_FILTER);
        fileChooser.setFileFilter(SPARKPAINT_FILTER); // Prefer layer format in open dialog
        fileChooser.setAcceptAllFileFilterUsed(true); // Let users open any file type

        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                DrawingCanvas canvas = mainFrame.getCanvas();

                if (file.getName().toLowerCase().endsWith("." + SPARKPAINT_FILE_EXTENSION)) {
                    // Load from SparkPaint format using the new method
                    canvas.loadFromLayeredFile(file);
                    mainFrame.setStatusMessage("Opened project with layers: " + file.getAbsolutePath());
                } else {
                    // Regular image opening
                    canvas.loadFromFile(file);
                    mainFrame.setStatusMessage("Opened: " + file.getAbsolutePath());
                }
                mainFrame.pack(); // Adjust frame size to the loaded image
            } catch (IOException | ClassNotFoundException ex) {
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
                // Check if we're about to flatten layers
                boolean isSparkPaintFormat = currentPath.toLowerCase().endsWith("." + SPARKPAINT_FILE_EXTENSION);
                boolean hasMultipleLayers = canvas.getLayerCount() > 1;

                if (!isSparkPaintFormat && hasMultipleLayers) {
                    int result = JOptionPane.showConfirmDialog(mainFrame,
                            "This format doesn't support layers. Your layers will be flattened." +
                                    "\nUse 'Save As...' and select SparkPaint format to preserve layers." +
                                    "\n\nContinue with flattened save?",
                            "Layers Will Be Lost",
                            JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        handleSaveAs(); // Let user choose SparkPaint format instead
                        return;
                    }
                }

                // Choose appropriate save method
                if (isSparkPaintFormat) {
                    SparkPaintFileFormat.saveToFile(
                            new File(currentPath),
                            canvas.getLayers(),
                            canvas.getCurrentLayerIndex()
                    );
                } else {
                    canvas.saveToFile(new File(currentPath));
                }
                mainFrame.setStatusMessage("Saved to: " + currentPath);
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

        // Add both filters with descriptive names
        fileChooser.addChoosableFileFilter(IMAGE_FILTER);
        fileChooser.addChoosableFileFilter(SPARKPAINT_FILTER);

        // Set SparkPaint format as default when working with multiple layers
        if (mainFrame.getCanvas().getLayerCount() > 1) {
            fileChooser.setFileFilter(SPARKPAINT_FILTER);
        } else {
            fileChooser.setFileFilter(IMAGE_FILTER);
        }

        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            FileNameExtensionFilter selectedFilter =
                    (FileNameExtensionFilter) fileChooser.getFileFilter();

            // Determine if saving in SparkPaint format
            boolean isSparkPaintFormat = selectedFilter == SPARKPAINT_FILTER;

            // Add appropriate extension if missing
            String fileName = file.getName().toLowerCase();
            if (!hasFileExtension(fileName)) {
                if (isSparkPaintFormat) {
                    file = new File(file.getAbsolutePath() + "." + SPARKPAINT_FILE_EXTENSION);
                } else {
                    file = new File(file.getAbsolutePath() + ".png"); // Default to PNG
                }
            }

            // Confirm overwrite
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
                if (isSparkPaintFormat) {
                    // Save with SparkPaintFileFormat
                    SparkPaintFileFormat.saveToFile(
                            file,
                            mainFrame.getCanvas().getLayers(),
                            mainFrame.getCanvas().getCurrentLayerIndex()
                    );
                    mainFrame.getCanvas().setCurrentFilePath(file.getAbsolutePath());
                    mainFrame.setStatusMessage("Project saved with layers to: " + file.getAbsolutePath());
                } else {
                    // Regular save with flattened image
                    mainFrame.getCanvas().saveToFile(file);
                    mainFrame.setStatusMessage("Image saved as: " + file.getAbsolutePath());
                }
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

    private boolean hasFileExtension(String fileName) {
        return fileName.endsWith(".png") ||
                fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".bmp") ||
                fileName.endsWith("." + SPARKPAINT_FILE_EXTENSION);
    }


    /**
     * Handles the export layers action.
     * Prompts the user to select a directory and exports visible layers as PNG files.
     */
    private void handleExportLayers() {
        DrawingCanvas canvas = mainFrame.getCanvas();
        List<Layer> layers = canvas.getLayers();

        // Count visible layers
        long visibleLayerCount = layers.stream()
                .filter(Layer::isVisible)
                .count();

        if (visibleLayerCount == 0) {
            JOptionPane.showMessageDialog(mainFrame,
                    "There are no visible layers to export.",
                    "Export Layers",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Choose directory for export
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle("Choose Export Directory");

        if (dirChooser.showDialog(mainFrame, "Export") != JFileChooser.APPROVE_OPTION) {
            mainFrame.setStatusMessage("Export cancelled");
            return;
        }

        File exportDir = dirChooser.getSelectedFile();

        // Determine filename prefix
        String fileNamePrefix;
        String currentPath = canvas.getCurrentFilePath();

        if (currentPath != null && !currentPath.isEmpty()) {
            // Use the base name of the current file as prefix
            File currentFile = new File(currentPath);
            fileNamePrefix = currentFile.getName();

            // Remove extension if present
            int lastDotIndex = fileNamePrefix.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileNamePrefix = fileNamePrefix.substring(0, lastDotIndex);
            }
        } else {
            // Generate a default prefix
            fileNamePrefix = mainFrame.getCanvas().generateFileNamePrefix();
        }

        try {
            int exportedCount = mainFrame.getCanvas().exportLayersAsPNG(exportDir, fileNamePrefix, layers);

            mainFrame.setStatusMessage("Exported " + exportedCount + " layers to " + exportDir.getAbsolutePath());
        } catch (IOException ex) {
            mainFrame.setStatusMessage("Error exporting layers!");
            JOptionPane.showMessageDialog(mainFrame,
                    "Error exporting layers: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void handleExit() {
        System.exit(0);  // Basic implementation - might want to add save prompts later
    }
}