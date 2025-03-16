package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;
import com.esgdev.sparkpaint.engine.selection.SelectionRenderer;
import com.esgdev.sparkpaint.engine.tools.*;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
import com.esgdev.sparkpaint.io.ClipboardChangeListener;
import com.esgdev.sparkpaint.io.ClipboardManager;
import com.esgdev.sparkpaint.io.FileManager;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * DrawingCanvas is a class that represents a drawing canvas where users can draw shapes, lines, and other graphics.
 * Supports various drawing tools and allows for undo/redo functionality, clipboard operations, and file management.
 */
public class DrawingCanvas extends JPanel {

    public enum Tool {
        BRUSH,
        PENCIL,
        LINE,
        RECTANGLE,
        CIRCLE,
        RECTANGLE_SELECTION,
        FREEHAND_SELECTION,
        FILL,
        TEXT, EYEDROPPER
    }
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;
    public static final int MAX_LINE_THICKNESS = 20;

    private BufferedImage image;
    private Graphics2D graphics;
    // Temporary canvas for drawing previews, tools will manage this, paintComponent will draw it
    private BufferedImage tempCanvas;

    private Tool currentTool = Tool.PENCIL;
    private Color drawingColor = Color.BLACK;
    private Color fillColor = Color.WHITE;
    private Color canvasBackground;
    private float lineThickness = 2.0f;
    private float zoomFactor = 1.0f;

    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<>();
    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    private final EnumMap<Tool, DrawingTool> tools = new EnumMap<>(Tool.class);
    private final HistoryManager historyManager;
    private final SelectionManager selectionManager;
    private final ClipboardManager clipboardManager;
    private final FileManager fileManager;

