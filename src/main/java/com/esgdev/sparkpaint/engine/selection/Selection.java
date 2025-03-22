package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;


/**
 * Represents a selection area in a drawing canvas.
 * The selection can be defined by a path (GeneralPath) and can contain an image.
 */
public class Selection {
    private GeneralPath path;
    private BufferedImage content;
    private boolean transparent;

    /**
     * Creates a new Selection object with the specified rectangle and content.
     *
     * @param rect    The rectangle defining the selection area.
     * @param content The image content of the selection.
     */
    public Selection(Rectangle rect, BufferedImage content) {
        this.path = new GeneralPath();
        path.append(rect, false);
        this.content = content;
    }

    /**
     * Creates a new Selection object with the specified path and content.
     *
     * @param path    The GeneralPath defining the selection area.
     * @param content The image content of the selection.
     */
    public Selection(GeneralPath path, BufferedImage content) {
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

    public boolean isTransparent() {
        return this.transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public boolean contains(Point point) {
        return path != null && path.contains(point);
    }

    public void clear() {
        path = null;
        content = null;
    }


    public void clearOutline() {
        path = null;
    }


    public boolean hasOutline() {
        return path != null;
    }


    /**
     * Rotates the selection content and path by the specified degrees.
     *
     * @param degrees The angle in degrees to rotate the selection.
     */
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


    public Rectangle getBounds() {
        return path != null ? path.getBounds() : null;
    }

    /**
     * Draws the selection content on the provided Graphics2D object.
     *
     * @param g2d The Graphics2D object to draw on.
     */
    public void drawSelectionContent(Graphics2D g2d) {
        if (content != null && path != null) {
            Rectangle bounds = path.getBounds();
            g2d.drawImage(content, bounds.x, bounds.y, null);
        }
    }

    /**
     * Draws a dotted outline around the selection path.
     *
     * @param g2d        The Graphics2D object to draw on.
     * @param zoomFactor  The zoom factor for scaling the outline.
     */
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

    /**
     * Mirrors the selection content horizontally.
     */
    public void flipHorizontal() {
        if (getContent() == null) return;

        BufferedImage content = getContent();
        int width = content.getWidth();
        int height = content.getHeight();

        BufferedImage mirrored = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mirrored.createGraphics();

        // Draw the original image flipped horizontally
        g2d.scale(-1, 1);
        g2d.translate(-width, 0);
        g2d.drawImage(content, 0, 0, null);
        g2d.dispose();

        setContent(mirrored);
    }

    /**
     * Mirrors the selection content vertically.
     */
    public void flipVertical() {
        if (getContent() == null) return;

        BufferedImage content = getContent();
        int width = content.getWidth();
        int height = content.getHeight();

        BufferedImage mirrored = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mirrored.createGraphics();

        // Draw the original image flipped vertically
        g2d.scale(1, -1);
        g2d.translate(0, -height);
        g2d.drawImage(content, 0, 0, null);
        g2d.dispose();

        setContent(mirrored);
    }
}