package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.*;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class DrawingCanvas extends JPanel {

    public enum Tool {
        PENCIL,
        LINE,
        RECTANGLE_OUTLINE,
        RECTANGLE_FILLED,
        CIRCLE_OUTLINE,
        CIRCLE_FILLED,
        ELLIPSE_OUTLINE,
        ELLIPSE_FILLED,
        SELECTION,
        FILL,
        EYEDROPPER
    }
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;
    public static final int MAX_LINE_THICKNESS = 20;

    private Image image;
    private Graphics2D graphics;
    private BufferedImage tempCanvas; // Temporary canvas for drawing previews
    private BufferedImage selectionContent;
    private int startX, startY;
    private Tool currentTool = Tool.PENCIL;
    private String currentFilePath;
    private Color drawingColor = Color.BLACK;
    private Color fillColor = Color.WHITE; //
    private Color canvasBackground = Color.WHITE;
    private float lineThickness = 2.0f;
    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<>();
    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    private final EnumMap<Tool, DrawingTool> tools = new EnumMap<>(Tool.class);
    private final HistoryManager historyManager;
    private final ClipboardManager clipboardManager;
    private Rectangle selectionRectangle;

    public DrawingCanvas() {
        setPreferredSize(new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
        this.canvasBackground = Color.WHITE;
        setBackground(canvasBackground);
        historyManager = new HistoryManager();
        clipboardManager = new ClipboardManager(this);
        MouseAdapter canvasMouseAdapter = new CanvasMouseAdapter();
        addMouseListener(canvasMouseAdapter);
        addMouseMotionListener(canvasMouseAdapter);
        initTools();
        createNewCanvas(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, canvasBackground);
    }

    /**
     * Creates a new canvas with the specified dimensions and background color.
     * @param width width of the canvas
     * @param height height of the canvas
     * @param canvasBackground background color of the canvas
     */
    public void createNewCanvas(int width, int height, Color canvasBackground) {
        this.canvasBackground = canvasBackground;
        this.drawingColor = Color.BLACK;
        this.fillColor = Color.WHITE;
        // Create new buffered images with new dimensions
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage newTempCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get graphics context from new image
        Graphics2D newGraphics = (Graphics2D) newImage.getGraphics();
        // Fill with background color
        newGraphics.setColor(canvasBackground);
        newGraphics.fillRect(0, 0, width, height);
        notifyBackgroundColorChanged();

        // Copy existing graphics settings if they exist
        if (graphics != null) {
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
            newGraphics.setColor(graphics.getColor());
            newGraphics.setStroke(graphics.getStroke());
            graphics.dispose();
        } else {
            // Initialize default settings for first creation
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            newGraphics.setColor(drawingColor);
            newGraphics.setStroke(new BasicStroke(lineThickness));
        }

        image = newImage;
        graphics = newGraphics;
        tempCanvas = newTempCanvas;
        currentFilePath = null;

        notifyDrawingColorChanged();
        notifyFillColorChanged();
        clearHistory();
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    public Image getImage() {
        return image;
    }

    public Graphics2D getCanvasGraphics() {
        return (Graphics2D) graphics.create();
    }

    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public void setSelectionRectangle(Rectangle selectionRectangle) {
        this.selectionRectangle = selectionRectangle;
    }

    public Image getSelectionContent() {
        return selectionContent;
    }

    public void setSelectionContent(BufferedImage selectionContent) {
        this.selectionContent = selectionContent;
    }

    public void setTempCanvas(BufferedImage tempCanvas) {
        this.tempCanvas = tempCanvas;
    }

    public void saveToFile(File file) throws IOException {
        // Get the image from the canvas
        BufferedImage imageToSave = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

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
                    BufferedImage.TYPE_INT_RGB);
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

        if (currentTool != Tool.SELECTION || selectionRectangle == null)
            return;

        Graphics2D g2d = (Graphics2D) g;

        DrawingTool tool = tools.get(currentTool);
        if (tool instanceof SelectionTool) {
            ((SelectionTool) tool).drawSelection(g2d);
        }
    }

    private void initTools() {
        tools.put(Tool.LINE, new LineTool(this));
        tools.put(Tool.RECTANGLE_FILLED, new RectangleTool(this));
        tools.put(Tool.CIRCLE_FILLED, new CircleTool(this));
        tools.put(Tool.ELLIPSE_FILLED, new EllipseTool(this));
        tools.put(Tool.FILL, new FillTool(this));
        tools.put(Tool.EYEDROPPER, new EyedropperTool(this));
        tools.put(Tool.PENCIL, new PencilTool(this));
        tools.put(Tool.SELECTION, new SelectionTool(this));
    }

    // Save the current canvas state to a temporary buffer
    public void saveCanvasState() {
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

    private void drawCircle(Graphics2D g, int x1, int y1, int x2, int y2, boolean isFilled) {

        // Calculate radius based on the distance to the second point
        int radius = (int) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        // Calculate top-left corner and diameter for the circle
        int topLeftX = x1 - radius;
        int topLeftY = y1 - radius;
        int diameter = radius * 2;

        if (isFilled) {
            g.setColor(fillColor);
            g.fillOval(topLeftX, topLeftY, diameter, diameter);
        }
        g.setColor(drawingColor);
        g.drawOval(topLeftX, topLeftY, diameter, diameter);
    }

    private void drawEllipse(Graphics2D g, int startX, int startY, int endX, int endY, boolean isFilled) {
        // Calculate the top-left corner, width, and height based on opposing corners
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);

        if (isFilled) {
            g.setColor(fillColor);
            g.fillOval(x, y, width, height);
        }
        g.setColor(drawingColor);
        g.drawOval(x, y, width, height);
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

    // Copy - paste

    public void cutSelection() {
        clipboardManager.cutSelection();
    }

    public void copySelection() {
        clipboardManager.copySelection();
    }

    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        clipboardManager.pasteSelection();
    }

    public boolean hasSelection() {
        return clipboardManager.hasSelection();
    }

    public boolean canPaste() {
        return clipboardManager.canPaste();
    }

    public void addClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardManager.addClipboardChangeListener(listener);
    }

    public void removeClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardManager.removeClipboardChangeListener(listener);
    }

    public void notifyClipboardStateChanged() {
        clipboardManager.notifyClipboardStateChanged();
    }

    // Undo - redo

    public void saveToUndoStack() {
        historyManager.saveToUndoStack((BufferedImage) image);
    }

    public void undo() {
        image = historyManager.undo((BufferedImage) image);
        graphics = (Graphics2D) image.getGraphics();
        selectionRectangle = null;
        repaint();
    }

    public void redo() {
        image = historyManager.redo((BufferedImage) image);
        graphics = (Graphics2D) image.getGraphics();
        selectionRectangle = null;
        repaint();
    }

    public boolean canUndo() {
        return historyManager.canUndo();
    }

    public boolean canRedo() {
        return historyManager.canRedo();
    }

    private void clearHistory() {
        historyManager.clearHistory();
    }

    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        historyManager.addUndoRedoChangeListener(listener);
    }


    private class CanvasMouseAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.setCursor();
                tool.mouseMoved(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.mouseDragged(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.setCursor();
                tool.mousePressed(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.mouseReleased(e);
            }
        }
    }
}