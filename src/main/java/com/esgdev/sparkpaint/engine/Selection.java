package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;


/// Represents the selection area and its content in the drawing canvas.
public class Selection {
    private Rectangle rectangle;
    private BufferedImage content;
    private Color transparencyColor;

    public Selection(Rectangle rectangle, BufferedImage content) {
        this.rectangle = rectangle;
        this.content = content;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public BufferedImage getContent() {
        return content;
    }

    public void setContent(BufferedImage content) {
        this.content = content;
    }

    public boolean isEmpty() {
        return rectangle == null || content == null;
    }

    public void clear() {
        rectangle = null;
        content = null;
    }
}