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
    private BrushShape shape = BrushShape.SQUARE;
    private int size = DEFAULT_SPRAY_SIZE;
    private int sprayDensity = DEFAULT_SPRAY_DENSITY;
    private boolean useAntiAliasing = true;

    public BrushTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastPoint = scalePoint(canvas, e.getPoint());
        canvas.saveToUndoStack();
        drawShape(e, lastPoint);
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point currentPoint = scalePoint(canvas, e.getPoint());
        drawShape(e, currentPoint);
        lastPoint = currentPoint;
        canvas.repaint();
    }

    private void drawShape(MouseEvent e, Point p) {
        BufferedImage image = (BufferedImage) canvas.getImage();
        if (image != null) {
            Graphics2D g2d = image.createGraphics();
            if (SwingUtilities.isLeftMouseButton(e)) {
                g2d.setColor(canvas.getDrawingColor());
            } else if (SwingUtilities.isRightMouseButton(e)) {
                g2d.setColor(canvas.getFillColor());
            }
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            int x = p.x - size / 2;
            int y = p.y - size / 2;

            switch (shape) {
                case SQUARE:
                    g2d.fillRect(x, y, size, size);
                    break;
                case CIRCLE:
                    g2d.fillOval(x, y, size, size);
                    break;
                case SPRAY:
                    sprayPaint(e, image, p);
                    break;
            }
            g2d.dispose();
        }
    }

    public void setShape(BrushShape shape) {
        this.shape = shape;
    }

    public void setSize(int size) {
        this.size = size;
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
        int color;
        if (SwingUtilities.isLeftMouseButton(e)) {
            color = canvas.getDrawingColor().getRGB();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            color = canvas.getFillColor().getRGB();
        } else return;

        int radius = size / 2;

        for (int i = 0; i < sprayDensity; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;

            int dx = (int) (distance * Math.cos(angle));
            int dy = (int) (distance * Math.sin(angle));

            int x = center.x + dx;
            int y = center.y + dy;

            if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                image.setRGB(x, y, color);
            }
        }
    }
}