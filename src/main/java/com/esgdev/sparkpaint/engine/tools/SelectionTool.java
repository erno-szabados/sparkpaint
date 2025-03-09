package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class SelectionTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean isDragging = false;
    private Point dragOffset = null;
    private Point originalSelectionLocation = null;

    public SelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Rectangle selectionRectangle = canvas.getSelectionRectangle();
        if (selectionRectangle != null && selectionRectangle.contains(e.getPoint())) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (canvas.getZoomFactor() != 1) {
            return;
        }
        startPoint = e.getPoint();
        Rectangle selectionRectangle = canvas.getSelectionRectangle();
        if (selectionRectangle != null && selectionRectangle.contains(e.getPoint())) {
            isDragging = true;
            dragOffset = new Point(e.getX() - selectionRectangle.x, e.getY() - selectionRectangle.y);
            originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
        } else {
            canvas.setSelectionRectangle(new Rectangle(startPoint.x, startPoint.y, 0, 0));
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (canvas.getZoomFactor() != 1) {
            return;
        }
        Rectangle selectionRectangle = canvas.getSelectionRectangle();
        if (isDragging) {
            int newX = e.getX() - dragOffset.x;
            int newY = e.getY() - dragOffset.y;
            selectionRectangle.setLocation(newX, newY);
        } else {
            int x = Math.min(startPoint.x, e.getX());
            int y = Math.min(startPoint.y, e.getY());
            int width = Math.abs(e.getX() - startPoint.x);
            int height = Math.abs(e.getY() - startPoint.y);
            selectionRectangle.setBounds(x, y, width, height);
        }
        canvas.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (canvas.getZoomFactor() != 1) {
            return;
        }
        Rectangle selectionRectangle = canvas.getSelectionRectangle();
        if (isDragging) {
            isDragging = false;
            if (selectionRectangle.contains(e.getPoint())) {
                canvas.setCursor(handCursor);
            } else {
                canvas.setCursor(Cursor.getDefaultCursor());
            }
            if (canvas.getSelectionContent() != null) {
                canvas.saveToUndoStack();
                Graphics2D g2d = canvas.getCanvasGraphics();
                g2d.setColor(canvas.getCanvasBackground());
                g2d.fillRect(originalSelectionLocation.x, originalSelectionLocation.y, selectionRectangle.width, selectionRectangle.height);
                g2d.drawImage(canvas.getSelectionContent(), selectionRectangle.x, selectionRectangle.y, null);
                g2d.dispose();
                canvas.repaint();
            }
        } else {
            // not dragging, Selecting
            canvas.notifyClipboardStateChanged();
            // Selection finished, copy the selected area to a BufferedImage
            if (selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                BufferedImage selectionContent = new BufferedImage(selectionRectangle.width, selectionRectangle.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = selectionContent.createGraphics();
                g2d.drawImage(canvas.getImage(), -selectionRectangle.x, -selectionRectangle.y, null);
                g2d.dispose();
                canvas.setSelectionContent(selectionContent);
            } else {
                canvas.setSelectionContent(null);
            }
        }
        canvas.setTempCanvas(null);
        canvas.repaint();
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {

    }

    @Override
    public void setCursor() {
        canvas.setCursor(crosshairCursor);
    }

    @Override
    public String statusMessage() {
        return "Selection tool selected";
    }

    public void drawSelection(Graphics2D g2d) {
        Rectangle selectionRectangle = canvas.getSelectionRectangle();
        Image selectionContent = canvas.getSelectionContent();
        // If dragging, draw the selection content at current position
        if (isDragging && selectionContent != null) {

            // Show the background color in the original position
            g2d.setColor(canvas.getCanvasBackground());
            g2d.fillRect(
                    originalSelectionLocation.x,
                    originalSelectionLocation.y,
                    selectionRectangle.width,
                    selectionRectangle.height);

            if (selectionContent.getWidth(null) > 0 &&
                selectionContent.getHeight(null) > 0) {
                g2d.drawImage(selectionContent,
                        selectionRectangle.x,
                        selectionRectangle.y,
                        null);
            }
        }

        if (selectionRectangle != null &&
                selectionRectangle.width > 0 &&
                selectionRectangle.height > 0) {
            drawSelectionRectangle(g2d);
        }
    }

    private void drawSelectionRectangle(Graphics2D g2d) {
        // Draw the dotted border
        float[] dashPattern = { 5, 5 }; // Define a pattern: 5px dash, 5px gap
        BasicStroke dottedStroke = new BasicStroke(
                1, // Line Width
                BasicStroke.CAP_BUTT, // End-cap style
                BasicStroke.JOIN_MITER, // Join style
                10.0f, // Miter limit
                dashPattern, // Dash pattern (dotted line)
                0 // Dash phase
        );
        g2d.setColor(Color.BLACK);
        g2d.setStroke(dottedStroke);
        g2d.draw(canvas.getSelectionRectangle());
    }
}