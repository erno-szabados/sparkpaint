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
        this.add(createPencilButton());
        this.add(createLineButton());
        this.add(createFillButton());
        this.add(createRectangleButton());
        this.add(createCircleButton());
        this.add(createEllipseButton());
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
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.PENCIL);
            statusMessageHandler.setStatusMessage("Pencil selected.");
        });

        return button;
    }

    private JButton createLineButton() {
        JButton button = new JButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.setToolTipText("Draw a Line");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.LINE);
            statusMessageHandler.setStatusMessage("Line selected.");
        });

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

    private JButton createSelectButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("select.png", IconWidth, IconHeight);
        JButton button = new JButton(icon);
        button.setToolTipText("Selection Tool");
        button.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.SELECTION));
        return button;
    }

    private JButton createBiStateButton(String toolName,
                                              Icon outlineIcon, Icon filledIcon,
                                              DrawingCanvas.Tool outlineTool, DrawingCanvas.Tool filledTool) {
        JButton button = new JButton();
        button.setIcon(outlineIcon);

        final boolean[] isFilled = {false};
        button.addActionListener(e -> {
            if (canvas.getCurrentTool() == (isFilled[0] ? filledTool : outlineTool)) {
                // Only toggle if this shape tool is already selected
                isFilled[0] = !isFilled[0];
                button.setIcon(isFilled[0] ? filledIcon : outlineIcon);
                canvas.setCurrentTool(isFilled[0] ? filledTool : outlineTool);
            } else {
                // First selection of this tool - always start with outline
                isFilled[0] = false;
                button.setIcon(outlineIcon);
                canvas.setCurrentTool(outlineTool);
            }
            statusMessageHandler.setStatusMessage("Selected " + toolName + (isFilled[0] ? " (filled)" : " (outline)"));
        });


        return button;
    }

    private JButton createRectangleButton() {
        return createBiStateButton(
                "Rectangle",
                // Replace these placeholders with the actual rectangle outline and filled icons
                IconLoader.loadAndScaleIcon("rect-outline.png", IconWidth, IconHeight),
                IconLoader.loadAndScaleIcon("rect-filled.png", IconWidth, IconHeight),
                DrawingCanvas.Tool.RECTANGLE_OUTLINE,
                DrawingCanvas.Tool.RECTANGLE_FILLED
        );
    }

    private JButton createCircleButton() {
        return createBiStateButton(
                "Circle",
                // Replace these placeholders with the actual circle outline and filled icons
                IconLoader.loadAndScaleIcon("circle-outline.png", IconWidth, IconHeight),
                IconLoader.loadAndScaleIcon("circle-filled.png", IconWidth, IconHeight),
                DrawingCanvas.Tool.CIRCLE_OUTLINE,
                DrawingCanvas.Tool.CIRCLE_FILLED
        );
    }

    private JButton createEllipseButton() {
        return createBiStateButton(
                "Ellipse",
                // Replace these placeholders with the actual ellipse outline and filled icons
                IconLoader.loadAndScaleIcon("ellipse-outline.png", IconWidth, IconHeight),
                IconLoader.loadAndScaleIcon("ellipse-filled.png", IconWidth, IconHeight),
                DrawingCanvas.Tool.ELLIPSE_OUTLINE,
                DrawingCanvas.Tool.ELLIPSE_FILLED
        );
    }


    private JButton createFillButton() {
        JButton fillButton = new JButton();
        fillButton.setIcon(IconLoader.loadAndScaleIcon("floodfill.png", IconWidth, IconHeight));
        fillButton.setToolTipText("Fill Tool");
        fillButton.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.FILL);
            statusMessageHandler.setStatusMessage("Fill tool selected");
        });
        //toolGroup.add(fillButton);
        return fillButton;
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