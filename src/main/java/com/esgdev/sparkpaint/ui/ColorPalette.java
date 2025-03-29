package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.io.PaletteManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorPalette extends JPanel implements PaletteManager.PaletteChangeListener {
    private static final int SWATCH_SIZE = 27;
    private static final int COLUMNS = 15;
    private static final Color[] DEFAULT_COLORS = {
            // Butter (Yellow)
            new Color(252, 233, 79),  // Light
            new Color(237, 212, 0),   // Medium
            new Color(196, 160, 0),   // Dark

            // Orange
            new Color(252, 175, 62),  // Light
            new Color(245, 121, 0),   // Medium
            new Color(206, 92, 0),    // Dark

            // Chocolate (Brown)
            new Color(233, 185, 110), // Light
            new Color(193, 125, 17),  // Medium
            new Color(143, 89, 2),    // Dark

            // Chameleon (Green)
            new Color(138, 226, 52),  // Light
            new Color(115, 210, 22),  // Medium
            new Color(78, 154, 6),    // Dark

            // Sky Blue
            new Color(114, 159, 207), // Light
            new Color(52, 101, 164),  // Medium
            new Color(32, 74, 135),   // Dark

            // Plum (Purple)
            new Color(173, 127, 168), // Light
            new Color(117, 80, 123),  // Medium
            new Color(92, 53, 102),   // Dark

            // Scarlet Red
            new Color(239, 41, 41),   // Light
            new Color(204, 0, 0),     // Medium
            new Color(164, 0, 0),     // Dark

            // Aluminium (Gray)
            new Color(238, 238, 236), // Light
            new Color(211, 215, 207), // Medium Light
            new Color(186, 189, 182), // Medium
            new Color(136, 138, 133), // Medium Dark
            new Color(85, 87, 83),    // Dark
            new Color(46, 52, 54)     // Darkest
    };
    private ArrayList<Color> colors;
    private final PaletteManager paletteManager;

    private final DrawingCanvas canvas;

    public ColorPalette(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.paletteManager = canvas.getPaletteManager();
        this.colors = new ArrayList<>(Arrays.asList(DEFAULT_COLORS));
        int rows = (colors.size() + COLUMNS - 1) / COLUMNS;
        setLayout(new GridLayout(rows, COLUMNS, 1, 1));
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        initializePalette();
        paletteManager.addPaletteChangeListener(this);
    }

    public void loadPalette(File file) throws IOException {
        colors = (ArrayList<Color>) paletteManager.loadPalette(file);
        removeAll();
        initializePalette();
        revalidate();
        repaint();
    }

    public void savePalette(File file) throws IOException {
        paletteManager.savePalette(file, colors);
    }

    private void initializePalette() {
        for (Color color : colors) {
            JPanel swatch = createColorSwatch(color);
            add(swatch);
        }
    }

    public void restoreDefaultPalette() {
        colors = new ArrayList<>(Arrays.asList(DEFAULT_COLORS));
        removeAll();
        initializePalette();
        revalidate();
        repaint();
    }

    private JPanel createColorSwatch(Color initialColor) {
        // Create a wrapper class to hold mutable color value
        class ColorHolder {
            Color color = initialColor;
        }
        ColorHolder holder = new ColorHolder();

        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(holder.color);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };

        swatch.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        swatch.setToolTipText(String.format("#%02X%02X%02X",
                holder.color.getRed(), holder.color.getGreen(), holder.color.getBlue()));

        JColorChooser colorChooser = new JColorChooser();
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    canvas.setDrawingColor(holder.color);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    canvas.setFillColor(holder.color);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JDialog dialog = JColorChooser.createDialog(
                            swatch,
                            "Choose Color",
                            true,
                            colorChooser,
                            action -> {
                                holder.color = colorChooser.getColor();
                                swatch.setToolTipText(String.format("#%02X%02X%02X",
                                        holder.color.getRed(), holder.color.getGreen(), holder.color.getBlue()));
                                swatch.repaint();
                            },
                            null);

                    colorChooser.setColor(holder.color);
                    dialog.setVisible(true);
                }
            }
        });

        return swatch;
    }

    // Implement the PaletteChangeListener interface
    @Override
    public void onPaletteChanged(List<Color> newPalette) {
        colors = new ArrayList<>(newPalette);
        removeAll();
        initializePalette();
        revalidate();
        repaint();
    }

    // Add a dispose method to prevent memory leaks
    public void dispose() {
        paletteManager.removePaletteChangeListener(this);
    }
}