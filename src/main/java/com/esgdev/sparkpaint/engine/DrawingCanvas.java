package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.*;
import com.esgdev.sparkpaint.engine.tools.DrawingTool;
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
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private final List<UndoRedoChangeListener> undoRedoChangeListeners = new ArrayList<>();
    private final List<ClipboardChangeListener> clipboardChangeListeners = new ArrayList<>();
    private EnumMap<Tool, DrawingTool> tools = new EnumMap<>(Tool.class);

    private static final int MAX_HISTORY_SIZE = 16;
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 600;

    private Rectangle selectionRectangle;
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor CROSSHAIR_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);

    public DrawingCanvas() {
        setPreferredSize(new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT));
        this.canvasBackground = Color.WHITE;
        setBackground(canvasBackground);

        MouseAdapter canvasMouseAdapter = new CanvasMouseAdapter();
        addMouseListener(canvasMouseAdapter);
        addMouseMotionListener(canvasMouseAdapter);
        initTools();
        createNewCanvas(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, canvasBackground);
    }

    /**
     * Creates a new canvas with the specified dimensions and background color.
     * @param width
     * @param height
     * @param canvasBackground
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
        switch (tool) {
            case EYEDROPPER:
            case PENCIL:
            case LINE:
            case RECTANGLE_OUTLINE:
            case RECTANGLE_FILLED:
            case CIRCLE_OUTLINE:
            case CIRCLE_FILLED:
            case ELLIPSE_OUTLINE:
            case ELLIPSE_FILLED:
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

    public void saveToUndoStack() {
        if (image != null) {
            BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            undoStack.push(copy);
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast(); // Keep the stack size within the limit
            }
            notifyUndoRedoStateChanged();
        }
    }

    private void eraseSelection() {
        // Clear the selected region by filling it with the canvas background color.
        graphics.setColor(canvasBackground);
        graphics.fillRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width,
                selectionRectangle.height);
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
                selectionRectangle.height);
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
            Point mousePosition = getMousePosition();
            int pasteX = 0;
            int pasteY = 0;

            if (mousePosition != null && contains(mousePosition)) {
                pasteX = mousePosition.x;
                pasteY = mousePosition.y;
            }
            graphics.drawImage(pastedImage, pasteX, pasteY, null);

            selectionRectangle = new Rectangle(pasteX, pasteY, pastedImage.getWidth(), pastedImage.getHeight());
            selectionContent = pastedImage;

            repaint();
            notifyClipboardStateChanged();
        }
    }

    public boolean hasSelection() {
        return selectionRectangle != null;
    }

    public boolean canPaste() {
        try {
            // Includes a workaround for JDK-6606476 (Uncatchable exception printed to
            // console for certain clipboard contents)
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
            BufferedImage currentState = new BufferedImage(image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
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
            BufferedImage currentState = new BufferedImage(image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
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
    public void notifyClipboardStateChanged() {
        boolean canCopy = hasSelection();
        boolean canPaste = canPaste();

        for (ClipboardChangeListener listener : clipboardChangeListeners) {
            listener.clipboardStateChanged(canCopy, canPaste);
        }
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