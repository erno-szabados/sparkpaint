package com.esgdev.sparkpaint.engine.tools;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.esgdev.sparkpaint.engine.selection.Selection;

public interface DrawingTool {
    void mouseMoved(MouseEvent e);

    void mousePressed(MouseEvent e);

    void mouseDragged(MouseEvent e);

    void mouseReleased(MouseEvent e);

    void mouseScrolled(MouseWheelEvent e);

    void setCursor();

    String statusMessage();

    static Point screenToWorld(float zoomFactor, Point point) {
        return new Point((int) (point.x / zoomFactor), (int) (point.y / zoomFactor));
    }

    static Point worldToScreen(Point worldPoint, float zoomFactor) {
        return new Point((int) (worldPoint.x * zoomFactor), (int) (worldPoint.y * zoomFactor));
    }

    // Scale a rectangle from screen space to world space
    static Rectangle screenToWorld(Rectangle screenRect, float zoomFactor) {
        double x = (screenRect.x / zoomFactor);
        double y = (screenRect.y / zoomFactor);
        double width = (screenRect.width / zoomFactor);
        double height = (screenRect.height / zoomFactor);
        return new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    static void worldToScreen(float zoomFactor, Rectangle rectangle) {
        int x = (int) (rectangle.x * zoomFactor);
        int y = (int) (rectangle.y * zoomFactor);
        int width = (int) (rectangle.width * zoomFactor);
        int height = (int) (rectangle.height * zoomFactor);

        rectangle.setBounds(x, y, width, height);
    }

    // Scale a rectangle from world space to screen space
    static Rectangle worldToScreen(Rectangle worldRect, float zoomFactor) {
        int x = (int) (worldRect.x * zoomFactor);
        int y = (int) (worldRect.y * zoomFactor);
        int width = (int) (worldRect.width * zoomFactor);
        int height = (int) (worldRect.height * zoomFactor);
        return new Rectangle(x, y, width, height);
    }

    default void applySelectionClip(Graphics2D g2d, Selection selection) {
        if (selection != null && selection.hasOutline()) {
            Rectangle selectionBounds = selection.getBounds();
            g2d.setClip(selectionBounds);
        }
    }
}