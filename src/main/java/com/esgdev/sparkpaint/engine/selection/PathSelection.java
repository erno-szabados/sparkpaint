package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.geom.AffineTransform;
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
    public void rotate(int degrees) {
        if (content == null) return;

        BufferedImage original = content;
        int width = original.getWidth();
        int height = original.getHeight();

        // Create new rotated image
        BufferedImage rotated = new BufferedImage(
                degrees % 180 == 0 ? width : height,
                degrees % 180 == 0 ? height : width,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = rotated.createGraphics();
        g2d.translate((rotated.getWidth() - width) / 2,
                (rotated.getHeight() - height) / 2);
        g2d.rotate(Math.toRadians(degrees), width / 2.0, height / 2.0);
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        content = rotated;

        // Rotate the path
        Rectangle bounds = path.getBounds();
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(degrees), bounds.getCenterX(), bounds.getCenterY());
        path.transform(transform);
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
        if (path == null) {
            return;
        }

        // Set the fill color to the canvas background
        g2d.setColor(canvasBackground);

        // Fill the path with the background color
        g2d.fill(path.getBounds());
    }


    @Override
    public void drawSelectionContent(Graphics2D g2d, double zoomFactor) {
        if (content != null && path != null) {
            Rectangle bounds = path.getBounds();
            g2d.drawImage(content, bounds.x, bounds.y, null);
        }
    }

    @Override
    public void drawSelectionOutline(Graphics2D g2d, double zoomFactor) {
        if (path == null) {
            return;
        }

        float[] dashPattern = {5, 5}; // Dash pattern: 5px dash, 5px gap

        BasicStroke dottedStroke1 = new BasicStroke(
                1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 0);

        BasicStroke dottedStroke2 = new BasicStroke(
                1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 5);

        // Scale the path to match the zoom factor
        GeneralPath scaledPath = new GeneralPath(path);
        scaledPath.transform(AffineTransform.getScaleInstance(zoomFactor, zoomFactor));

        // Draw the selection outline with the first dash pattern
        g2d.setColor(Color.BLACK);
        g2d.setStroke(dottedStroke1);
        g2d.draw(scaledPath);

        // Draw the selection outline with the second dash pattern
        g2d.setColor(Color.WHITE);
        g2d.setStroke(dottedStroke2);
        g2d.draw(scaledPath);
    }
}