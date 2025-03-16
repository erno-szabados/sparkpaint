package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.image.BufferedImage;


/// Represents the selection area and its content in the drawing canvas.
public class RectangleSelection implements Selection {
    private Rectangle rectangle;
    private BufferedImage content;

    public RectangleSelection(Rectangle rectangle, BufferedImage content) {
        this.rectangle = rectangle;
        this.content = content;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    @Override
    public BufferedImage getContent() {
        return content;
    }

    @Override
    public void setContent(BufferedImage content) {
        this.content = content;
    }

    @Override
    public boolean isEmpty() {
        return rectangle == null || content == null;
    }

    @Override
    public boolean contains(Point point) {
        return rectangle != null && rectangle.contains(point);
    }

    @Override
    public void clear() {
        rectangle = null;
        content = null;
    }

    public void clearOutline() {
        rectangle = null;
    }

    @Override
    public boolean hasOutline() {
        return rectangle != null;
    }

    @Override
    public Rectangle getBounds() {
        if (rectangle != null) {
            return rectangle.getBounds();
        }
        return null;
    }

    @Override
    public void delete(Graphics2D g2d, Color canvasBackground) {
        g2d.setColor(canvasBackground);
        g2d.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        g2d.dispose();
    }
}