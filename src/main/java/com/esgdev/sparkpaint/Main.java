package com.esgdev.sparkpaint;

import com.esgdev.sparkpaint.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        // Ensure all UI-related setup is handled on the Event Dispatch Thread (EDT)
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(); // Separate class for main window logic
            mainFrame.setVisible(true);           // Make the window visible
        });
    }
}