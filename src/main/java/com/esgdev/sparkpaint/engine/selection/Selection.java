package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Selection {
    BufferedImage getContent();

    void setContent(BufferedImage content);

    boolean isEmpty();

    boolean contains(Point point);

    void clear();

    void clearOutline();

    boolean hasOutline();

    Rectangle getBounds();

    void delete(Graphics2D g2d, Color canvasBackground);
}
