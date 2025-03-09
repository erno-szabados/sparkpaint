package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public interface DrawingTool {
    void mouseMoved(MouseEvent e);
    void mousePressed(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void mouseReleased(MouseEvent e);
    void mouseScrolled(MouseWheelEvent e);
    void setCursor();
    String statusMessage();

    default Point scalePoint(DrawingCanvas canvas, Point point) {
        float zoomFactor = canvas.getZoomFactor();
        return new Point((int) (point.x / zoomFactor), (int) (point.y / zoomFactor));
    }
}