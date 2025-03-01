package com.esgdev.sparkpaint.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class FileMenu extends JMenu {
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
            mainFrame.pack();  // Adjust frame size to new canvas size
        }
    }


    private void handleOpen() {
        // TODO: Implement open functionality
    }

    private void handleSave() {
        // TODO: Implement save functionality
    }

    private void handleSaveAs() {
        // TODO: Implement save as functionality
    }

    private void handleExit() {
        System.exit(0);  // Basic implementation - might want to add save prompts later
    }
}