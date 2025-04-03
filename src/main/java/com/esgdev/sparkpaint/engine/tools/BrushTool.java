package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * BrushTool is a drawing tool that allows users to paint on a canvas using various brush shapes.
 * It supports different brush shapes (square, circle, spray), size adjustments, and antialiasing.
 * The tool can also blend colors based on the specified blend strength.
 */
public class BrushTool implements DrawingTool {

    public enum BrushShape {
        SQUARE,
        CIRCLE,
        SPRAY
    }

    private final Random random = new Random();
    public static final int DEFAULT_BLEND_STRENGTH = 25;
    public static final int DEFAULT_SPRAY_DENSITY = 25;
    public static final int DEFAULT_SPRAY_SIZE = 25;
    public static final int SPRAY_REFERENCE_SIZE = 5;
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

    /**
     * Handles mouse press events to initiate drawing.
     * Converts the screen coordinates to world coordinates and checks if the click is within a selection.
     * If a selection exists, it only allows drawing within that selection.
     *
     * @param e The mouse event triggering the drawing.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Save last point and update canvas
        lastPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        canvas.saveToUndoStack();

        // Get appropriate graphics context and draw
        Graphics2D g2d;
        Point drawPoint = lastPoint;

        if (selection != null && selection.hasOutline()) {
            selection.setModified(true);
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            drawPoint = new Point(lastPoint.x - bounds.x, lastPoint.y - bounds.y);
        } else {
            // Draw on current layer
            g2d = canvas.getCurrentLayerImage().createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        drawShape(e, drawPoint, g2d);
        g2d.dispose();

        canvas.repaint();
    }

    /**
     * Handles mouse drag events to continue drawing.
     * Converts the screen coordinates to world coordinates and checks if the drag is within a selection.
     * If a selection exists, it only allows drawing within that selection.
     *
     * @param e The mouse event triggering the drawing.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if dragging inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Get current point and update
        Point currentPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context for drawing
        Graphics2D g2d;
        Point drawPoint = currentPoint;

        if (selection != null && selection.hasOutline()) {
            // Get drawing graphics from the selection manager
            g2d = canvas.getDrawingGraphics();

            // Get selection bounds to adjust coordinates
            Rectangle bounds = selection.getBounds();

            // Adjust coordinates relative to the selection bounds
            drawPoint = new Point(currentPoint.x - bounds.x, currentPoint.y - bounds.y);
        } else {
            // Draw on current layer
            g2d = canvas.getCurrentLayerImage().createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        drawShape(e, drawPoint, g2d);
        g2d.dispose();

        lastPoint = currentPoint;
        canvas.repaint();
    }

    /**
     * Draws the shape at the specified point using the provided Graphics2D context.
     *
     * @param e   The mouse event triggering the drawing.
     * @param p   The point where the shape should be drawn.
     * @param g2d The Graphics2D context for drawing.
     */
    private void drawShape(MouseEvent e, Point p, Graphics2D g2d) {
        Color paintColor;
        if (SwingUtilities.isLeftMouseButton(e)) {
            paintColor = canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            paintColor = canvas.getFillColor();
        } else {
            return;
        }

        // If using transparent color, use special handling
        if (paintColor.getAlpha() == 0) {
            handleTransparentPainting(e, p, g2d);
            return;
        }

        // Regular painting code for non-transparent colors
        BufferedImage targetImage;
        Selection selection = canvas.getSelection();
        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
        } else {
            targetImage = canvas.getCurrentLayerImage();
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


    /**
     * Draws a blended shape (square or circle) on the target image.
     *
     * @param image      The target image to draw on.
     * @param g2d        The Graphics2D context for drawing.
     * @param x          The x-coordinate of the shape's top-left corner.
     * @param y          The y-coordinate of the shape's top-left corner.
     * @param width      The width of the shape.
     * @param height     The height of the shape.
     * @param paintColor The color to use for painting.
     * @param isSquare   True if the shape is a square, false if it's a circle.
     */
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

    /**
     * Handles painting with transparency - this erases content by setting pixels to transparent
     */
    private void handleTransparentPainting(MouseEvent e, Point p, Graphics2D g2d) {
        BufferedImage targetImage;
        Selection selection = canvas.getSelection();
        if (selection != null && selection.hasOutline()) {
            targetImage = selection.getContent();
        } else {
            targetImage = canvas.getCurrentLayerImage();
        }

        int x = p.x - size / 2;
        int y = p.y - size / 2;

        // Create a mask image for the alpha feathering
        BufferedImage maskImage = new BufferedImage(targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();
        maskG2d.setColor(Color.WHITE);
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw the shape to the mask
        switch (shape) {
            case SQUARE:
                maskG2d.fillRect(x, y, size, size);
                break;
            case CIRCLE:
                maskG2d.fillOval(x, y, size, size);
                break;
            case SPRAY:
                // For spray, do individual feathered dots
                int radius = size / 2;
                double area = Math.PI * radius * radius;
                int effectiveDensity = (int) (sprayDensity * (area / (Math.PI * SPRAY_REFERENCE_SIZE * SPRAY_REFERENCE_SIZE)));

                for (int i = 0; i < effectiveDensity; i++) {
                    double x_offset = (random.nextDouble() * 2 - 1) * radius;
                    double y_offset = (random.nextDouble() * 2 - 1) * radius;

                    if (x_offset * x_offset + y_offset * y_offset > radius * radius) {
                        continue;
                    }

                    int px = p.x + (int) x_offset;
                    int py = p.y + (int) y_offset;

                    if (px >= 0 && px < targetImage.getWidth() && py >= 0 && py < targetImage.getHeight()) {
                        // Check if this point is within the clipping region
                        if (g2d.getClip() == null || g2d.getClip().contains(px, py)) {
                            // Draw a small dot
                            maskG2d.fillOval(px - 1, py - 1, 3, 3);
                        }
                    }
                }
                break;
        }
        maskG2d.dispose();

        // Apply alpha feathering based on the mask
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(targetImage.getWidth(), x + size + 1);
        int endY = Math.min(targetImage.getHeight(), y + size + 1);

        // Alpha adjustment strength (using blend strength)
        float alphaStrength = maxBlendStrength;

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int maskRGB = maskImage.getRGB(i, j);
                // Only process visible pixels from the mask
                if ((maskRGB & 0xFF000000) != 0) {
                    // Check if this pixel is within the clipping region
                    if (g2d.getClip() == null || g2d.getClip().contains(i, j)) {
                        // Get current pixel
                        int currentRGB = targetImage.getRGB(i, j);

                        // Extract current alpha value
                        int alpha = (currentRGB >> 24) & 0xFF;

                        // Calculate new alpha based on blend strength
                        int newAlpha = Math.max(0, (int) (alpha * (1.0f - alphaStrength)));

                        // Apply new alpha, keeping RGB components the same
                        int newRGB = (currentRGB & 0x00FFFFFF) | (newAlpha << 24);
                        targetImage.setRGB(i, j, newRGB);
                    }
                }
            }
        }
    }

