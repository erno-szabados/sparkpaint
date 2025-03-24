package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.history.HistoryManagement;
import com.esgdev.sparkpaint.engine.history.HistoryManager;
import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.history.UndoRedoChangeListener;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.layer.LayerChangeListener;
import com.esgdev.sparkpaint.engine.layer.LayerManagement;
import com.esgdev.sparkpaint.engine.layer.LayerManager;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManagement;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;
import com.esgdev.sparkpaint.engine.tools.*;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
import com.esgdev.sparkpaint.io.*;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * DrawingCanvas is a class that represents a drawing canvas where users can draw shapes, lines, and other graphics.
 * Supports various drawing tools and allows for undo/redo functionality, clipboard operations, and file management.
 */
public class DrawingCanvas extends JPanel implements
        HistoryManagement,
        LayerManagement,
        SelectionManagement,
        ClipboardManagement,
        FileManagement {


    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;
    public static final int MAX_LINE_THICKNESS = 20;

    private Color drawingColor = Color.BLACK;
    private Color fillColor = Color.WHITE;
    private Color canvasBackground;
    private float lineThickness = 2.0f;
    private float zoomFactor = 1.0f;

    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    private HistoryManagement historyManager;
    private SelectionManagement selectionManager;
    private ClipboardManagement clipboardManager;
    private LayerManagement layerManager;
    private FileManagement fileManager;
    private ToolManagement toolManager;
    private Point cursorShapeCenter = new Point(0, 0);
    private int cursorSize = 0;
    private BrushTool.BrushShape cursorShape;

    /**
     * Creates a fully configured DrawingCanvas with all required dependencies.
     *
     * @return The configured DrawingCanvas
     */
    public static DrawingCanvas create() {
        // Create managers first without canvas reference
        HistoryManagement historyManager = new HistoryManager();
        FileManager fileManager = new FileManager();

        // Create the canvas with minimal dependencies
        DrawingCanvas canvas = new DrawingCanvas();

        // Create managers that need canvas reference
        LayerManager layerManager = new LayerManager(canvas);
        SelectionManager selectionManager = new SelectionManager(canvas);
        ClipboardManager clipboardManager = new ClipboardManager(canvas);
        ToolManager toolManager = new ToolManager(canvas);
        CanvasMouseAdapter mouseAdapter = new CanvasMouseAdapter(canvas);

        // Initialize the canvas with all dependencies
        canvas.initialize(historyManager, selectionManager, clipboardManager,
                layerManager, fileManager, toolManager, mouseAdapter);

        return canvas;
    }

    /**
     * Creates a DrawingCanvas for testing purposes with all required dependencies.
     *
     * @param historyManager    The HistoryManagement instance
     * @param selectionManager  The SelectionManagement instance
     * @param clipboardManager  The ClipboardManagement instance
     * @param layerManager      The LayerManagement instance
     * @param fileManager       The FileManagement instance
     * @param toolManager       The ToolManagement instance
     * @param mouseAdapter      The CanvasMouseAdapter instance
     * @return The configured DrawingCanvas for testing
     */
    static DrawingCanvas createForTesting(
        HistoryManagement historyManager,
        SelectionManagement selectionManager,
        ClipboardManagement clipboardManager,
        LayerManagement layerManager,
        FileManagement fileManager,
        ToolManagement toolManager,
        CanvasMouseAdapter mouseAdapter) {

        DrawingCanvas canvas = new DrawingCanvas();
        canvas.initialize(historyManager, selectionManager, clipboardManager,
                        layerManager, fileManager, toolManager, mouseAdapter);
        return canvas;
    }

    /**
     * Default constructor for DrawingCanvas.
     * Initializes the canvas with a white background.
     * To create a fully configured canvas, use the static method createDrawingCanvas().
     */
    private DrawingCanvas() {
        // Minimal initialization, enough to create a valid JPanel
        setBackground(Color.WHITE);
    }

    // Add an initialize method
    private void initialize(HistoryManagement historyManager,
                           SelectionManagement selectionManager,
                           ClipboardManagement clipboardManager,
                           LayerManagement layerManager,
                           FileManagement fileManager,
                           ToolManagement toolManager,
                           CanvasMouseAdapter mouseAdapter) {

        this.historyManager = historyManager;
        this.selectionManager = selectionManager;
        this.clipboardManager = clipboardManager;
        this.layerManager = layerManager;
        this.fileManager = fileManager;
        this.toolManager = toolManager;

        this.canvasBackground = Color.WHITE;
        setPreferredSize(new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
        initMouseHandlers(mouseAdapter);
        toolManager.initTools();
        createNewCanvas(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, canvasBackground);
    }

    /**
     * Creates a new canvas with the specified dimensions and background color.
     *
     * @param width            width of the canvas
     * @param height           height of the canvas
     * @param canvasBackground background color of the canvas
     */
    // Update createNewCanvas in DrawingCanvas class
    public void createNewCanvas(int width, int height, Color canvasBackground) {
        this.canvasBackground = canvasBackground;
        this.drawingColor = Color.BLACK;
        this.fillColor = Color.WHITE;

        // Initialize layers
        layerManager.initializeLayers(width, height);

        // Fill the base layer with background color
        BufferedImage baseLayer = layerManager.getLayers().get(0).getImage();
        Graphics2D baseGraphics = baseLayer.createGraphics();
        baseGraphics.setColor(canvasBackground);
        baseGraphics.fillRect(0, 0, width, height);
        baseGraphics.dispose();

        notifyBackgroundColorChanged();


        notifyDrawingColorChanged();
        notifyFillColorChanged();
        zoomFactor = 1.0f;
        clearHistory();
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    public void setToolCanvas(BufferedImage toolCanvas) {
        toolManager.setToolCanvas(toolCanvas);
    }

    public BufferedImage getToolCanvas() {
        return toolManager.getToolCanvas();
    }

    public void setDrawingColor(Color color) {
        if (color != null && !color.equals(this.drawingColor)) {
            this.drawingColor = color;
            notifyDrawingColorChanged();
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
    }


    public void setCursorSize(int size) {
        cursorSize = size;
        repaint();
    }

    public void setCursorShape(BrushTool.BrushShape cursorShape) {
        this.cursorShape = cursorShape;
        repaint();
    }

    public float getLineThickness() {
        return lineThickness;
    }

    public float getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(float zoomLevel) {
        this.zoomFactor = zoomLevel;
    }

    public void addToolChangeListener(ToolChangeListener listener) {
        toolManager.addToolChangeListener(listener);
    }

    public ToolManager.Tool getCurrentTool() {
        return toolManager.getCurrentTool();
    }

    public DrawingTool getActiveTool() {
        return toolManager.getActiveTool();
    }

    public DrawingTool getTool(ToolManager.Tool tool) {
        return toolManager.getTool(tool);
    }

    public void setCurrentTool(ToolManager.Tool tool) {
        // Notify all listeners
        toolManager.setCurrentTool(tool);
    }

    public void setCursorShapeCenter(Point p) {
        this.cursorShapeCenter = p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(zoomFactor, zoomFactor);

        // Draw the transparency checkerboard if enabled
        if (layerManager.isTransparencyVisualizationEnabled()) {
            g2d.drawImage(layerManager.getTransparencyBackground(), 0, 0, null);
        } else {
            // Clear the canvas with the background color
            g2d.setColor(canvasBackground);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // Draw all visible layers from bottom to top
        List<Layer> layers = layerManager.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }

        // Rest of the painting code...
        if (toolManager.getToolCanvas() != null) {
            g2d.drawImage(toolManager.getToolCanvas(), 0, 0, null);
        }

        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.drawSelectionContent(g2d);
        }

        // Reset scale for grid drawing
        g2d.scale(1 / zoomFactor, 1 / zoomFactor);
        renderZoomGrid(g2d);
        if (selection != null) {
            selection.drawSelectionOutline(g2d, zoomFactor);
        }

        // Draw the brush cursor
        if (toolManager.isShowBrushCursor()) {
            drawCursorShape(g2d);
        }

        g2d.dispose();
    }

    private void initMouseHandlers(CanvasMouseAdapter canvasMouseAdapter) {
        addMouseListener(canvasMouseAdapter);
        addMouseMotionListener(canvasMouseAdapter);
        addMouseWheelListener(canvasMouseAdapter);
    }

    private void renderZoomGrid(Graphics2D g2d) {
        if (zoomFactor <= 5.0f) {
            return;
        }
        // Use the dimensions of the current layer instead of image
        BufferedImage currentLayer = layerManager.getCurrentLayerImage();
        int scaledWidth = (int) (currentLayer.getWidth() * zoomFactor);
        int scaledHeight = (int) (currentLayer.getHeight() * zoomFactor);
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
                int x = (int) (circleCenter.x - (cursorSize / 2.0) * zoomFactor);
                int y = (int) (circleCenter.y - (cursorSize / 2.0) * zoomFactor);
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

    public void addCanvasPropertyChangeListener(CanvasPropertyChangeListener listener) {
        propertyChangeListeners.add(listener);
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

    void updateCanvasAfterZoom() {
        // Calculate new dimensions using current layer instead of image
        BufferedImage currentLayer = layerManager.getCurrentLayerImage();
        int scaledWidth = (int) (currentLayer.getWidth() * zoomFactor);
        int scaledHeight = (int) (currentLayer.getHeight() * zoomFactor);
        setPreferredSize(new Dimension(scaledWidth, scaledHeight));
    }

    /// LayerManagement interface

    @Override
    public void addNewLayer() {
        layerManager.addNewLayer();
    }

    @Override
    public boolean duplicateCurrentLayer() {
        return layerManager.duplicateCurrentLayer();
    }

    @Override
    public void deleteCurrentLayer() {
        layerManager.deleteCurrentLayer();
    }

    @Override
    public void deleteLayer(int index) {
        layerManager.deleteLayer(index);
    }

    @Override
    public boolean moveLayer(int fromIndex, int toIndex) {
        return layerManager.moveLayer(fromIndex, toIndex);
    }

    @Override
    public List<Layer> getLayers() {
        return layerManager.getLayers();
    }

    @Override
    public void setLayers(List<Layer> layers) {
        layerManager.setLayers(layers);
    }

    @Override
    public int getCurrentLayerIndex() {
        return layerManager.getCurrentLayerIndex();
    }

    @Override
    public void setCurrentLayerIndex(int index) {
        layerManager.setCurrentLayerIndex(index);
    }

    @Override
    public BufferedImage getCurrentLayerImage() {
        return layerManager.getCurrentLayerImage();
    }

    @Override
    public int getLayerCount() {
        return layerManager.getLayerCount();
    }

    @Override
    public boolean mergeCurrentLayerDown() {
        return layerManager.mergeCurrentLayerDown();
    }

    @Override
    public boolean flattenLayers() {
        return layerManager.flattenLayers();
    }

    @Override
    public boolean isTransparencyVisualizationEnabled() {
        return layerManager.isTransparencyVisualizationEnabled();
    }

    @Override
    public void setTransparencyVisualizationEnabled(boolean enabled) {
        layerManager.setTransparencyVisualizationEnabled(enabled);
    }

    @Override
    public void initializeLayers(int width, int height) {
        layerManager.initializeLayers(width, height);
    }

    @Override
    public BufferedImage getTransparencyBackground() {
        return layerManager.getTransparencyBackground();
    }

    // SelectionManagement interface

    @Override
    public Selection getSelection() {
        return selectionManager.getSelection();
    }

    @Override
    public void setSelection(Selection selection) {
        selectionManager.setSelection(selection);
    }

    @Override
    public void clearSelection() {
        selectionManager.clearSelection();
    }

    @Override
    public void selectAll() {
        selectionManager.selectAll();
    }

    @Override
    public void deleteSelection() {
        selectionManager.deleteSelection();
    }

    @Override
    public void rotateSelection(int degrees) {
        selectionManager.rotateSelection(degrees);
    }

    @Override
    public void flipSelection(boolean horizontal) {
        selectionManager.flipSelection(horizontal);
    }

    @Override
    public boolean isWithinSelection(Point worldPoint) {
        return selectionManager.isWithinSelection(worldPoint);
    }

    @Override
    public Graphics2D getDrawingGraphics() {
        return selectionManager.getDrawingGraphics();
    }

    @Override
    public Point getDrawingCoordinates(Point screenPoint, float zoomFactor) {
        return selectionManager.getDrawingCoordinates(screenPoint, zoomFactor);
    }

    /// FileManagement interface

    @Override
    public void saveToFile(File file) throws IOException {
        ((FileManager)fileManager).saveToFile(file, layerManager.getLayers());
    }

    @Override
    public LayerState loadFromFile(File file) throws IOException {
        zoomFactor = 1.0f;

        // Load layers and active layer index from file
        LayerState layerState = fileManager.loadFromFile(file);

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
        return layerState;
    }

    @Override
    public String getCurrentFilePath() {
        return fileManager.getCurrentFilePath();
    }

    @Override
    public void resetCurrentFilePath() {
        fileManager.setCurrentFilePath(null);
    }

    @Override
    public void setCurrentFilePath(String path) {
        fileManager.setCurrentFilePath(path);
    }

    ///  HistoryManagement interface

    @Override
    public void saveToUndoStack() {
        // Get layers and current layer index from the layerManager
        List<Layer> layers = layerManager.getLayers();
        int currentLayerIndex = layerManager.getCurrentLayerIndex();

        // Pass these to the historyManager's detailed method
        ((HistoryManager)historyManager).saveToUndoStack(layers, currentLayerIndex);
    }

    @Override
    public LayerState undo() {
        LayerState state = ((HistoryManager)historyManager).undo(layerManager.getLayers(), layerManager.getCurrentLayerIndex());
        layerManager.setLayers(state.getLayers());
        layerManager.setCurrentLayerIndex(state.getCurrentLayerIndex());

        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.clearOutline();
        }
        repaint();
        return state;
    }

    @Override
    public LayerState redo() {
        LayerState state = ((HistoryManager)historyManager).redo(layerManager.getLayers(), layerManager.getCurrentLayerIndex());
        layerManager.setLayers(state.getLayers());
        layerManager.setCurrentLayerIndex(state.getCurrentLayerIndex());

        Selection selection = selectionManager.getSelection();
        if (selection != null) {
            selection.clearOutline();
        }
        repaint();
        return state;
    }

    @Override
    public boolean canUndo() {
        return historyManager.canUndo();
    }

    @Override
    public boolean canRedo() {
        return historyManager.canRedo();
    }

    @Override
    public void clearHistory() {
        historyManager.clearHistory();
    }

    @Override
    public void addUndoRedoChangeListener(UndoRedoChangeListener listener) {
        historyManager.addUndoRedoChangeListener(listener);
    }

    /// ClipboardManagement interface

    @Override
    public void cutSelection() {
        clipboardManager.cutSelection();
    }

    @Override
    public void copySelection() {
        clipboardManager.copySelection();
    }

    @Override
    public void pasteSelection() throws IOException, UnsupportedFlavorException {
        clipboardManager.pasteSelection();
    }

    @Override
    public void eraseSelection() {
        clipboardManager.eraseSelection();
    }

    @Override
    public boolean hasSelection() {
        return clipboardManager.hasSelection();
    }

    @Override
    public boolean canPaste() {
        return clipboardManager.canPaste();
    }

    @Override
    public void addClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardManager.addClipboardChangeListener(listener);
    }

    @Override
    public void removeClipboardChangeListener(ClipboardChangeListener listener) {
        clipboardManager.removeClipboardChangeListener(listener);
    }

    @Override
    public void notifyClipboardStateChanged() {
        clipboardManager.notifyClipboardStateChanged();
    }

    @Override
    public void addLayerChangeListener(LayerChangeListener listener) {
layerManager.addLayerChangeListener(listener);
    }

    @Override
    public void removeLayerChangeListener(LayerChangeListener listener) {
layerManager.removeLayerChangeListener(listener);
    }

    @Override
    public void notifyLayersChanged() {
layerManager.notifyLayersChanged();
    }
}