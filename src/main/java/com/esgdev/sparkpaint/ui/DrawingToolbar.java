package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.CanvasPropertyChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.UndoRedoChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DrawingToolbar extends JToolBar implements UndoRedoChangeListener {
    private static final int IconWidth = 24;
    private static final int IconHeight = 24;
    private final StatusMessageHandler statusMessageHandler;

    private ButtonGroup toolGroup;
    private final DrawingCanvas canvas;

    private JButton colorButton;
    private JButton fillColorButton;
    private JButton undoButton;
    private JButton redoButton;


    public DrawingToolbar(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(JToolBar.VERTICAL);
        this.canvas = canvas;
        this.statusMessageHandler = statusMessageHandler;

        initializeToolbar();

        this.canvas.addCanvasPropertyChangeListener(new CanvasPropertyChangeListener() {
            @Override
            public void onDrawingColorChanged(Color newColor) {
                updateDrawingColorButton(newColor);
            }

            @Override
            public void onFillColorChanged(Color newColor) {
                updateFillColorButton(newColor);
            }

            @Override
            public void onBackgroundColorChanged(Color newColor) {
                // Optional: handle if needed
            }
        });
    }

    private void initializeToolbar() {
        // toolGroup = new ButtonGroup();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setFloatable(false); // Disable floating toolbar

        this.add(createSelectButton());

        // Undo/redo tools
        undoButton = createUndoButton();
        this.add(undoButton);
        redoButton = createRedoButton();
        this.add(redoButton);

        this.addSeparator();

        canvas.addUndoRedoChangeListener(this);

        // Create Pencil button
        JButton pencilButton = createPencilButton();
        this.add(pencilButton);

        // Create Line button
        JButton lineButton = createLineButton();
        this.add(lineButton);

        this.add(createRectangleButton());
        this.add(createCircleButton());
        colorButton = createColorButton();
        this.add(colorButton);
        fillColorButton = createFillColorButton();
        this.add(fillColorButton);
        this.addSeparator();
    }

    private JButton createUndoButton() {
        JButton button = new JButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("undo.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.setToolTipText("Undo");
        button.setEnabled(false); // Initial state
        button.addActionListener(this::handleUndo);
        button.addActionListener(e -> statusMessageHandler.setStatusMessage("Undo last drawing operation."));
        return button;
    }

    private JButton createRedoButton() {
        JButton button = new JButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("redo.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.setToolTipText("Redo");
        button.setEnabled(false); // Initial state
        button.addActionListener(this::handleRedo);
        button.addActionListener(e -> statusMessageHandler.setStatusMessage("Redo last drawing operation."));

        return button;
    }

    private JButton createPencilButton() {
        JButton button = new JButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("pencil.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.setToolTipText("Draw with Pencil");
        button.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.PENCIL));
        button.addActionListener(e -> statusMessageHandler.setStatusMessage("Pencil selected."));

        return button;
    }

    private JButton createLineButton() {
        JButton button = new JButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.setToolTipText("Draw a Line");
        button.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.LINE));
        button.addActionListener(e -> statusMessageHandler.setStatusMessage("Line selected."));

        return button;
    }

    private JButton createColorButton() {
        JButton button = new JButton();
        Icon colorIcon = getColorIcon(canvas.getDrawingColor());

        // Set the icon to the button
        Color color = canvas.getDrawingColor();
        button.setIcon(getColorIcon(color));
        button.setToolTipText(String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
        button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color",
                    button.getBackground());
            if (newColor != null) {
                button.setIcon(getColorIcon(newColor));
                // Update the hex color label
                button.setToolTipText(String.format("#%02X%02X%02X",
                        newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                // Update the canvas drawing color
                canvas.setDrawingColor(newColor);
            }
        });
        return button;
    }

    private JButton createFillColorButton() {
        JButton button = new JButton();
        Color color = canvas.getFillColor();
        button.setIcon(getColorIcon(color));
        button.setToolTipText(String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));

        button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    this,
                    "Choose Fill Color",
                    canvas.getFillColor());
            if (newColor != null) {
                canvas.setFillColor(newColor);
                button.setIcon(getColorIcon(newColor));
            }
        });

        return button;
    }

    private JToggleButton createSelectButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("select.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.setToolTipText("Selection Tool");
        button.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.SELECTION));
        //toolGroup.add(button); // Add to the button group for mutual exclusivity
        return button;
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

    private void updateDrawingColorButton(Color color) {
        colorButton.setIcon(getColorIcon(color));
        colorButton.setToolTipText(String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
        colorButton.repaint();
    }

    private void updateFillColorButton(Color color) {
        fillColorButton.setIcon(getColorIcon(color));
        fillColorButton.setToolTipText(String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
        fillColorButton.repaint();
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

    @Override
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo) {
        undoButton.setEnabled(canUndo);
        redoButton.setEnabled(canRedo);
    }

}