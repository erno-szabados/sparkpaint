package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Selection {
    BufferedImage getContent();

    void setContent(BufferedImage content);

    boolean isTransparent();

    void setTransparent(boolean transparent);

    boolean contains(Point point);

    void clear();

    void clearOutline();

    boolean hasOutline();

    void rotate(int degrees);

    Rectangle getBounds();

    void delete(Graphics2D g2d, Color canvasBackground);

    void drawSelectionContent(Graphics2D g2d, double zoomFactor);

    void drawSelectionOutline(Graphics2D g2d, double zoomFactor);
}
