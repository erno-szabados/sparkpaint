package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.UndoRedoChangeListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class EditMenu extends JMenu implements UndoRedoChangeListener {
    private final DrawingCanvas canvas;
    private final JMenuItem undoItem;
    private final JMenuItem redoItem;

    public EditMenu(MainFrame mainFrame) {
        super("Edit");
        this.canvas = mainFrame.getCanvas();

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

        // Register as UndoRedoChangeListener
        canvas.addUndoRedoChangeListener(this);

        // Initialize enabled state of menu items
        updateMenuItems(canvas.canUndo(), canvas.canRedo());

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

    @Override
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo) {
        updateMenuItems(canUndo, canRedo);
    }

}