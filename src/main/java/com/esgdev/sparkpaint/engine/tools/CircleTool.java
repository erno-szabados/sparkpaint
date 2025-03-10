package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class CircleTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isFilled;
    private boolean useAntiAliasing = true;

    public CircleTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.isFilled = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }


    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = scalePoint(canvas, e.getPoint());
        canvas.saveToUndoStack();
        canvas.saveCanvasState();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point point = scalePoint(canvas, e.getPoint());
        BufferedImage tempCanvas = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempCanvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.drawImage(canvas.getImage(), 0, 0, null);
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setColor(canvas.getDrawingColor());
        int radius = (int) Math.sqrt(Math.pow(point.x - startPoint.x, 2) + Math.pow(point.y - startPoint.y, 2));
        int x = startPoint.x - radius;
        int y = startPoint.y - radius;
        int diameter = radius * 2;
        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, diameter, diameter);
        }
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawOval(x, y, diameter, diameter);
        g2d.dispose();
        canvas.setTempCanvas(tempCanvas);
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point point = scalePoint(canvas, e.getPoint());
        BufferedImage image = (BufferedImage) canvas.getImage();
        Graphics2D g2d = image.createGraphics();
        if (g2d == null) {
            System.out.println("Graphics is null");
            return;
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        int radius = (int) Math.sqrt(Math.pow(point.x - startPoint.x, 2) + Math.pow(point.y - startPoint.y, 2));
        int x = startPoint.x - radius;
        int y = startPoint.y - radius;
        int diameter = radius * 2;
        if (isFilled) {
            g2d.setColor(canvas.getFillColor());
            g2d.fillOval(x, y, diameter, diameter);
        }
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawOval(x, y, diameter, diameter);
        g2d.dispose();
        canvas.setTempCanvas(null);
        canvas.repaint();
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {

    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        return "Circle tool selected";
    }

    public void setFilled(boolean selected) {
        this.isFilled = selected;
    }

    // Add getter/setter for anti-aliasing
    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }


}