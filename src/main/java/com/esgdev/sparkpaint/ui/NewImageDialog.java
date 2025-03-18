package com.esgdev.sparkpaint.ui;

        import com.esgdev.sparkpaint.engine.DrawingCanvas;
        import javax.swing.*;
        import java.awt.*;

        public class NewImageDialog extends JDialog {
            private int imageWidth = DrawingCanvas.DEFAULT_CANVAS_WIDTH;
            private int imageHeight = DrawingCanvas.DEFAULT_CANVAS_HEIGHT;
            private Color backgroundColor = Color.WHITE;
            private boolean approved = false;

            private static final String CUSTOM_SIZE = "Custom";
            private static final String[][] PRESET_SIZES = {
                {"Custom", "0", "0"},
                {"32x32", "32", "32"},
                {"64x64", "64", "64"},
                {"128x128", "128", "128"},
                {"256x256", "256", "256"},
                {"512x512", "512", "512"},
                {"640x480 (VGA)", "640", "480"},
                {"800x600 (SVGA)", "800", "600"},
                {"1024x768 (XGA)", "1024", "768"},
                {"1280x720 (HD)", "1280", "720"},
                {"1920x1080 (Full HD)", "1920", "1080"}
            };

            public NewImageDialog(Frame owner) {
                super(owner, "New Image", true);

                // Create components
                JTextField widthField = new JTextField(String.valueOf(imageWidth), 5);
                JTextField heightField = new JTextField(String.valueOf(imageHeight), 5);
                JComboBox<String> presetSizes = new JComboBox<>(createPresetNames());
                JButton colorButton = new JButton("Choose Background Color");
                colorButton.setToolTipText("Select the background color for the new image");
                colorButton.setBackground(backgroundColor);

                // Layout
                JPanel contentPane = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.anchor = GridBagConstraints.WEST;

                // Preset sizes row
                gbc.gridx = 0; gbc.gridy = 0;
                contentPane.add(new JLabel("Preset:"), gbc);
                gbc.gridx = 1; gbc.gridwidth = 2;
                contentPane.add(presetSizes, gbc);

                // Width row
                gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
                contentPane.add(new JLabel("Width:"), gbc);
                gbc.gridx = 1;
                contentPane.add(widthField, gbc);
                gbc.gridx = 2;
                contentPane.add(new JLabel("pixels"), gbc);

                // Height row
                gbc.gridx = 0; gbc.gridy = 2;
                contentPane.add(new JLabel("Height:"), gbc);
                gbc.gridx = 1;
                contentPane.add(heightField, gbc);
                gbc.gridx = 2;
                contentPane.add(new JLabel("pixels"), gbc);

                // Color row
                gbc.gridx = 0; gbc.gridy = 3;
                contentPane.add(new JLabel("Background:"), gbc);
                gbc.gridx = 1; gbc.gridwidth = 2;
                contentPane.add(colorButton, gbc);

                // Buttons panel
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton okButton = new JButton("OK");
                JButton cancelButton = new JButton("Cancel");
                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                gbc.gridx = 0; gbc.gridy = 4;
                gbc.gridwidth = 3;
                gbc.anchor = GridBagConstraints.EAST;
                contentPane.add(buttonPanel, gbc);

                // Add actions
                presetSizes.addActionListener(e -> {
                    int selectedIndex = presetSizes.getSelectedIndex();
                    if (selectedIndex > 0) {
                        widthField.setText(PRESET_SIZES[selectedIndex][1]);
                        heightField.setText(PRESET_SIZES[selectedIndex][2]);
                    }
                });

                colorButton.addActionListener(e -> {
                    Color newColor = JColorChooser.showDialog(this, "Choose Background Color", backgroundColor);
                    if (newColor != null) {
                        backgroundColor = newColor;
                        colorButton.setBackground(backgroundColor);
                    }
                });

                okButton.addActionListener(e -> {
                    try {
                        imageWidth = Integer.parseInt(widthField.getText());
                        imageHeight = Integer.parseInt(heightField.getText());
                        if (imageWidth <= 0 || imageHeight <= 0) {
                            throw new NumberFormatException();
                        }
                        if (imageWidth > 3000 || imageHeight > 3000) {
                            JOptionPane.showMessageDialog(this,
                                "Width and height must be less than or equal to 10000 pixels.",
                                "Invalid Input",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        approved = true;
                        dispose();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this,
                            "Please enter valid positive numbers for width and height.",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });

                cancelButton.addActionListener(e -> dispose());

                // Dialog setup
                setContentPane(contentPane);
                pack();
                setLocationRelativeTo(owner);
                setResizable(false);
            }

            private String[] createPresetNames() {
                String[] names = new String[PRESET_SIZES.length];
                for (int i = 0; i < PRESET_SIZES.length; i++) {
                    names[i] = PRESET_SIZES[i][0];
                }
                return names;
            }

            public boolean isApproved() {
                return approved;
            }

            public int getImageWidth() {
                return imageWidth;
            }

            public int getImageHeight() {
                return imageHeight;
            }

            public Color getBackgroundColor() {
                return backgroundColor;
            }
        }