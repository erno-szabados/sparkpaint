package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class BrushTool implements DrawingTool {
    public enum BrushShape {
        PIXEL,
        SQUARE,
        CIRCLE
    }


    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point lastPoint;
    private BrushShape shape = BrushShape.PIXEL;
    private int size = 1;

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
        drawShape(lastPoint);
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point currentPoint = scalePoint(canvas, e.getPoint());
        drawShape(currentPoint);
        lastPoint = currentPoint;
        canvas.repaint();
    }

    private void drawShape(Point p) {
        BufferedImage image = (BufferedImage) canvas.getImage();
        if (image != null) {
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(canvas.getDrawingColor());
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = p.x - size / 2;
            int y = p.y - size / 2;

            switch (shape) {
                case PIXEL:
                    image.setRGB(p.x, p.y, canvas.getDrawingColor().getRGB());
                    break;
                case SQUARE:
                    g2d.fillRect(x, y, size, size);
                    break;
                case CIRCLE:
                    g2d.fillOval(x, y, size, size);
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
        return "Brush tool selected (pixel mode)";
    }
}