package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.ui.CanvasPropertyChangeListener;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingCanvas extends JPanel {
    public static final int MAX_LINE_THICKNESS = 20;
    private String currentFilePath;
    private Color drawingColor = Color.BLACK; // Default color
    private Color fillColor = Color.WHITE; //
    private Color canvasBackground = Color.WHITE;
    private float lineThickness = 2.0f; // Default line thickness
    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<>();
    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();

    private final Stack<BufferedImage> undoStack = new Stack<>();
    private final Stack<BufferedImage> redoStack = new Stack<>();
    private static final int MAX_HISTORY_SIZE = 16;

    public enum Tool {
        PENCIL,
        LINE,
        RECTANGLE_OUTLINE,
        RECTANGLE_FILLED,
        CIRCLE_OUTLINE,
        CIRCLE_FILLED
    }

    private Image image;
    private Graphics2D graphics;
    private BufferedImage tempCanvas; // Temporary canvas for drawing previews
    private int startX, startY;
    private Tool currentTool = Tool.PENCIL;

    public DrawingCanvas() {
        setPreferredSize(new Dimension(800, 600));
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
        if (tool == Tool.PENCIL || tool == Tool.LINE || tool == Tool.RECTANGLE_OUTLINE || tool == Tool.RECTANGLE_FILLED) {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); // Crosshair cursor for drawing tools
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // Default cursor
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
            System.out.println("undo stack:" + undoStack.size());
            graphics = (Graphics2D) image.getGraphics();
            repaint();
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
            System.out.println("redo stack:" + redoStack.size());
            graphics = (Graphics2D) image.getGraphics();
            repaint();
        }
    }

    private void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private class ToolMouseAdapter extends MouseAdapter {
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

        @Override
        public void mouseDragged(MouseEvent e) {
            switch (currentTool) {
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
    }
}