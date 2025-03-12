package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.image.BufferedImage;


/// Represents the selection area and its content in the drawing canvas.
public class Selection {
    private Rectangle rectangle;
    private BufferedImage content;

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

    public void applyTransparency(int transparency) {
        if (content == null) return;

        for (int y = 0; y < content.getHeight(); y++) {
            for (int x = 0; x < content.getWidth(); x++) {
                int rgba = content.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xff;
                alpha = (alpha * transparency) / 255;
                rgba = (rgba & 0x00ffffff) | (alpha << 24);
                content.setRGB(x, y, rgba);
            }
        }
    }
}