package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.*;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
import com.esgdev.sparkpaint.io.ClipboardChangeListener;
import com.esgdev.sparkpaint.io.ClipboardManager;
import com.esgdev.sparkpaint.io.FileManager;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
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
        ELLIPSE,
        SELECTION,
        FILL,
        EYEDROPPER
    }
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;
    public static final int MAX_LINE_THICKNESS = 20;
    public static final float MAX_ZOOM_FACTOR = 12.0f;
    public static final float MIN_ZOOM_FACTOR = 1.0f;

    private Image image;
    private Graphics2D graphics;
    private BufferedImage tempCanvas; // Temporary canvas for drawing previews
    private BufferedImage selectionContent;
    private Rectangle selectionRectangle;

    private Tool currentTool = Tool.PENCIL;
    private Color drawingColor = Color.BLACK;
    private Color fillColor = Color.WHITE; //
    private Color canvasBackground = Color.WHITE;
    private float lineThickness = 2.0f;
    private float zoomFactor = 1.0f;
    private BufferedImage zoomGridCache;
    private float cachedZoomFactor = -1.0f;
    private Map<Float, BufferedImage> zoomGridCacheMap = new HashMap<>();

    private final List<ToolChangeListener> toolChangeListeners = new ArrayList<>();
    private final List<CanvasPropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    private final EnumMap<Tool, DrawingTool> tools = new EnumMap<>(Tool.class);
    private final HistoryManager historyManager;
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
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.scale(zoomFactor, zoomFactor);
        return g2d;
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
        fileManager.saveToFile(file, image);
    }

    public void loadFromFile(File file) throws IOException {
        BufferedImage loadedImage = fileManager.loadFromFile(file);

        Color currentColor = graphics != null ? graphics.getColor() : Color.BLACK;
        Stroke currentStroke = graphics != null ? graphics.getStroke() : new BasicStroke(1);

        image = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        graphics = (Graphics2D) image.getGraphics();

        graphics.setColor(currentColor);
        graphics.setStroke(currentStroke);
        graphics.drawImage(loadedImage, 0, 0, null);

        setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        repaint();
        clearHistory();
    }

    public String getCurrentFilePath() {
        return fileManager.getCurrentFilePath();
    }

    public void resetCurrentFilePath() {
        fileManager.setCurrentFilePath(null);
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

    // Getter for the current tool
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

        // Reset scale for grid drawing
        g2d.scale(1 / zoomFactor, 1 / zoomFactor);

        // Draw grid when zoom factor is greater than 5
        renderZoomGrid(g2d);

        // Restore scale for selection tool
        g2d.scale(zoomFactor, zoomFactor);

        if (currentTool != Tool.SELECTION || selectionRectangle == null)
            return;

        DrawingTool tool = tools.get(currentTool);
        if (tool instanceof SelectionTool) {
            ((SelectionTool) tool).drawSelection(g2d);
        }
    }

    private void renderZoomGrid(Graphics2D g2d) {
        if (zoomFactor <= 5.0f) {
            return;
        }

        BufferedImage cachedGrid = zoomGridCacheMap.get(zoomFactor);
        if (cachedGrid == null) {
            int scaledWidth = (int) (image.getWidth(null) * zoomFactor);
            int scaledHeight = (int) (image.getHeight(null) * zoomFactor);
            cachedGrid = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gridGraphics = cachedGrid.createGraphics();

            gridGraphics.setColor(new Color(200, 200, 200, 100));
            for (int x = 0; x <= scaledWidth; x += (int) zoomFactor) {
                gridGraphics.drawLine(x, 0, x, scaledHeight);
            }
            for (int y = 0; y <= scaledHeight; y += (int) zoomFactor) {
                gridGraphics.drawLine(0, y, scaledWidth, y);
            }
            gridGraphics.dispose();
            zoomGridCacheMap.put(zoomFactor, cachedGrid);
        }

        g2d.drawImage(cachedGrid, 0, 0, null);
    }

    private void initTools() {
        tools.put(Tool.LINE, new LineTool(this));
        tools.put(Tool.RECTANGLE, new RectangleTool(this));
        tools.put(Tool.CIRCLE, new CircleTool(this));
        tools.put(Tool.ELLIPSE, new EllipseTool(this));
        tools.put(Tool.FILL, new FillTool(this));
        tools.put(Tool.EYEDROPPER, new EyedropperTool(this));
        tools.put(Tool.PENCIL, new PencilTool(this));
        tools.put(Tool.BRUSH, new BrushTool(this));
        tools.put(Tool.SELECTION, new SelectionTool(this));
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

    /**
     * MouseAdapter for handling mouse events on the canvas.
     * Relies on the current tool to handle events.
     */
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

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            Point viewPosition = SwingUtilities.getAncestorOfClass(JScrollPane.class, DrawingCanvas.this)
                    .getComponent(0).getLocation();
            Point cursorPoint = e.getPoint();

            // Store the position relative to the image before zooming
            double relativeX = cursorPoint.x / (image.getWidth(null) * zoomFactor);
            double relativeY = cursorPoint.y / (image.getHeight(null) * zoomFactor);


            // Define valid zoom factors
            float[] zoomLevels = {1.0f, 2.0f, 4.0f, 8.0f, 12.0f};

            if (e.getWheelRotation() < 0) { // Zoom in
                for (float zoomLevel : zoomLevels) {
                    if (zoomFactor < zoomLevel) {
                        zoomFactor = zoomLevel;
                        break;
                    }
                }
            } else { // Zoom out
                for (int i = zoomLevels.length - 1; i >= 0; i--) {
                    if (zoomFactor > zoomLevels[i]) {
                        zoomFactor = zoomLevels[i];
                        break;
                    }
                }
            }

            // Update preferred size based on zoom factor
            int scaledWidth = (int) (image.getWidth(null) * zoomFactor);
            int scaledHeight = (int) (image.getHeight(null) * zoomFactor);
            setPreferredSize(new Dimension(scaledWidth, scaledHeight));

            // Calculate new scroll position to keep the cursor point fixed
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, DrawingCanvas.this);
            if (scrollPane != null) {
                JViewport viewport = scrollPane.getViewport();
                Point newViewPosition = new Point();

                // Calculate the new view position based on cursor position
                newViewPosition.x = (int) (relativeX * scaledWidth - cursorPoint.x);
                newViewPosition.y = (int) (relativeY * scaledHeight - cursorPoint.y);

                // Ensure the new position is within bounds
                newViewPosition.x = Math.max(0, Math.min(newViewPosition.x, scaledWidth - viewport.getWidth()));
                newViewPosition.y = Math.max(0, Math.min(newViewPosition.y, scaledHeight - viewport.getHeight()));

                viewport.setViewPosition(newViewPosition);
            }

            revalidate();
            repaint();

            DrawingTool tool = tools.get(currentTool);
            if (tool != null) {
                tool.mouseScrolled(e);
            }
        }
    }
}