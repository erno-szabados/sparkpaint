package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.PathSelection;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Random;

public class BrushTool implements DrawingTool {
    public enum BrushShape {
        SQUARE,
        CIRCLE,
        SPRAY
    }

    private final Random random = new Random();
    public static final int DEFAULT_SPRAY_DENSITY = 20;
    public static final int DEFAULT_SPRAY_SIZE = 5;
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private BrushShape shape = BrushShape.SPRAY;
    private int size = DEFAULT_SPRAY_SIZE;
    private int sprayDensity = DEFAULT_SPRAY_DENSITY;
    private boolean useAntiAliasing = true;
    private float maxBlendStrength = 0.1f;  // Range: 0.01f to 1.0f


    public BrushTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

  @Override
    public void mousePressed(MouseEvent e) {
        SelectionManager selectionManager = canvas.getSelectionManager();
        Selection selection = selectionManager.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Save last point and update canvas
        lastPoint = selectionManager.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        canvas.saveToUndoStack();

        // Get appropriate graphics context and draw
        Graphics2D g2d = selectionManager.getDrawingGraphics(canvas);
        drawShape(e, lastPoint, g2d);
        g2d.dispose();

        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        SelectionManager selectionManager = canvas.getSelectionManager();
        Selection selection = selectionManager.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if dragging inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Get current point and update
        Point currentPoint = selectionManager.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context and draw
        Graphics2D g2d = selectionManager.getDrawingGraphics(canvas);
        drawShape(e, currentPoint, g2d);
        g2d.dispose();

        lastPoint = currentPoint;
        canvas.repaint();
    }

    // Updated drawShape method that accepts a Graphics2D parameter
    private void drawShape(MouseEvent e, Point p, Graphics2D g2d) {
        Color paintColor;
        if (SwingUtilities.isLeftMouseButton(e)) {
            paintColor = canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            paintColor = canvas.getFillColor();
        } else {
            return;
        }

        // Get the target image for drawing
        BufferedImage targetImage;
        SelectionManager selectionManager = canvas.getSelectionManager();
        if (selectionManager.isWithinSelection(DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint()))) {
            targetImage = selectionManager.getSelection().getContent();
        } else {
            targetImage = canvas.getImage();
        }

        int x = p.x - size / 2;
        int y = p.y - size / 2;

        switch (shape) {
            case SQUARE:
                drawBlendedShape(targetImage, g2d, x, y, size, size, paintColor, true);
                break;
            case CIRCLE:
                drawBlendedShape(targetImage, g2d, x, y, size, size, paintColor, false);
                break;
            case SPRAY:
                sprayPaint(e, targetImage, g2d, p);
                break;
        }
    }

    // Updated methods to accept external Graphics2D
    private void drawBlendedShape(BufferedImage image, Graphics2D g2d, int x, int y, int width, int height, Color paintColor, boolean isSquare) {
        g2d.setColor(paintColor);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Create a temporary image for the shape
        BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempG2d = tempImage.createGraphics();
        tempG2d.setColor(paintColor);
        tempG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw the shape to the temporary image
        if (isSquare) {
            tempG2d.fillRect(x, y, width, height);
        } else {
            tempG2d.fillOval(x, y, width, height);
        }
        tempG2d.dispose();

        // Apply blending to the main image
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(image.getWidth(), x + width);
        int endY = Math.min(image.getHeight(), y + height);

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int tempRGB = tempImage.getRGB(i, j);
                // Only process non-transparent pixels from the shape
                if ((tempRGB & 0xFF000000) != 0) {
                    // Check if this pixel is within the clipping region
                    if (g2d.getClip() == null || g2d.getClip().contains(i, j)) {
                        int currentRGB = image.getRGB(i, j);
                        Color blendedColor = getBlendedColor(currentRGB, maxBlendStrength, paintColor);
                        image.setRGB(i, j, blendedColor.getRGB());
                    }
                }
            }
        }
    }

    private Color getBlendedColor(int currentRGB, float maxBlendStrength, Color paintColor) {
        Color currentColor = new Color(currentRGB, true);

        // Apply blending with a consistent strength
        int r = (int) ((1 - maxBlendStrength) * currentColor.getRed() + maxBlendStrength * paintColor.getRed());
        int g = (int) ((1 - maxBlendStrength) * currentColor.getGreen() + maxBlendStrength * paintColor.getGreen());
        int b = (int) ((1 - maxBlendStrength) * currentColor.getBlue() + maxBlendStrength * paintColor.getBlue());

        return new Color(r, g, b);
    }

    public void setMaxBlendStrength(float strength) {
        this.maxBlendStrength = Math.max(0.01f, Math.min(1.0f, strength));
    }

    public float getMaxBlendStrength() {
        return maxBlendStrength;
    }

    public void setShape(BrushShape shape) {
        this.shape = shape;
        canvas.setCursorShape(shape);
    }

    public void setSize(int size) {
        this.size = size;
        canvas.setCursorSize(size);
    }

    public void setSprayDensity(int sprayDensity) {
        this.sprayDensity = sprayDensity;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // No action needed for mouse scroll
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        switch (shape) {
            case SQUARE: return "Brush tool: Square mode";
            case CIRCLE: return  "Brush tool: Circle mode";
            case SPRAY: return "Brush tool: Spray mode";
            default:
                return "Brush tool: Unknown mode";
        }
    }

    // Add getter/setter for anti-aliasing
    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }

    private void sprayPaint(MouseEvent e, BufferedImage image, Graphics2D g2d, Point center) {
        Color paintColor;
        if (SwingUtilities.isLeftMouseButton(e)) {
            paintColor = canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            paintColor = canvas.getFillColor();
        } else return;

        int radius = size / 2;
        double area = Math.PI * radius * radius;
        int effectiveDensity = (int) (sprayDensity * (area / (Math.PI * DEFAULT_SPRAY_SIZE * DEFAULT_SPRAY_SIZE)));

        for (int i = 0; i < effectiveDensity; i++) {
            double x_offset = (random.nextDouble() * 2 - 1) * radius;
            double y_offset = (random.nextDouble() * 2 - 1) * radius;

            if (x_offset * x_offset + y_offset * y_offset > radius * radius) {
                continue;
            }

            int x = center.x + (int)x_offset;
            int y = center.y + (int)y_offset;

            if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                // Check if this point is within the clipping region
                if (g2d.getClip() == null || g2d.getClip().contains(x, y)) {
                    int currentRGB = image.getRGB(x, y);
                    Color blendedColor = getBlendedColor(currentRGB, random.nextFloat() * maxBlendStrength, paintColor);
                    image.setRGB(x, y, blendedColor.getRGB());
                }
            }
        }
    }
}