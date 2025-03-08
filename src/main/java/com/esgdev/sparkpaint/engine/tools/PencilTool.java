package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.event.MouseEvent;

public class PencilTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;

    public PencilTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        canvas.saveToUndoStack();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point point = e.getPoint();
        Graphics2D g2d = canvas.getCanvasGraphics();
        if (g2d == null) {
            System.out.println("Graphics is null");
            return;
        }
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
        startPoint = point;
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        return "Pencil tool selected";
    }
}