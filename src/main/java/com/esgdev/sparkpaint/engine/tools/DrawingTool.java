package com.esgdev.sparkpaint.engine.tools;

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

    default Point screenToWorld(float zoomFactor, Point point) {
        return new Point((int) (point.x / zoomFactor), (int) (point.y / zoomFactor));
    }

    default Point worldToScreen(Point worldPoint, float zoomFactor) {
        return new Point((int) (worldPoint.x * zoomFactor), (int) (worldPoint.y * zoomFactor));
    }

    // Scale a rectangle from screen space to world space
    default Rectangle screenToWorld(Rectangle screenRect, float zoomFactor) {
        int x = (int) (screenRect.x / zoomFactor);
        int y = (int) (screenRect.y / zoomFactor);
        int width = (int) (screenRect.width / zoomFactor);
        int height = (int) (screenRect.height / zoomFactor);
        return new Rectangle(x, y, width, height);
    }

    default void worldToScreen(float zoomFactor, Rectangle rectangle) {
        int x = (int) (rectangle.x * zoomFactor);
        int y = (int) (rectangle.y * zoomFactor);
        int width = (int) (rectangle.width * zoomFactor);
        int height = (int) (rectangle.height * zoomFactor);

        rectangle.setBounds(x, y, width, height);
    }

    // Scale a rectangle from world space to screen space
    default Rectangle worldToScreen(Rectangle worldRect, float zoomFactor) {
        int x = (int) (worldRect.x * zoomFactor);
        int y = (int) (worldRect.y * zoomFactor);
        int width = (int) (worldRect.width * zoomFactor);
        int height = (int) (worldRect.height * zoomFactor);
        return new Rectangle(x, y, width, height);
    }
}