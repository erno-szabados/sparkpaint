package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.selection.SelectionManager;
import com.esgdev.sparkpaint.io.ClipboardChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.history.UndoRedoChangeListener;
import com.esgdev.sparkpaint.io.ClipboardManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * EditMenu is a custom JMenu that provides various editing options such as Cut, Copy, Paste,
 * Undo, Redo, and more. It interacts with the DrawingCanvas and SelectionManager to perform
 * these actions.
 */
public class EditMenu extends JMenu implements UndoRedoChangeListener, ClipboardChangeListener {
    private final DrawingCanvas canvas;
    //private final ClipboardManager clipboardManager;
    private final JMenuItem undoItem;
    private final JMenuItem redoItem;
    private final JMenuItem cutItem; // New Cut menu item
    private final JMenuItem copyItem; // New Copy menu item
    private final JMenuItem pasteItem; // New Paste menu item


    /**
     * Constructor for EditMenu.
     *
     * @param mainFrame The main frame of the application, used to access the canvas and clipboard manager.
     */
    public EditMenu(MainFrame mainFrame) {
        super("Edit");
        this.canvas = mainFrame.getCanvas();
        //this.selectionManager = canvas.getSelectionManager();
        //this.clipboardManager = canvas.getClipboardManager();


        // Create and add Cut item
        cutItem = new JMenuItem("Cut");
        cutItem.setMnemonic('T'); // Shortcut for accessibility (Alt+T)
        // Update action listeners to use selectionManager and clipboardManager
        cutItem.addActionListener(e -> canvas.cutSelection());
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK)); // Ctrl+X
        add(cutItem);

        // Create and add Copy item
        copyItem = new JMenuItem("Copy");
        copyItem.setMnemonic('C'); // Shortcut for accessibility (Alt+C)
        copyItem.addActionListener(e -> canvas.copySelection());
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK)); // Ctrl+C
        add(copyItem);

        // Create and add Paste item
        pasteItem = new JMenuItem("Paste");
        pasteItem.setMnemonic('P'); // Shortcut for accessibility (Alt+P)
        pasteItem.addActionListener(e -> {
            try {
                canvas.pasteSelection();
            } catch (Exception ex) {
                mainFrame.setStatusMessage("Error pasting clipboard content!");
                JOptionPane.showMessageDialog(mainFrame,
                        "Error pasting clipboard content!: " + ex.getMessage(),
                        "Clipboard Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK)); // Ctrl+V
        add(pasteItem);

        // In EditMenu class constructor, after the paste item
        addSeparator();

        // Add Select All item
        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setMnemonic('A');
        selectAllItem.addActionListener(e -> canvas.selectAll());

        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        add(selectAllItem);

        // Add Delete item
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.setMnemonic('D');
        deleteItem.addActionListener(e -> canvas.deleteSelection());
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        add(deleteItem);

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
        JMenuItem loadPaletteItem = getLoadPaletteItem(mainFrame);
        add(loadPaletteItem);

        // Create and add Save Palette item
        JMenuItem savePaletteItem = getSavePaletteItem(mainFrame);
        add(savePaletteItem);

        // After adding savePaletteItem, add:
        JMenuItem restoreDefaultPaletteItem = getRestoreDefaultPaletteItem(mainFrame);
        add(restoreDefaultPaletteItem);

        // Register as UndoRedoChangeListener
        canvas.addUndoRedoChangeListener(this);

        // Initialize enabled state of menu items
        updateMenuItems(canvas.canUndo(), canvas.canRedo());
    }

    /**
     * Updates the enabled state of the Undo and Redo menu items.
     *
     * @param canUndo Indicates if an undo operation is possible.
     * @param canRedo Indicates if a redo operation is possible.
     */
    @Override
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo) {
        SwingUtilities.invokeLater(() -> updateMenuItems(canUndo, canRedo));
    }

    /**
     * Updates the enabled state of the Cut, Copy, and Paste menu items based on the clipboard state.
     *
     * @param canCopy Indicates if a copy operation is possible.
     * @param canPaste Indicates if a paste operation is possible.
     */
    // Implement ClipboardChangeListener
    @Override
    public void clipboardStateChanged(boolean canCopy, boolean canPaste) {
        // Ensure UI updates happen on EDT
        SwingUtilities.invokeLater(() -> updateClipboardMenuItems(canCopy, canPaste));
    }

    /**
     * Creates a menu item to restore the default color palette.
     *
     * @param mainFrame The main frame of the application.
     * @return A JMenuItem for restoring the default color palette.
     */
    private static JMenuItem getRestoreDefaultPaletteItem(MainFrame mainFrame) {
        JMenuItem restoreDefaultPaletteItem = new JMenuItem("Restore Default Palette");
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
        return restoreDefaultPaletteItem;
    }

    /**
     * Creates a menu item to save the current color palette.
     *
     * @param mainFrame The main frame of the application.
     * @return A JMenuItem for saving the current color palette.
     */
    private static JMenuItem getSavePaletteItem(MainFrame mainFrame) {
        JMenuItem savePaletteItem = new JMenuItem("Save Palette As...");
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
        return savePaletteItem;
    }

    /**
     * Creates a menu item to load a color palette from a file.
     *
     * @param mainFrame The main frame of the application.
     * @return A JMenuItem for loading a color palette.
     */
    private static JMenuItem getLoadPaletteItem(MainFrame mainFrame) {
        JMenuItem loadPaletteItem = new JMenuItem("Load Palette...");
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
        return loadPaletteItem;
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


}