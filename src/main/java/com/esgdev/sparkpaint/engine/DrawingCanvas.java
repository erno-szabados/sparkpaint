package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;
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
    private final LayerManager layerManager;
    private final FileManager fileManager;
    private boolean showBrushCursor = false;
    private Point cursorShapeCenter = new Point(0, 0);
    private int cursorSize = 0;
    private BrushTool.BrushShape cursorShape;

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
        layerManager = new LayerManager(this);
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
    // Update createNewCanvas in DrawingCanvas class
    public void createNewCanvas(int width, int height, Color canvasBackground) {
        this.canvasBackground = canvasBackground;
        this.drawingColor = Color.BLACK;
        this.fillColor = Color.WHITE;

        // Initialize layers
        layerManager.initializeLayers(width, height);

        // Create new buffered image for the main canvas
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

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

    public LayerManager getLayerManager() {
        return layerManager;
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
            tempCanvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
        return tempCanvas;
    }

   public void saveToFile(File file) throws IOException {
       fileManager.saveToFile(file, layerManager.getLayers(), layerManager.getCurrentLayerIndex());
   }

   public void loadFromFile(File file) throws IOException {
       zoomFactor = 1.0f;

       // Load layers and active layer index from file
       HistoryManager.LayerState layerState = fileManager.loadFromFile(file);

       // Apply the loaded layers to the layer manager
       layerManager.setLayers(layerState.getLayers());
       layerManager.setCurrentLayerIndex(layerState.getCurrentLayerIndex());

       // Get dimensions from first layer
       BufferedImage firstLayerImage = layerState.getLayers().get(0).getImage();

       // Update canvas size
       setPreferredSize(new Dimension(firstLayerImage.getWidth(), firstLayerImage.getHeight()));
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


    public void setCursorSize(int size) {
        cursorSize = size;
        repaint();
    }

    public int getCursorSize() {
        return cursorSize;
    }

    public void setCursorShape(BrushTool.BrushShape cursorShape) {
        this.cursorShape = cursorShape;
        repaint();
    }

    public BrushTool.BrushShape getCursorShape() {
        return cursorShape;
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
        showBrushCursor = tool == Tool.BRUSH;
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

        // Draw all visible layers from bottom to top
        List<Layer> layers = layerManager.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }

        // Draw the temporary canvas on top
        if (tempCanvas != null) {
            g2d.drawImage(tempCanvas, 0, 0, null);
        }

        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.drawSelectionContent(g2d, zoomFactor);
        }

        // Reset scale for grid drawing
        g2d.scale(1 / zoomFactor, 1 / zoomFactor);
        renderZoomGrid(g2d);
        if (selection != null) {
            selection.drawSelectionOutline(g2d, zoomFactor);
        }

        // Draw the brush cursor
        if (showBrushCursor) {
            drawCursorShape(g2d);
        }

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

    private void drawCursorShape(Graphics2D g2d) {
        if (cursorShape == null) return;
        g2d.setColor(Color.BLACK);
        float[] dottedPattern = {3, 3};
        BasicStroke dottedStroke = new BasicStroke(
                1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, dottedPattern, 0);
        g2d.setStroke(dottedStroke);
        switch (cursorShape) {
            case SPRAY:
            case CIRCLE:
                Point circleCenter = cursorShapeCenter;
                int x = (int) (circleCenter.x - (cursorSize / 2.0 ) * zoomFactor);
                int y = (int) (circleCenter.y - (cursorSize / 2.0 ) * zoomFactor);
                int diameter = (int) (cursorSize * zoomFactor);
                g2d.drawOval(x, y, diameter, diameter);
                break;
            case SQUARE:
                Point squareCenter = cursorShapeCenter;
                int squareX = (int) (squareCenter.x - (cursorSize / 2.0) * zoomFactor);
                int squareY = (int) (squareCenter.y - (cursorSize / 2.0) * zoomFactor);
                int squareSide = (int) (cursorSize * zoomFactor);
                g2d.drawRect(squareX, squareY, squareSide, squareSide);
                break;
            default:
                // unsupported
                break;
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
        tools.put(Tool.FREEHAND_SELECTION, new FreeHandSelectionTool(this));
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
        historyManager.saveToUndoStack(layerManager.getLayers(), layerManager.getCurrentLayerIndex());
    }

    public void undo() {
        HistoryManager.LayerState state = historyManager.undo(layerManager.getLayers(), layerManager.getCurrentLayerIndex());
        layerManager.setLayers(state.getLayers());
        layerManager.setCurrentLayerIndex(state.getCurrentLayerIndex());

        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.clearOutline();
        }
        repaint();
    }

    public void redo() {
        HistoryManager.LayerState state = historyManager.redo(layerManager.getLayers(), layerManager.getCurrentLayerIndex());
        layerManager.setLayers(state.getLayers());
        layerManager.setCurrentLayerIndex(state.getCurrentLayerIndex());

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
            //cursorCircleCenter = DrawingTool.screenToWorld(zoomFactor,  e.getPoint());
            cursorShapeCenter = e.getPoint();
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.mouseDragged(e);
            }

            //cursorCircleCenter = DrawingTool.screenToWorld(zoomFactor,  e.getPoint());
            cursorShapeCenter = e.getPoint();
            repaint();
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
            //cursorCircleCenter = DrawingTool.screenToWorld(zoomFactor,  e.getPoint());
            cursorShapeCenter = e.getPoint();
            revalidate();
            repaint();
        }
    }
}