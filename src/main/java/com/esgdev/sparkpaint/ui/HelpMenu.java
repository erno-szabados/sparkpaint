package com.esgdev.sparkpaint.ui;

import javax.swing.*;
import java.awt.*;

public class HelpMenu extends JMenu {
    public HelpMenu() {
        setText("Help");
        
        // Create About menu item
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        
        // Add menu items to Help menu
        add(aboutItem);
    }
    
    private void showAboutDialog() {
        JDialog aboutDialog = new JDialog();
        aboutDialog.setTitle("About SparkPaint");
        aboutDialog.setModal(true);
        aboutDialog.setSize(300, 200);
        aboutDialog.setLocationRelativeTo(null); // Center on screen
        
        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Add application name
        JLabel nameLabel = new JLabel("SparkPaint");
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.BOLD, 16));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add version
        JLabel versionLabel = new JLabel("Version 0.6");
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add description
        JLabel descriptionLabel = new JLabel("<html><center>A simple and lightweight<br>drawing application</center></html>");
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components to panel
        contentPanel.add(nameLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(descriptionLabel);
        
        aboutDialog.add(contentPanel);
        aboutDialog.setVisible(true);
    }
}
