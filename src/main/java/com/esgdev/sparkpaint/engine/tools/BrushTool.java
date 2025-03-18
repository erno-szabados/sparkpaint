package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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
        lastPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        canvas.saveToUndoStack();
        drawShape(e, lastPoint);
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point currentPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        drawShape(e, currentPoint);
        lastPoint = currentPoint;
        canvas.repaint();
    }

    private void drawShape(MouseEvent e, Point p) {
        BufferedImage image = canvas.getImage();
        if (image != null) {
            Color paintColor;
            if (SwingUtilities.isLeftMouseButton(e)) {
                paintColor = canvas.getDrawingColor();
            } else if (SwingUtilities.isRightMouseButton(e)) {
                paintColor = canvas.getFillColor();
            } else {
                return;
            }

            int x = p.x - size / 2;
            int y = p.y - size / 2;

            switch (shape) {
                case SQUARE:
                    drawBlendedShape(image, x, y, size, size, paintColor, true);
                    break;
                case CIRCLE:
                    drawBlendedShape(image, x, y, size, size, paintColor, false);
                    break;
                case SPRAY:
                    sprayPaint(e, image, p);
                    break;
            }
        }
    }

    private void drawBlendedShape(BufferedImage image, int x, int y, int width, int height, Color paintColor, boolean isSquare) {
        // Create a temporary image for the shape
        BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        g2d.setColor(paintColor);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw the shape to the temporary image
        if (isSquare) {
            g2d.fillRect(x, y, width, height);
        } else {
            g2d.fillOval(x, y, width, height);
        }
        g2d.dispose();

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
                    int currentRGB = image.getRGB(i, j);
                    Color currentColor = new Color(currentRGB, true);

                    // Apply blending with a consistent strength
                    float blend = maxBlendStrength;
                    int r = (int) ((1 - blend) * currentColor.getRed() + blend * paintColor.getRed());
                    int g = (int) ((1 - blend) * currentColor.getGreen() + blend * paintColor.getGreen());
                    int b = (int) ((1 - blend) * currentColor.getBlue() + blend * paintColor.getBlue());

                    Color blendedColor = new Color(r, g, b);
                    image.setRGB(i, j, blendedColor.getRGB());
                }
            }
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

    private void sprayPaint(MouseEvent e, BufferedImage image, Point center) {
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
                int currentRGB = image.getRGB(x, y);
                Color currentColor = new Color(currentRGB, true);

                float blend = random.nextFloat() * maxBlendStrength;
                int r = (int) ((1 - blend) * currentColor.getRed() + blend * paintColor.getRed());
                int g = (int) ((1 - blend) * currentColor.getGreen() + blend * paintColor.getGreen());
                int b = (int) ((1 - blend) * currentColor.getBlue() + blend * paintColor.getBlue());

                Color blendedColor = new Color(r, g, b);
                image.setRGB(x, y, blendedColor.getRGB());
            }
        }
    }
}