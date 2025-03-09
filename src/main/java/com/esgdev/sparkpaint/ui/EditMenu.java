package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.io.ClipboardChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.UndoRedoChangeListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class EditMenu extends JMenu implements UndoRedoChangeListener, ClipboardChangeListener {
    private final DrawingCanvas canvas;
    private final JMenuItem undoItem;
    private final JMenuItem redoItem;
    private final JMenuItem cutItem; // New Cut menu item
    private final JMenuItem copyItem; // New Copy menu item
    private final JMenuItem pasteItem; // New Paste menu item
    private final JMenuItem loadPaletteItem;
    private final JMenuItem savePaletteItem;
    private final JMenuItem restoreDefaultPaletteItem;
    private final MainFrame mainFrame;


    public EditMenu(MainFrame mainFrame) {
        super("Edit");
        this.mainFrame = mainFrame;
        this.canvas = mainFrame.getCanvas();

        // Create and add Cut item
        cutItem = new JMenuItem("Cut");
        cutItem.setMnemonic('T'); // Shortcut for accessibility (Alt+T)
        cutItem.addActionListener(this::handleCut);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK)); // Ctrl+X
        add(cutItem);

        // Create and add Copy item
        copyItem = new JMenuItem("Copy");
        copyItem.setMnemonic('C'); // Shortcut for accessibility (Alt+C)
        copyItem.addActionListener(this::handleCopy);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK)); // Ctrl+C
        add(copyItem);

        // Create and add Paste item
        pasteItem = new JMenuItem("Paste");
        pasteItem.setMnemonic('P'); // Shortcut for accessibility (Alt+P)
        pasteItem.addActionListener(this::handlePaste);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK)); // Ctrl+V
        add(pasteItem);
        // Register for clipboard changes
        canvas.addClipboardChangeListener(this);

        // Initialize menu items' enabled state
        updateClipboardMenuItems(false, canvas.canPaste());


        // Add a separator between Undo/Redo and Cut/Copy/Paste
        addSeparator();

        // Create and add Undo item
        undoItem = new JMenuItem("Undo");
        undoItem.setMnemonic('U'); // Shortcut for accessibility
        undoItem.addActionListener(this::handleUndo);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
        add(undoItem);

        // Create and add Redo item
        redoItem = new JMenuItem("Redo");
        redoItem.setMnemonic('R');
        redoItem.addActionListener(this::handleRedo);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
        add(redoItem);

        addSeparator();

        // Create and add Load Palette item
        loadPaletteItem = new JMenuItem("Load Palette...");
        loadPaletteItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Palette Files (*.palette)", "palette"));

            if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                try {
                    mainFrame.getColorPalette().loadPalette(fileChooser.getSelectedFile());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Error loading palette: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        add(loadPaletteItem);

        // Create and add Save Palette item
        savePaletteItem = new JMenuItem("Save Palette As...");
        savePaletteItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Palette Files (*.palette)", "palette"));

            if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                try {
                    mainFrame.getColorPalette().savePalette(fileChooser.getSelectedFile());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Error saving palette: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        add(savePaletteItem);

        // After adding savePaletteItem, add:
        restoreDefaultPaletteItem = new JMenuItem("Restore Default Palette");
        restoreDefaultPaletteItem.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                    "This will replace your current palette with the default colors. Continue?",
                    "Restore Default Palette",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                mainFrame.getColorPalette().restoreDefaultPalette();
            }
        });
        add(restoreDefaultPaletteItem);

        // Register as UndoRedoChangeListener
        canvas.addUndoRedoChangeListener(this);

        // Initialize enabled state of menu items
        updateMenuItems(canvas.canUndo(), canvas.canRedo());
    }

    private void handleCut(ActionEvent e) {
        canvas.cutSelection(); // Assuming canvas handles the cut logic
    }

    private void handleCopy(ActionEvent e) {
        canvas.copySelection(); // Assuming canvas handles the copy logic
    }

    private void handlePaste(ActionEvent e) {
        try {
            canvas.pasteSelection(); // Assuming canvas handles the paste logic
        } catch (Exception ex) {
            mainFrame.setStatusMessage("Error pasting clipboard content!");
            JOptionPane.showMessageDialog(mainFrame,
                    "Error pasting clipboard content!: " + ex.getMessage(),
                    "Clipboard Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUndo(ActionEvent e) {
        if (canvas.canUndo()) {
            canvas.undo();
        }
    }

    private void handleRedo(ActionEvent e) {
        if (canvas.canRedo()) {
            canvas.redo();
        }
    }

    private void updateMenuItems(boolean canUndo, boolean canRedo) {
        undoItem.setEnabled(canUndo);
        redoItem.setEnabled(canRedo);
    }

    // Add new method to update clipboard-related menu items
    private void updateClipboardMenuItems(boolean canCopy, boolean canPaste) {
        cutItem.setEnabled(canCopy);
        copyItem.setEnabled(canCopy);
        pasteItem.setEnabled(canPaste);
    }


    @Override
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo) {
        SwingUtilities.invokeLater(() -> updateMenuItems(canUndo, canRedo));
    }

    // Implement ClipboardChangeListener
    @Override
    public void clipboardStateChanged(boolean canCopy, boolean canPaste) {
        // Ensure UI updates happen on EDT
        SwingUtilities.invokeLater(() -> updateClipboardMenuItems(canCopy, canPaste));
    }
}