package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class PencilTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean useAntiAliasing = true;

    public PencilTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        canvas.saveToUndoStack();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point point = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        BufferedImage image = canvas.getImage();
        Graphics2D g2d = image.createGraphics();
        if (g2d == null) {
            System.out.println("Graphics is null");
            return;
        }
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        if (SwingUtilities.isLeftMouseButton(e)) {
            g2d.setColor(canvas.getDrawingColor());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            g2d.setColor(canvas.getFillColor());
        }
        g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
        startPoint = point;
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
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
        return "Pencil tool selected";
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}