    /**
     * Default constructor for the DrawingCanvas class.
     */
    public DrawingCanvas() {
        setPreferredSize(new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
        this.canvasBackground = Color.WHITE;
        setBackground(canvasBackground);
        historyManager = new HistoryManager();
        selectionManager = new SelectionManager(this);
        clipboardManager = new ClipboardManager(this);
        fileManager = new FileManager();
        MouseAdapter canvasMouseAdapter = new CanvasMouseAdapter();
        addMouseListener(canvasMouseAdapter);
        addMouseMotionListener(canvasMouseAdapter);
        addMouseWheelListener(canvasMouseAdapter); // Redraw the JPanel with the updated zoom level
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
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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

        zoomFactor = 1.0f;

        notifyDrawingColorChanged();
        notifyFillColorChanged();
        clearHistory();
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Graphics2D getCanvasGraphics() {
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.scale(zoomFactor, zoomFactor);
        return g2d;
    }

    public void setTempCanvas(BufferedImage tempCanvas) {
        this.tempCanvas = tempCanvas;
    }

    public BufferedImage getTempCanvas() {
        if (tempCanvas == null ) {
            tempCanvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        }
        return tempCanvas;
    }

    public void saveToFile(File file) throws IOException {
        fileManager.saveToFile(file, image);
    }

    public void loadFromFile(File file) throws IOException {
        zoomFactor = 1.0f;
        BufferedImage loadedImage = fileManager.loadFromFile(file);

        Color currentColor = graphics != null ? graphics.getColor() : Color.BLACK;
        Stroke currentStroke = graphics != null ? graphics.getStroke() : new BasicStroke(1);

        image = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        graphics = (Graphics2D) image.getGraphics();

        graphics.setColor(currentColor);
        graphics.setStroke(currentStroke);
        graphics.drawImage(loadedImage, 0, 0, null);

        setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        revalidate();
        repaint();
        clearHistory();
    }

    public String getCurrentFilePath() {
        return fileManager.getCurrentFilePath();
    }

    public void resetCurrentFilePath() {
        fileManager.setCurrentFilePath(null);
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

    public Color getFillColor() {
        return fillColor;
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

    public float getZoomFactor() {
        return zoomFactor;
    }

    public void addToolChangeListener(ToolChangeListener listener) {
        toolChangeListeners.add(listener);
    }

    public void removeToolChangeListener(ToolChangeListener listener) {
        toolChangeListeners.remove(listener);
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public DrawingTool getActiveTool() {
        return tools.get(currentTool);
    }

    public DrawingTool getTool(Tool tool) {
        return tools.get(tool);
    }

    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
        getActiveTool().setCursor();
        // Notify all listeners
        for (ToolChangeListener listener : toolChangeListeners) {
            listener.onToolChanged(tool);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(zoomFactor, zoomFactor);

        // Clear the canvas with the background color
        g2d.setColor(canvasBackground);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw the permanent canvas
        if (image != null) {
            g2d.drawImage(image, 0, 0, null);
        }

        // Draw the temporary canvas on top
        if (tempCanvas != null) {
            g2d.drawImage(tempCanvas, 0, 0, null);
        }

        SelectionRenderer selectionRenderer = (SelectionRenderer) getTool(Tool.RECTANGLE_SELECTION);
        Selection selection = selectionManager.getSelection();
        if (currentTool == Tool.RECTANGLE_SELECTION && selection != null) {
            selectionRenderer.drawSelectionContent(g2d);
        }

        // Reset scale for grid drawing
        g2d.scale(1 / zoomFactor, 1 / zoomFactor);
        renderZoomGrid(g2d);
        selectionRenderer.drawSelectionOutline(g2d);
        g2d.dispose();
    }

    private void renderZoomGrid(Graphics2D g2d) {
        if (zoomFactor <= 5.0f) {
            return;
        }
        int scaledWidth = (int) (image.getWidth(null) * zoomFactor);
        int scaledHeight = (int) (image.getHeight(null) * zoomFactor);
        g2d.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x <= scaledWidth; x += (int) zoomFactor) {
            g2d.drawLine(x, 0, x, scaledHeight);
        }
        for (int y = 0; y <= scaledHeight; y += (int) zoomFactor) {
            g2d.drawLine(0, y, scaledWidth, y);
        }
    }

    private void initTools() {
        tools.put(Tool.LINE, new LineTool(this));
        tools.put(Tool.RECTANGLE, new RectangleTool(this));
        tools.put(Tool.CIRCLE, new CircleTool(this));
        tools.put(Tool.FILL, new FillTool(this));
        tools.put(Tool.EYEDROPPER, new EyedropperTool(this));
        tools.put(Tool.PENCIL, new PencilTool(this));
        tools.put(Tool.BRUSH, new BrushTool(this));
        tools.put(Tool.RECTANGLE_SELECTION, new RectangleSelectionTool(this));
        tools.put(Tool.TEXT, new TextTool(this));
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
        historyManager.saveToUndoStack(image);
    }

    public void undo() {
        image = historyManager.undo(image);
        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.clearOutline();
        }
        repaint();
    }

    public void redo() {
        image = historyManager.redo(image);
        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.clearOutline();
        }
        repaint();
    }

    public boolean canUndo() {
        return historyManager.canUndo();
    }

    public boolean canRedo() {
        return historyManager.canRedo();
    }

    public void clearHistory() {
        historyManager.clearHistory();
    }

    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        historyManager.addUndoRedoChangeListener(listener);
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

    private void updateCanvasAfterZoom() {
        // Calculate new dimensions
        int scaledWidth = (int) (image.getWidth(null) * zoomFactor);
        int scaledHeight = (int) (image.getHeight(null) * zoomFactor);
        setPreferredSize(new Dimension(scaledWidth, scaledHeight));
    }

    /**
     * MouseAdapter for handling mouse events on the canvas.
     * Relies on the current tool to handle events.
     */
    private class CanvasMouseAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
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

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            // Apply zoom
            float[] zoomLevels = {1.0f, 2.0f, 4.0f, 8.0f, 12.0f};

            if (e.getWheelRotation() < 0) {
                for (float zoomLevel : zoomLevels) {
                    if (zoomFactor < zoomLevel) {
                        zoomFactor = zoomLevel;
                        break;
                    }
                }
            } else {
                for (int i = zoomLevels.length - 1; i >= 0; i--) {
                    if (zoomFactor > zoomLevels[i]) {
                        zoomFactor = zoomLevels[i];
                        break;
                    }
                }
            }

            updateCanvasAfterZoom();
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.mouseScrolled(e);
            }
            revalidate();
            repaint();
        }
    }
}