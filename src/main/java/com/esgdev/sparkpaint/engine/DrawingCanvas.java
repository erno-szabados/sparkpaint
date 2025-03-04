package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingCanvas extends JPanel {
    public enum Tool {
        PENCIL,
        LINE,
        RECTANGLE_OUTLINE,
        RECTANGLE_FILLED,
        CIRCLE_OUTLINE,
        CIRCLE_FILLED,
        SELECTION,
        FILL
    }

    private Image image;
    private Graphics2D graphics;
    private BufferedImage tempCanvas; // Temporary canvas for drawing previews
    private BufferedImage selectionContent;
    private int startX, startY;
    private Tool currentTool = Tool.PENCIL;
    public static final int MAX_LINE_THICKNESS = 20;
    private static final int FILL_EPSILON = 30; // Tolerance for color matching (flood fill)
    private String currentFilePath;
    private Color drawingColor = Color.BLACK;
    private Color fillColor = Color.WHITE; //
    private Color canvasBackground = Color.WHITE;
    private float lineThickness = 2.0f;
    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<>();
    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    private final Stack<BufferedImage> undoStack = new Stack<>();
    private final Stack<BufferedImage> redoStack = new Stack<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners = new ArrayList<>();
    private final List<ClipboardChangeListener> clipboardChangeListeners = new ArrayList<>();

    private static final int MAX_HISTORY_SIZE = 16;
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;

    private Rectangle selectionRectangle;
    private boolean isSelecting = false;
    private boolean isDragging = false;
    private Point originalSelectionLocation;
    private Point dragOffset = null; // relative mouse position during dragging.
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor CROSSHAIR_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);

    public DrawingCanvas() {
        setPreferredSize(new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
        setBackground(Color.WHITE);

        ToolMouseAdapter toolMouseAdapter = new ToolMouseAdapter();
        addMouseListener(toolMouseAdapter);
        addMouseMotionListener(toolMouseAdapter);
    }

    public void createNewCanvas(int width, int height, Color backgroundColor) {
        this.canvasBackground = backgroundColor; // Set the background color
        this.drawingColor = Color.BLACK;
        this.fillColor = Color.WHITE;
        // Create new buffered images with new dimensions
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage newTempCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get graphics context from new image
        Graphics2D newGraphics = (Graphics2D) newImage.getGraphics();

        // Copy existing graphics settings if they exist
        if (graphics != null) {
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
            newGraphics.setColor(graphics.getColor());
            newGraphics.setStroke(graphics.getStroke());
        } else {
            // Initialize default settings for first creation
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // Fill with background color
        newGraphics.setColor(backgroundColor);
        newGraphics.fillRect(0, 0, width, height);
        notifyBackgroundColorChanged();

        // Restore previous drawing color if it exists
        if (graphics != null) {
            newGraphics.setColor(graphics.getColor());
        }

        // Update class fields
        if (graphics != null) {
            graphics.dispose(); // Clean up old graphics context
        }


        notifyDrawingColorChanged();
        notifyFillColorChanged();

        image = newImage;
        graphics = newGraphics;
        tempCanvas = newTempCanvas;
        currentFilePath = null;

        // Update panel size
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
        clearHistory();
    }


    public void saveToFile(File file) throws IOException {
        // Get the image from the canvas
        BufferedImage imageToSave = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );

        // Draw the current canvas state to the new image
        Graphics2D g = imageToSave.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Get the file extension
        String fileName = file.getName().toLowerCase();
        String formatName = "png"; // default format

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            formatName = "jpeg";
            // Convert to RGB for JPEG (no alpha channel)
            BufferedImage rgbImage = new BufferedImage(
                    imageToSave.getWidth(),
                    imageToSave.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D rgbGraphics = rgbImage.createGraphics();
            rgbGraphics.drawImage(imageToSave, 0, 0, Color.WHITE, null);
            rgbGraphics.dispose();
            imageToSave = rgbImage;
        }

        // Save the image
        if (!ImageIO.write(imageToSave, formatName, file)) {
            throw new IOException("No appropriate writer found for format: " + formatName);
        }

        currentFilePath = file.getAbsolutePath();
    }

    public void loadFromFile(File file) throws IOException {
        BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Failed to load image: " + file.getName());
        }

        // Store current graphics settings before creating new image
        Color currentColor = graphics != null ? graphics.getColor() : Color.BLACK;
        Stroke currentStroke = graphics != null ? graphics.getStroke() : new BasicStroke(1);

        // Create a new buffered image with ARGB support
        image = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        graphics = (Graphics2D) image.getGraphics();

        // Restore graphics settings
        graphics.setColor(currentColor);
        graphics.setStroke(currentStroke);

        // Draw the loaded image
        graphics.drawImage(loadedImage, 0, 0, null);

        currentFilePath = file.getAbsolutePath();
        setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        repaint();
        clearHistory();
    }


    public String getCurrentFilePath() {
        return currentFilePath;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Clear the canvas with the background color
        g.setColor(canvasBackground);
        g.fillRect(0, 0, getWidth(), getHeight());


        // Draw the permanent canvas
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }

        // Draw the temporary canvas on top
        if (tempCanvas != null) {
            g.drawImage(tempCanvas, 0, 0, null);
        }

        if (currentTool == Tool.SELECTION && selectionRectangle != null) {
            Graphics2D g2d = (Graphics2D) g;

            drawSelectionRectangle(g2d);

            // If dragging, draw the selection content at current position
            if (isDragging && selectionContent != null) {
                // Show the background color in the original position
                g2d.setColor(canvasBackground);
                g2d.fillRect(
                        originalSelectionLocation.x,
                        originalSelectionLocation.y,
                        selectionRectangle.width,
                        selectionRectangle.height
                );


                if (selectionContent != null && selectionContent.getWidth() > 0 && selectionContent.getHeight() > 0) {
                    g2d.drawImage(selectionContent,
                            selectionRectangle.x,
                            selectionRectangle.y,
                            null
                    );
                }

                if (selectionRectangle != null && selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                    drawSelectionRectangle(g2d);
                }
            }
        }
    }

    private void drawSelectionRectangle(Graphics2D g2d) {
        // Draw the dotted border
        float[] dashPattern = {5, 5}; // Define a pattern: 5px dash, 5px gap
        BasicStroke dottedStroke = new BasicStroke(
                1,                       // Line Width
                BasicStroke.CAP_BUTT,    // End-cap style
                BasicStroke.JOIN_MITER,  // Join style
                10.0f,                   // Miter limit
                dashPattern,             // Dash pattern (dotted line)
                0                        // Dash phase
        );
        g2d.setColor(Color.BLACK);
        g2d.setStroke(dottedStroke);
        g2d.draw(selectionRectangle);
    }

    // Initialize the canvas image and graphics object
    private void initializeCanvas() {
        image = createImage(getWidth(), getHeight());
        graphics = (Graphics2D) image.getGraphics();

        // Set initial properties for the graphics object
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        graphics.setColor(drawingColor);
        graphics.setStroke(new BasicStroke(2));
        clearHistory();
    }

    // Save the current canvas state to a temporary buffer
    private void saveCanvasState() {
        tempCanvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempGraphics = tempCanvas.createGraphics();
        tempGraphics.drawImage(image, 0, 0, null); // Copy the permanent canvas to the temporary canvas
        tempGraphics.dispose();
    }


    public void setDrawingColor(Color color) {
        if (color != null && !color.equals(this.drawingColor)) {
            this.drawingColor = color;
            notifyDrawingColorChanged();
        }

        if (graphics != null) {
            graphics.setColor(color);
        }
    }

    public Color getDrawingColor() {
        return drawingColor;
    }

    // New Method: Set background color
    public void setFillColor(Color color) {
        if (color != null && !color.equals(this.fillColor)) {
            this.fillColor = color;
            notifyFillColorChanged();
        }

        repaint();
    }

    // New Method: Get background color
    public Color getFillColor() {
        return fillColor;
    }

    public void setCanvasBackground(Color color) {
        if (color != null && !color.equals(this.canvasBackground)) {
            this.canvasBackground = color;
            notifyBackgroundColorChanged();
        }

        repaint();
    }

    public Color getCanvasBackground() {
        return canvasBackground;
    }

    public void setLineThickness(float thickness) {
        this.lineThickness = Math.min(thickness, MAX_LINE_THICKNESS);
        if (graphics != null) {
            graphics.setStroke(new BasicStroke(thickness));
        }
    }

    public float getLineThickness() {
        return lineThickness;
    }

    private void drawRectangle(Graphics2D g, int x1, int y1, int x2, int y2, boolean filled) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);

        if (filled) {
            g.setColor(fillColor);
            g.fillRect(x, y, width, height);
        }
        g.setColor(drawingColor);
        g.drawRect(x, y, width, height);
    }

    private void drawCircle(Graphics2D g, int x1, int y1, int x2, int y2, boolean filled) {

        // Calculate radius based on the distance to the second point
        int radius = (int) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        // Calculate top-left corner and diameter for the circle
        int topLeftX = x1 - radius;
        int topLeftY = y1 - radius;
        int diameter = radius * 2;

        if (filled) {
            g.setColor(fillColor);
            g.fillOval(topLeftX, topLeftY, diameter, diameter);
        }
        g.setColor(drawingColor);
        g.drawOval(topLeftX, topLeftY, diameter, diameter);
    }

    public void addToolChangeListener(ToolChangeListener listener) {
        toolChangeListeners.add(listener);
    }

    public void removeToolChangeListener(ToolChangeListener listener) {
        toolChangeListeners.remove(listener);
    }

    // Getter for the current tool
    public Tool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
        switch (tool) {
            case PENCIL:
            case LINE:
            case RECTANGLE_OUTLINE:
            case RECTANGLE_FILLED:
            case CIRCLE_OUTLINE:
            case CIRCLE_FILLED:
            case SELECTION:
            case FILL:
                setCursor(CROSSHAIR_CURSOR); // Crosshair cursor for drawing tools
                break;
            default:
                setCursor(DEFAULT_CURSOR); // Default cursor

                break;
        }
        // Notify all listeners
        for (ToolChangeListener listener : toolChangeListeners) {
            listener.onToolChanged(tool);
        }
    }

    public void addCanvasPropertyChangeListener(CanvasPropertyChangeListener listener) {
        propertyChangeListeners.add(listener);
    }

    public void removeCanvasPropertyChangeListener(CanvasPropertyChangeListener listener) {
        propertyChangeListeners.remove(listener);
    }

    private void notifyDrawingColorChanged() {
        for (CanvasPropertyChangeListener listener : propertyChangeListeners) {
            listener.onDrawingColorChanged(drawingColor);
        }
    }

    private void notifyFillColorChanged() {
        for (CanvasPropertyChangeListener listener : propertyChangeListeners) {
            listener.onFillColorChanged(fillColor);
        }
    }

    private void notifyBackgroundColorChanged() {
        for (CanvasPropertyChangeListener listener : propertyChangeListeners) {
            listener.onBackgroundColorChanged(canvasBackground);
        }
    }

    private void saveToUndoStack() {
        if (image != null) {
            BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            undoStack.push(copy);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeFirst(); // Keep the stack size within the limit
            }
            notifyUndoRedoStateChanged();
        }
    }

    private void eraseSelection() {
        // Clear the selected region by filling it with the canvas background color.
        graphics.setColor(canvasBackground);
        graphics.fillRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width, selectionRectangle.height);
    }

    public void cutSelection() {
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        copySelection(false);
        eraseSelection();
        selectionRectangle = null; // Clear selection after cutting.
        repaint();
        notifyClipboardStateChanged();
    }

    public void copySelection() {
        copySelection(true);
    }

    private void copySelection(boolean clearRectangle) {
        if (selectionRectangle == null
                || selectionRectangle.width <= 0
                || selectionRectangle.height <= 0) {
            return;
        }
        // Extract the selected region from the canvas image.
        BufferedImage canvasImage = (BufferedImage) image;
        BufferedImage selectionImage = canvasImage.getSubimage(
                selectionRectangle.x,
                selectionRectangle.y,
                selectionRectangle.width,
                selectionRectangle.height
        );
        ImageSelection.copyImage(selectionImage);
        if (clearRectangle) {
            selectionRectangle = null;
            repaint();
        }
        notifyClipboardStateChanged();
    }

    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        BufferedImage pastedImage = ImageSelection.pasteImage();
        if (pastedImage != null) {
            if (image == null || graphics == null) {
                createNewCanvas(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, canvasBackground);
                saveToUndoStack();
            }
            // For simplicity, paste at (0,0). Modify as needed for custom positioning.
            graphics.drawImage(pastedImage, 0, 0, null);
            repaint();
            notifyClipboardStateChanged();
        }
    }

    public boolean hasSelection() {
        return selectionRectangle != null;
    }

    public boolean canPaste() {
        try {
            // Includes a workaround for JDK-6606476 (Uncatchable exception printed to console for certain clipboard contents)
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] flavors = clipboard.getAvailableDataFlavors();

            // Explicitly check only for image flavor without constructing other flavors
            for (DataFlavor flavor : flavors) {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            tempCanvas = null;
            // Save current state to redo stack
            BufferedImage currentState = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = currentState.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            redoStack.push(currentState);

            // Pop previous state from undo stack and set it as current image
            image = undoStack.pop();
            graphics = (Graphics2D) image.getGraphics();
            repaint();
            notifyUndoRedoStateChanged();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            tempCanvas = null;
            // Save current state to undo stack
            BufferedImage currentState = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = currentState.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            undoStack.push(currentState);

            // Pop next state from redo stack and set it as current image
            image = redoStack.pop();
            graphics = (Graphics2D) image.getGraphics();
            repaint();
            notifyUndoRedoStateChanged();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }


    private void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private class ToolMouseAdapter extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            if (currentTool == Tool.SELECTION && selectionRectangle != null) {
                // Change to hand cursor when over the selection rectangle
                if (selectionRectangle.contains(e.getPoint())) {
                    setCursor(HAND_CURSOR);
                } else {
                    setCursor(DEFAULT_CURSOR);
                }
            }
        }


        @Override
        public void mousePressed(MouseEvent e) {
            startX = e.getX();
            startY = e.getY();

            // Initialize the canvas if not done already
            if (graphics == null) {
                initializeCanvas();
            }

            saveToUndoStack();
            //If tool is LINE or RECTANGLE, save the current canvas state
            switch (currentTool) {
                case PENCIL:
                    graphics.setColor(drawingColor);
                    break;
                case LINE:
                case RECTANGLE_OUTLINE:
                case RECTANGLE_FILLED:
                case CIRCLE_OUTLINE:
                case CIRCLE_FILLED:
                    saveCanvasState();
                    break;
                case SELECTION:
                    // check if click is inside existing selection
                    if (selectionRectangle != null && (selectionRectangle.contains(e.getPoint()))) {
                        isDragging = true;
                        setCursor(HAND_CURSOR);
                        dragOffset = new Point(
                                e.getX() - selectionRectangle.x,
                                e.getY() - selectionRectangle.y);
                        // Store the original location when starting drag
                        originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                    } else {
                        selectionRectangle = new Rectangle(startX, startY, 0, 0);
                        isSelecting = true;
                    }
                case FILL:
                    saveToUndoStack();
                    BufferedImage bufferedImage = (BufferedImage) image;
                    Color targetColor = new Color(bufferedImage.getRGB(startX, startY));
                    floodFill(startX, startY, targetColor, drawingColor, FILL_EPSILON);

                    // Notify listeners that we've made a change
                    notifyUndoRedoStateChanged();
                    repaint();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            switch (currentTool) {
                case SELECTION:
                    if (isDragging) {
                        // Update selection rectangle position
                        int newX = e.getX() - dragOffset.x;
                        int newY = e.getY() - dragOffset.y;
                        selectionRectangle.setLocation(newX, newY);
                        repaint();
                    } else if (isSelecting) {
                        drawSelection(e);
                        repaint();
                    }
                    break;
                case PENCIL:// Pencil drawing
                    int x = e.getX();
                    int y = e.getY();

                    if (graphics != null) {
                        graphics.setStroke(new BasicStroke(lineThickness));
                        graphics.drawLine(startX, startY, x, y);
                        repaint();
                        startX = x; // Update start point for continuous freehand drawing
                        startY = y;
                    }
                    break;
                case LINE:
                case RECTANGLE_OUTLINE:
                case RECTANGLE_FILLED:
                case CIRCLE_OUTLINE:
                case CIRCLE_FILLED:// Draw a temporary preview of the shape
                    if (tempCanvas != null) {
                        Graphics2D tempGraphics = tempCanvas.createGraphics();
                        tempGraphics.drawImage(image, 0, 0, null); // Restore the original canvas
                        tempGraphics.setColor(drawingColor);
                        tempGraphics.setStroke(new BasicStroke(lineThickness));
                        // Preview the appropriate shape
                        if (currentTool == Tool.LINE) {
                            tempGraphics.drawLine(startX, startY, e.getX(), e.getY());
                        } else if (currentTool == Tool.RECTANGLE_OUTLINE ||
                                currentTool == Tool.RECTANGLE_FILLED) {
                            int rectX = Math.min(startX, e.getX());
                            int rectY = Math.min(startY, e.getY());
                            int rectWidth = Math.abs(e.getX() - startX);
                            int rectHeight = Math.abs(e.getY() - startY);
                            drawRectangle(tempGraphics, rectX, rectY, rectX + rectWidth, rectY + rectHeight, currentTool == Tool.RECTANGLE_FILLED);
                        } else if (currentTool == Tool.CIRCLE_OUTLINE ||
                                currentTool == Tool.CIRCLE_FILLED) {
                            drawCircle(tempGraphics, startX, startY, e.getX(), e.getY(), currentTool == Tool.CIRCLE_FILLED);
                        }

                        tempGraphics.dispose();
                        repaint(); // Update the canvas to show the temporary shape
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int endX = e.getX();
            int endY = e.getY();

            // Finalize the shape based on the selected tool
            if (graphics != null) {
                // Set the color and stroke for the final shape
                graphics.setColor(drawingColor);
                graphics.setStroke(new BasicStroke(lineThickness));
                switch (currentTool) {
                    case SELECTION:
                        if (isSelecting) {
                            notifyClipboardStateChanged();
                            isSelecting = false;
                            // Capture the selected content
                            if (selectionRectangle != null && selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                                selectionContent = new BufferedImage(
                                        selectionRectangle.width,
                                        selectionRectangle.height,
                                        BufferedImage.TYPE_INT_ARGB
                                );
                                Graphics2D g = selectionContent.createGraphics();
                                g.drawImage(image,
                                        -selectionRectangle.x, -selectionRectangle.y,
                                        null
                                );
                                g.dispose();
                            } else {
                                selectionContent = null;
                            }
                            repaint();
                        } else if (isDragging) {
                            isDragging = false;
                            // Only change cursor if still over selection
                            if (selectionRectangle.contains(e.getPoint())) {
                                setCursor(HAND_CURSOR);
                            } else {
                                setCursor(DEFAULT_CURSOR);
                            }
                            if (selectionContent != null) {
                                saveToUndoStack();
                                // Clear the original area with canvas background color
                                graphics.setColor(canvasBackground);
                                graphics.fillRect(
                                        originalSelectionLocation.x,
                                        originalSelectionLocation.y,
                                        selectionRectangle.width,
                                        selectionRectangle.height
                                );

                                graphics.drawImage(selectionContent,
                                        selectionRectangle.x,
                                        selectionRectangle.y,
                                        null
                                );
                                repaint();
                            }
                        }
                        break;
                    case LINE:
                        // Finalize the line
                        graphics.drawLine(startX, startY, endX, endY);
                        repaint();
                        break;
                    case RECTANGLE_OUTLINE:
                    case RECTANGLE_FILLED:
                        // Finalize the rectangle
                        int rectX = Math.min(startX, endX);
                        int rectY = Math.min(startY, endY);
                        int rectWidth = Math.abs(endX - startX);
                        int rectHeight = Math.abs(endY - startY);
                        drawRectangle(graphics, rectX, rectY, rectX + rectWidth, rectY + rectHeight, currentTool == Tool.RECTANGLE_FILLED);
                        repaint();
                        break;
                    case CIRCLE_OUTLINE:
                    case CIRCLE_FILLED:
                        // Draw the circle on the permanent canvas
                        drawCircle(graphics, startX, startY, endX, endY, currentTool == Tool.CIRCLE_FILLED);
                        repaint();

                        break;
                    default:
                        break;
                }

                // Clear the temporary canvas
                tempCanvas = null;
                redoStack.clear();
            }
        }

        private void drawSelection(MouseEvent e) {
            int currentX = e.getX();
            int currentY = e.getY();
            // Update selection rectangle dimensions
            int x = Math.min(startX, currentX);
            int y = Math.min(startY, currentY);
            int width = Math.abs(currentX - startX);
            int height = Math.abs(currentY - startY);
            selectionRectangle.setBounds(x, y, width, height);
        }
    }

    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        undoRedoChangeListeners.add(listener);
    }

    public void removeUndoRedoChangeListener(UndoRedoChangeListener listener) {
        undoRedoChangeListeners.remove(listener);
    }

    private void notifyUndoRedoStateChanged() {
        boolean canUndo = canUndo();
        boolean canRedo = canRedo();
        for (UndoRedoChangeListener listener : undoRedoChangeListeners) {
            listener.undoRedoStateChanged(canUndo, canRedo);
        }
    }

    // Add listener management methods
    public void addClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardChangeListeners.add(listener);
    }

    public void removeClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardChangeListeners.remove(listener);
    }

    // Method to notify listeners
    private void notifyClipboardStateChanged() {
        boolean canCopy = hasSelection();
        boolean canPaste = canPaste();

        for (ClipboardChangeListener listener : clipboardChangeListeners) {
            listener.clipboardStateChanged(canCopy, canPaste);
        }
    }

    public void floodFill(int x, int y, Color targetColor, Color replacementColor, int epsilon) {
        if (image == null) {
            return;
        }

        BufferedImage bufferedImage = (BufferedImage) image;
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        if (bufferedImage.getRGB(x, y) == replacementRGB) {
            return; // Avoid infinite loop if fill color is same as target
        }


        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            x = p.x;
            y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height || bufferedImage.getRGB(x, y) == replacementRGB) {
                continue;
            }

            if (colorDistance(bufferedImage.getRGB(x, y), targetRGB) <= epsilon) {

                bufferedImage.setRGB(x, y, replacementRGB);

                stack.push(new Point(x + 1, y));
                stack.push(new Point(x - 1, y));
                stack.push(new Point(x, y + 1));
                stack.push(new Point(x, y - 1));

            }
        }

        repaint();

    }

    private double colorDistance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        double deltaR = r1 - r2;
        double deltaG = g1 - g2;
        double deltaB = b1 - b2;


        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
    }

}