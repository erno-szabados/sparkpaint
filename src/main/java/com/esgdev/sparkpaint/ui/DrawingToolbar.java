package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class DrawingToolbar extends JToolBar {
    private static final int IconWidth = 24;
    private static final int IconHeight = 24;
    private final StatusMessageHandler statusMessageHandler;

    private ButtonGroup toolGroup;
    private final DrawingCanvas canvas;

    public DrawingToolbar(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(JToolBar.VERTICAL);
        this.canvas = canvas;
        this.statusMessageHandler = statusMessageHandler;
        initializeToolbar();
    }

    private void initializeToolbar() {
        //toolGroup = new ButtonGroup();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setFloatable(false); // Disable floating toolbar

        // Load and scale icons
        ImageIcon pencilIcon = IconLoader.loadAndScaleIcon("pencil.png", IconWidth, IconHeight);
        ImageIcon lineIcon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);

        // Create Pencil button
        JButton pencilButton = new JButton();
        pencilButton.setIcon(pencilIcon);
        pencilButton.setToolTipText("Draw with Pencil");
        pencilButton.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.PENCIL));
        pencilButton.addActionListener(e -> statusMessageHandler.setStatusMessage("Pencil selected."));
        this.add(pencilButton);

        // Create Line button
        JButton lineButton = new JButton();
        lineButton.setIcon(lineIcon);
        lineButton.setToolTipText("Draw a Line");
        lineButton.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.LINE));
        lineButton.addActionListener(e -> statusMessageHandler.setStatusMessage("Line selected."));
        this.add(lineButton);

        this.add(createRectangleButton());
        this.add(createCircleButton());
        this.add(createColorButton());
        this.add(createBackgroundColorButton());
        this.addSeparator();
    }

    private JButton createColorButton() {
        JButton colorButton = new JButton();
        Icon colorIcon = getColorIcon(canvas.getDrawingColor());

        // Set the icon to the button
        colorButton.setIcon(colorIcon);
        colorButton.setToolTipText("Choose Drawing Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color",
                    colorButton.getBackground());
            if (newColor != null) {
                colorButton.setIcon(getColorIcon(newColor));
                // Update the hex color label
                colorButton.setToolTipText(String.format("#%02X%02X%02X",
                        newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                // Update the canvas drawing color
                canvas.setDrawingColor(newColor);
            }
        });
        return colorButton;
    }

    private JButton createBackgroundColorButton() {
        JButton backgroundColorButton = new JButton();
        backgroundColorButton.setIcon(getColorIcon(canvas.getFillColor()));
        backgroundColorButton.setToolTipText("Choose Fill Color");

        backgroundColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this,
                    "Choose Fill Color",
                    canvas.getFillColor()
            );
            if (newColor != null) {
                canvas.setFillColor(newColor);
                backgroundColorButton.setIcon(getColorIcon(newColor));
            }
        });

        return backgroundColorButton;
    }

    private JToggleButton createRectangleButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon outlineIcon = IconLoader.loadAndScaleIcon("rect-outline.png", IconWidth, IconHeight);
        ImageIcon filledIcon = IconLoader.loadAndScaleIcon("rect-filled.png", IconWidth, IconHeight);

        button.setIcon(outlineIcon);
        button.setToolTipText("Rectangle (Outline)");

        // Track the filled state
        final boolean[] isFilled = {false};

        button.addActionListener(e -> {
            if (canvas.getCurrentTool() != DrawingCanvas.Tool.RECTANGLE_OUTLINE &&
                    canvas.getCurrentTool() != DrawingCanvas.Tool.RECTANGLE_FILLED) {
                // First click - just select the tool in outline mode
                button.setIcon(outlineIcon);
                button.setToolTipText("Rectangle (Outline)");
                canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_OUTLINE);
                statusMessageHandler.setStatusMessage("Rectangle (Outline) selected.");
                isFilled[0] = false;
            } else {
                // Tool is already selected, toggle between modes
                isFilled[0] = !isFilled[0];
                if (isFilled[0]) {
                    button.setIcon(filledIcon);
                    button.setToolTipText("Rectangle (Filled)");
                    canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_FILLED);
                    statusMessageHandler.setStatusMessage("Rectangle (Filled) selected.");
                } else {
                    button.setIcon(outlineIcon);
                    button.setToolTipText("Rectangle (Outline)");
                    canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_OUTLINE);
                    statusMessageHandler.setStatusMessage("Rectangle (Outline) selected.");
                }
            }

        });

        return button;
    }

    private JToggleButton createCircleButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon outlineIcon = IconLoader.loadAndScaleIcon("circle-outline.png", IconWidth, IconHeight);
        ImageIcon filledIcon = IconLoader.loadAndScaleIcon("circle-filled.png", IconWidth, IconHeight);

        button.setIcon(outlineIcon);
        button.setToolTipText("Center point circle (Outline)");

        // Track the filled state
        final boolean[] isFilled = {false};

        button.addActionListener(e -> {
            if (canvas.getCurrentTool() != DrawingCanvas.Tool.CIRCLE_OUTLINE &&
                    canvas.getCurrentTool() != DrawingCanvas.Tool.CIRCLE_FILLED) {
                // First click - just select the tool in outline mode
                button.setIcon(outlineIcon);
                button.setToolTipText("Center point circle (Outline)");
                canvas.setCurrentTool(DrawingCanvas.Tool.CIRCLE_OUTLINE);
                statusMessageHandler.setStatusMessage("Center point circle (Outline) selected.");
                isFilled[0] = false;
            } else {
                // Tool is already selected, toggle between modes
                isFilled[0] = !isFilled[0];
                if (isFilled[0]) {
                    button.setIcon(filledIcon);
                    button.setToolTipText("Center point circle (Filled)");
                    canvas.setCurrentTool(DrawingCanvas.Tool.CIRCLE_FILLED);
                    statusMessageHandler.setStatusMessage("Center point circle (Filled) selected.");
                } else {
                    button.setIcon(outlineIcon);
                    button.setToolTipText("Center point circle (Outline)");
                    canvas.setCurrentTool(DrawingCanvas.Tool.CIRCLE_OUTLINE);
                    statusMessageHandler.setStatusMessage("Center point circle (Outline) selected.");
                }
            }

        });

        return button;
    }

    private Icon getColorIcon(Color color) {
        // Assuming canvas provides the current color
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color); // Assuming canvas provides the current color
                g.fillRect(x, y, IconWidth, IconHeight);
            }

            @Override
            public int getIconWidth() {
                return IconWidth;
            }

            @Override
            public int getIconHeight() {
                return IconHeight;
            }
        };
    }
}