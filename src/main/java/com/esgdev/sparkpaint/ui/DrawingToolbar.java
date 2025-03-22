package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.CanvasPropertyChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.history.UndoRedoChangeListener;

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
        this.setFloatable(false); // Disable floating toolbar

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
        button.putClientProperty("tool", DrawingCanvas.Tool.BRUSH);
        button.setToolTipText("Draw with Brush");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.BRUSH);
            statusMessageHandler.setStatusMessage("Brush selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createPencilButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("pencil.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.PENCIL);
        button.setToolTipText("Draw with Pencil");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.PENCIL);
            statusMessageHandler.setStatusMessage("Pencil selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createTextButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("text.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.TEXT);
        button.setToolTipText("Draw text");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.TEXT);
            statusMessageHandler.setStatusMessage("Text tool selected.");
        });
        toolGroup.add(button);

        return button;
    }

    private JToggleButton createLineButton() {
        JToggleButton button = new JToggleButton();
        ImageIcon icon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);
        button.setIcon(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.LINE);
        button.setToolTipText("Draw a Line");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.LINE);
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
        button.setToolTipText(String.format("Draw color - #%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
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
        button.setToolTipText(String.format("Fill color - #%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));


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
        button.putClientProperty("tool", DrawingCanvas.Tool.RECTANGLE_SELECTION);
        button.setToolTipText("Rectangle Selection Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE_SELECTION);
            statusMessageHandler.setStatusMessage("Rectangle selection tool selected.");});
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createFreehandSelectButton() {
        // FIXME new icon
        ImageIcon icon = IconLoader.loadAndScaleIcon("lasso.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.FREEHAND_SELECTION);
        button.setToolTipText("Freehand Selection Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.FREEHAND_SELECTION);
            statusMessageHandler.setStatusMessage("Freehand Selection tool selected.");});
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createEyedropperButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("eyedropper.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.EYEDROPPER);
        button.setToolTipText("Eyedropper Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.EYEDROPPER);
            statusMessageHandler.setStatusMessage("Eyedropper tool selected.");});
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createRectangleButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("rect-outline.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.RECTANGLE);
        button.setToolTipText("Rectangle Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE);
            statusMessageHandler.setStatusMessage("Rectangle tool selected.");});
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createCircleButton() {
        ImageIcon icon = IconLoader.loadAndScaleIcon("circle-outline.png", IconWidth, IconHeight);
        JToggleButton button = new JToggleButton(icon);
        button.putClientProperty("tool", DrawingCanvas.Tool.CIRCLE);
        button.setToolTipText("Circle Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.CIRCLE);
            statusMessageHandler.setStatusMessage("Circle tool selected.");});
        toolGroup.add(button);
        return button;
    }

    private JToggleButton createFillButton() {
        JToggleButton button = new JToggleButton();
        button.putClientProperty("tool", DrawingCanvas.Tool.FILL);
        button.setIcon(IconLoader.loadAndScaleIcon("floodfill.png", IconWidth, IconHeight));
        button.setToolTipText("Fill Tool");
        button.addActionListener(e -> {
            canvas.setCurrentTool(DrawingCanvas.Tool.FILL);
            statusMessageHandler.setStatusMessage("Fill tool selected");
        });
        toolGroup.add(button);
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

    @Override
    public void onToolChanged(DrawingCanvas.Tool newTool) {
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