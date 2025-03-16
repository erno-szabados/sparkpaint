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

        rectangle.setSize(rotated.getWidth(), rotated.getHeight());
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

    @Override
    public void drawSelectionContent(Graphics2D g2d, double zoomFactor) {
        if (content != null && rectangle != null) {
            g2d.drawImage(content, rectangle.x, rectangle.y, null);
        }
    }

    @Override
    public void drawSelectionOutline(Graphics2D g2d, double zoomFactor) {
        if (rectangle == null) {
            return;
        }

        float[] dashPattern = {5, 5}; // Dash pattern: 5px dash, 5px gap

        BasicStroke dottedStroke1 = new BasicStroke(
                1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 0);

        BasicStroke dottedStroke2 = new BasicStroke(
                1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 5);

        // Scale the rectangle to match the zoom factor
        Rectangle scaledRectangle = new Rectangle(
                (int) (rectangle.x * zoomFactor),
                (int) (rectangle.y * zoomFactor),
                (int) (rectangle.width * zoomFactor),
                (int) (rectangle.height * zoomFactor)
        );

        // Draw the selection outline with the first dash pattern
        g2d.setColor(Color.BLACK);
        g2d.setStroke(dottedStroke1);
        g2d.draw(scaledRectangle);

        // Draw the selection outline with the second dash pattern
        g2d.setColor(Color.WHITE);
        g2d.setStroke(dottedStroke2);
        g2d.draw(scaledRectangle);
    }
}