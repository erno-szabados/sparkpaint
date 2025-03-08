package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class EyedropperTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    public EyedropperTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point point = e.getPoint();
        BufferedImage bufferedImage = (BufferedImage) canvas.getImage();
        if (bufferedImage == null) {
            return;
        }

        int rgb = bufferedImage.getRGB(point.x, point.y);
        Color pickedColor = new Color(rgb);

        if (SwingUtilities.isLeftMouseButton(e)) {
            canvas.setDrawingColor(pickedColor);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            canvas.setFillColor(pickedColor);
        }
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
        return "Eyedropper tool selected";
    }
}