package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class FillTool implements DrawingTool {
    private static final int FILL_EPSILON = 30;
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    public FillTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point point = e.getPoint();
        canvas.saveToUndoStack();
        BufferedImage bufferedImage = (BufferedImage) canvas.getImage();
        Color targetColor = new Color(bufferedImage.getRGB(point.x, point.y));
        Color replacementColor = SwingUtilities.isLeftMouseButton(e) ? canvas.getDrawingColor() : canvas.getFillColor();
        floodFill(bufferedImage, point.x, point.y, targetColor, replacementColor, FILL_EPSILON);
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // No action needed for mouse dragged
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        return "Fill tool selected";
    }

    private void floodFill(BufferedImage image, int x, int y, Color targetColor, Color replacementColor, int epsilon) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        if (targetRGB == replacementRGB) {
            return; // Avoid infinite loop if fill color is same as target
        }

        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            x = p.x;
            y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height || image.getRGB(x, y) == replacementRGB) {
                continue;
            }

            if (colorDistance(image.getRGB(x, y), targetRGB) <= epsilon) {
                image.setRGB(x, y, replacementRGB);
                stack.push(new Point(x + 1, y));
                stack.push(new Point(x - 1, y));
                stack.push(new Point(x, y + 1));
                stack.push(new Point(x, y - 1));
            }
        }
    }

    private double colorDistance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        double deltaR = r1 - r2;
        double deltaG = g1 - g2;
        double deltaB = b1 - b2;

        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB);
    }
}