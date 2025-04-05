package com.esgdev.sparkpaint.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HelpMenu extends JMenu {
    public HelpMenu() {
        setText("Help");

        // Create About menu item
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());

        // Add menu items to Help menu
        addManualMenuItem();
        add(aboutItem);
    }

    private void showAboutDialog() {
        JDialog aboutDialog = new JDialog();
        aboutDialog.setTitle("About SparkPaint");
        aboutDialog.setModal(true);

        // Create content panel with proper layout
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add application name
        JLabel nameLabel = new JLabel("SparkPaint");
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.BOLD, 16));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add version
        JLabel versionLabel = new JLabel("Version 0.19");
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create JTextPane for styled text with proper alignment
        JTextPane descriptionPane = new JTextPane();
        descriptionPane.setEditable(false);
        descriptionPane.setOpaque(false);
        descriptionPane.setFocusable(false);
        descriptionPane.setBackground(UIManager.getColor("Label.background"));
        descriptionPane.setFont(UIManager.getFont("Label.font"));
        descriptionPane.setBorder(BorderFactory.createEmptyBorder());
        descriptionPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Configure the StyledDocument
        StyledDocument doc = descriptionPane.getStyledDocument();
        SimpleAttributeSet centerStyle = new SimpleAttributeSet();
        StyleConstants.setAlignment(centerStyle, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), centerStyle, true);

        try {
            // Insert the main description
            doc.insertString(0, "A simple and lightweight drawing application", null);

            // Add a line break and dedication on a new line
            doc.insertString(doc.getLength(), "\n\nDedicated to", null);
            doc.insertString(doc.getLength(), "\nJanos Szikora, who challenged me to think", null);
            doc.insertString(doc.getLength(), "\nJozsef Szivos, with whom I shared interest in computers and Sci-Fi.", null);
            doc.insertString(doc.getLength(), "\nand the artists of the demoscene who are vital in creating computer culture.", null);

            // Apply the center alignment to the entire document
            doc.setParagraphAttributes(0, doc.getLength(), centerStyle, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add components to panel
        contentPanel.add(nameLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(descriptionPane);

        // Add OK button in a separate panel
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> aboutDialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(okButton);

        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(buttonPanel);

        aboutDialog.add(contentPanel);
        aboutDialog.pack();
        aboutDialog.setLocationRelativeTo(null);
        aboutDialog.setVisible(true);
    }

    private void addManualMenuItem() {
        JMenuItem manualItem = new JMenuItem("User Manual");
        manualItem.addActionListener(e -> showManualDialog());
        add(manualItem);
    }

    private void showManualDialog() {
        JDialog manualDialog = new JDialog();
        manualDialog.setTitle("SparkPaint Manual");
        manualDialog.setModal(true);
        manualDialog.setSize(800, 600);

        // Create a text area to display the manual content
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);

        // Load and display the manual content
        try {
            String manualContent = loadManualContent();
            displayFormattedManual(textPane, manualContent);
        } catch (Exception e) {
            textPane.setText("Error loading manual: " + e.getMessage());
            e.printStackTrace();
        }

        // Add scrolling capability
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Add a close button at the bottom
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> manualDialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        // Set up the layout
        manualDialog.setLayout(new BorderLayout());
        manualDialog.add(scrollPane, BorderLayout.CENTER);
        manualDialog.add(buttonPanel, BorderLayout.SOUTH);

        manualDialog.setLocationRelativeTo(null);
        manualDialog.setVisible(true);
    }

    private String loadManualContent() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/data/Manual.md")) {
            if (is == null) {
                throw new IOException("Manual.md not found in resources");
            }

            // Java 8 compatible way to read all bytes
            try (java.util.Scanner scanner = new java.util.Scanner(is, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A"); // Read the entire stream as one token
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private void displayFormattedManual(JTextPane textPane, String markdownContent) {
        StyledDocument doc = textPane.getStyledDocument();

        // Define styles
        Style defaultStyle = textPane.getStyle(StyleContext.DEFAULT_STYLE);

        Style heading1 = textPane.addStyle("Heading1", defaultStyle);
        StyleConstants.setFontSize(heading1, 24);
        StyleConstants.setBold(heading1, true);

        Style heading2 = textPane.addStyle("Heading2", defaultStyle);
        StyleConstants.setFontSize(heading2, 20);
        StyleConstants.setBold(heading2, true);

        Style heading3 = textPane.addStyle("Heading3", defaultStyle);
        StyleConstants.setFontSize(heading3, 18);
        StyleConstants.setBold(heading3, true);

        Style boldStyle = textPane.addStyle("Bold", defaultStyle);
        StyleConstants.setBold(boldStyle, true);

        // Split the content into lines and process each line
        String[] lines = markdownContent.split("\n");
        try {
            for (String line : lines) {
                // Process headings
                if (line.startsWith("# ")) {
                    doc.insertString(doc.getLength(), line.substring(2) + "\n\n", heading1);
                } else if (line.startsWith("## ")) {
                    doc.insertString(doc.getLength(), line.substring(3) + "\n\n", heading2);
                } else if (line.startsWith("### ")) {
                    doc.insertString(doc.getLength(), line.substring(4) + "\n\n", heading3);
                } else if (line.startsWith("![")) {
                    // Skip image links as we can't display them
                    doc.insertString(doc.getLength(), "(Image: " +
                            line.substring(line.indexOf("]") + 2, line.lastIndexOf(")")) + ")\n", defaultStyle);
                } else if (line.startsWith("|")) {
                    // Handle table rows simply
                    doc.insertString(doc.getLength(), line.replace("|", "\t") + "\n", defaultStyle);
                } else {
                    // Handle normal text, including basic formatting for bold
                    if (line.contains("**")) {
                        int index = 0;
                        boolean isBold = false;
                        while (index < line.length()) {
                            int boldStart = line.indexOf("**", index);
                            if (boldStart == -1) {
                                // No more bold markers
                                doc.insertString(doc.getLength(), line.substring(index),
                                        isBold ? boldStyle : defaultStyle);
                                break;
                            }

                            // Insert text before bold marker
                            doc.insertString(doc.getLength(), line.substring(index, boldStart),
                                    isBold ? boldStyle : defaultStyle);

                            // Toggle bold state
                            isBold = !isBold;
                            index = boldStart + 2;
                        }
                        doc.insertString(doc.getLength(), "\n", defaultStyle);
                    } else {
                        // Regular line
                        doc.insertString(doc.getLength(), line + "\n", defaultStyle);
                    }
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
