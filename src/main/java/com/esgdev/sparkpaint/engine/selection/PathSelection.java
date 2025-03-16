package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;


/// Represents the selection area and its content in the drawing canvas.
public class PathSelection implements Selection {
    private GeneralPath path;
    private BufferedImage content;

    public PathSelection(GeneralPath path, BufferedImage content) {
        this.path = path;
        this.content = content;
    }

    public GeneralPath getPath() {
        return path;
    }

    public void setPath(GeneralPath path) {
        this.path = path;
    }

    public BufferedImage getContent() {
        return content;
    }

    public void setContent(BufferedImage content) {
        this.content = content;
    }

    public boolean isEmpty() {
        return path == null || content == null;
    }

    public boolean contains(Point point) {
        return path != null && path.contains(point);
    }

    public void clear() {
        path = null;
        content = null;
    }

    @Override
    public void clearOutline() {
        path = null;
    }

    @Override
    public boolean hasOutline() {
        return path != null;
    }

    @Override
    public Rectangle getBounds() {
        if (path != null) {
            return path.getBounds();
        }
        return null;
    }

    @Override
    public void delete(Graphics2D g2d, Color canvasBackground) {
        // TODO: Implement the delete method for path selection
    }
}