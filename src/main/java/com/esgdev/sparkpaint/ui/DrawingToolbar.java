package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.CanvasPropertyChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.history.UndoRedoChangeListener;
import com.esgdev.sparkpaint.engine.tools.ToolManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

public class DrawingToolbar extends JToolBar implements UndoRedoChangeListener, ToolChangeListener {
    public static final int IconWidth = 24;
    public static final int IconHeight = 24;
    private final StatusMessageHandler statusMessageHandler;
    private final DrawingCanvas canvas;
    private final JColorChooser colorChooser;
    private final ButtonGroup toolGroup = new ButtonGroup();

    private JButton colorButton;
    private JButton fillColorButton;
    private JButton undoButton;
    private JButton redoButton;

    public DrawingToolbar(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(JToolBar.VERTICAL);
        this.canvas = canvas;
        this.statusMessageHandler = statusMessageHandler;
        this.colorChooser = new JColorChooser();

        initializeToolbar();

        canvas.addCanvasPropertyChangeListener(new CanvasPropertyChangeListener() {
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
        canvas.addToolChangeListener(this);
    }


    private void initializeToolbar() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setFloatable(false);
        this.setRollover(true);
        this.add(createSelectButton());
        this.add(createFreehandSelectButton());
        // Undo/redo tools
        undoButton = createUndoButton();
        this.add(undoButton);
        redoButton = createRedoButton();
        this.add(redoButton);
        this.addSeparator();
        canvas.addUndoRedoChangeListener(this);
        this.add(createBrushButton());
        this.add(createPencilButton());
        this.add(createTextButton());
        this.add(createLineButton());
        this.add(createRectangleButton());
        this.add(createCircleButton());
        this.addSeparator();
        this.add(createFillButton());
        this.add(createEyedropperButton());
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

    private JToggleButton createBrushButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("brush.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", ToolManager.Tool.BRUSH);
        button.setToolTipText("Draw with Brush");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.BRUSH);
            statusMessageHandler.setStatusMessage("Brush selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createPencilButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("pencil.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", ToolManager.Tool.PENCIL);
        button.setToolTipText("Draw with Pencil");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.PENCIL);
            statusMessageHandler.setStatusMessage("Pencil selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createTextButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("text.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", ToolManager.Tool.TEXT);
        button.setToolTipText("Draw text");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.TEXT);
            statusMessageHandler.setStatusMessage("Text tool selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createLineButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", ToolManager.Tool.LINE);
        button.setToolTipText("Draw a Line");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.LINE);
            statusMessageHandler.setStatusMessage("Line selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JButton createColorButton() {
        JButton button = new JButton();

        // Set the icon to the button
        Color color = canvas.getDrawingColor();
        button.setIcon(getColorIcon(color));
        if (color.getAlpha() == 0) {
            button.setToolTipText("Draw color - Transparent");
        } else {
            button.setToolTipText(String.format("Draw color - #%02X%02X%02X",
                    color.getRed(), color.getGreen(), color.getBlue()));
        }
        button.addActionListener(e -> {
            // Use the shared colorChooser instance
            JDialog dialog = JColorChooser.createDialog(
                    this,
                    "Choose Drawing Color",
                    true,
                    colorChooser,
                    action -> {
                        Color newColor = colorChooser.getColor();
                        button.setIcon(getColorIcon(newColor));
                        button.setToolTipText(String.format("Draw color - #%02X%02X%02X",
                                newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        canvas.setDrawingColor(newColor);
                    },
                    null);

            colorChooser.setColor(canvas.getDrawingColor());
            dialog.setVisible(true);
        });
        return button;
    }

    private JButton createFillColorButton() {
        JButton button = new JButton();
        Color color = canvas.getFillColor();
        button.setIcon(getColorIcon(color));
        if (color.getAlpha() == 0) {
            button.setToolTipText("Fill color - Transparent");
        } else {
            button.setToolTipText(String.format("Fill color - #%02X%02X%02X",
                    color.getRed(), color.getGreen(), color.getBlue()));
        }


        button.addActionListener(e -> {
            // Use the shared colorChooser instance
            JDialog dialog = JColorChooser.createDialog(
                    this,
                    "Choose Fill Color",
                    true,
                    colorChooser,
                    action -> {
                        Color newColor = colorChooser.getColor();
                        button.setIcon(getColorIcon(newColor));
                        button.setToolTipText(String.format("Fill color - #%02X%02X%02X",
                                newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        canvas.setFillColor(newColor);
                    },
                    null);

            colorChooser.setColor(canvas.getFillColor());
            dialog.setVisible(true);
        });

        return button;
    }

    private JToggleButton createSelectButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("select.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", ToolManager.Tool.RECTANGLE_SELECTION);
        button.setToolTipText("Rectangle Selection Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.RECTANGLE_SELECTION);
            statusMessageHandler.setStatusMessage("Rectangle selection tool selected.");
        });
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createFreehandSelectButton() {
        // FIXME new icon
        ImageIcon icon = IconLoader.loadAndScaleIcon("lasso.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", ToolManager.Tool.FREEHAND_SELECTION);
        button.setToolTipText("Freehand Selection Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.FREEHAND_SELECTION);
            statusMessageHandler.setStatusMessage("Freehand Selection tool selected.");
        });
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createEyedropperButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("eyedropper.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", ToolManager.Tool.EYEDROPPER);
        button.setToolTipText("Eyedropper Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.EYEDROPPER);
            statusMessageHandler.setStatusMessage("Eyedropper tool selected.");
        });
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createRectangleButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("rect-outline.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", ToolManager.Tool.RECTANGLE);
        button.setToolTipText("Rectangle Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.RECTANGLE);
            statusMessageHandler.setStatusMessage("Rectangle tool selected.");
        });
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createCircleButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("circle-outline.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", ToolManager.Tool.CIRCLE);
        button.setToolTipText("Circle Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.CIRCLE);
            statusMessageHandler.setStatusMessage("Circle tool selected.");
        });
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createFillButton() {
        JToggleButton button = new JToggleButton();
        button.putClientProperty("tool", ToolManager.Tool.FILL);
        button.setIcon(IconLoader.loadAndScaleIcon("floodfill.png", IconWidth, IconHeight));
        button.setToolTipText("Fill Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(ToolManager.Tool.FILL);
            statusMessageHandler.setStatusMessage("Fill tool selected");
        });
        toolGroup.add(button);
        return button;
    }


    private Icon getColorIcon(Color color) {
        return new Icon() {
            private static final int CHECKER_SIZE = 5; // Size of each checker square

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                // Check if color is transparent (alpha is 0)
                if (color.getAlpha() == 0) {
                    Graphics2D g2d = (Graphics2D) g;
                    // Draw checker pattern for transparency
                    for (int i = x; i < x + IconWidth; i += CHECKER_SIZE) {
                        for (int j = y; j < y + IconHeight; j += CHECKER_SIZE) {
                            if ((i / CHECKER_SIZE + j / CHECKER_SIZE) % 2 == 0) {
                                g2d.setColor(Color.WHITE);
                            } else {
                                g2d.setColor(Color.LIGHT_GRAY);
                            }
                            g2d.fillRect(i, j, CHECKER_SIZE, CHECKER_SIZE);
                        }
                    }
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect(x, y, IconWidth - 1, IconHeight - 1);
                } else {
                    g.setColor(color);
                    g.fillRect(x, y, IconWidth, IconHeight);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(x, y, IconWidth - 1, IconHeight - 1);
                }
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
        if (color.getAlpha() == 0) {
            colorButton.setToolTipText("Draw color - Transparent");
        } else {
            colorButton.setToolTipText(String.format("Draw color - #%02X%02X%02X",
                    color.getRed(), color.getGreen(), color.getBlue()));
        }
        colorButton.repaint();
    }

    private void updateFillColorButton(Color color) {
        fillColorButton.setIcon(getColorIcon(color));
        if (color.getAlpha() == 0) {
            fillColorButton.setToolTipText("Fill color - Transparent");
        } else {
            fillColorButton.setToolTipText(String.format("Fill color - #%02X%02X%02X",
                    color.getRed(), color.getGreen(), color.getBlue()));
        }
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

    @Override
    public void onToolChanged(ToolManager.Tool newTool) {
        Enumeration<AbstractButton> elements = toolGroup.getElements();
        while (elements.hasMoreElements()) {
            AbstractButton button = elements.nextElement();
            if (button.getClientProperty("tool") == newTool) {
                button.setSelected(true);
                break;
            }
        }
    }
}