    /**
     * Paints using a spray effect at the specified point.
     *
     * @param e      The mouse event triggering the spray.
     * @param image  The target image to draw on.
     * @param g2d    The Graphics2D context for drawing.
     * @param center The center point of the spray effect.
     */
    private void sprayPaint(MouseEvent e, BufferedImage image, Graphics2D g2d, Point center) {
        Color paintColor;
        if (SwingUtilities.isLeftMouseButton(e)) {
            paintColor = canvas.getDrawingColor();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            paintColor = canvas.getFillColor();
        } else return;

        int radius = size / 2;
        double area = Math.PI * radius * radius;
        int effectiveDensity = (int) (sprayDensity * (area / (Math.PI * SPRAY_REFERENCE_SIZE * SPRAY_REFERENCE_SIZE)));
        for (int i = 0; i < effectiveDensity; i++) {
            double x_offset = (random.nextDouble() * 2 - 1) * radius;
            double y_offset = (random.nextDouble() * 2 - 1) * radius;

            if (x_offset * x_offset + y_offset * y_offset > radius * radius) {
                continue;
            }

            int x = center.x + (int) x_offset;
            int y = center.y + (int) y_offset;

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

    /**
     * Blends the current color with the paint color based on the blend strength.
     *
     * @param currentRGB    The current pixel color in RGB format.
     * @param blendStrength The strength of the blending (0.0 to 1.0).
     * @param paintColor    The color to blend with.
     * @return The blended color.
     */
    private Color getBlendedColor(int currentRGB, float blendStrength, Color paintColor) {
        Color currentColor = new Color(currentRGB, true);

        // Extract alpha channel
        int currentAlpha = currentColor.getAlpha();

        // If completely transparent, just use the paint color with some opacity
        if (currentAlpha == 0) {
            return new Color(
                    paintColor.getRed(),
                    paintColor.getGreen(),
                    paintColor.getBlue(),
                    (int) (255 * blendStrength)
            );
        }

        // For partially or fully opaque pixels, blend color components
        int r = (int) ((1 - blendStrength) * currentColor.getRed() + blendStrength * paintColor.getRed());
        int g = (int) ((1 - blendStrength) * currentColor.getGreen() + blendStrength * paintColor.getGreen());
        int b = (int) ((1 - blendStrength) * currentColor.getBlue() + blendStrength * paintColor.getBlue());

        // Blend the alpha channel as well
        int a = Math.min(255, currentAlpha + (int) (blendStrength * (255 - currentAlpha)));

        return new Color(r, g, b, a);
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
            case SQUARE:
                return "Brush tool: Square mode";
            case CIRCLE:
                return "Brush tool: Circle mode";
            case SPRAY:
                return "Brush tool: Spray mode";
            default:
                return "Brush tool: Unknown mode";
        }
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

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}