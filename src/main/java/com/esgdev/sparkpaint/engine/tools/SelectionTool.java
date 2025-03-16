package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.SelectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/// Handles user interactions for selecting and manipulating an area of the drawing canvas.
public class SelectionTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor crosshairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point worldStartPoint;
    private boolean isDragging = false;
    private Point worldDragOffset = null;
    private Point originalSelectionLocation = null;
    private final SelectionManager selectionManager;
    private boolean transparencyEnabled = false;
    private final Rectangle scaledSelectionRectangle = new Rectangle();


    public SelectionTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.selectionManager = canvas.getSelectionManager();
    }

    // Add this method
    public void setTransparencyEnabled(boolean enabled) {
        this.transparencyEnabled = enabled;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point worldPoint = screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle != null && selectionRectangle.contains(worldPoint)) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

   @Override
    public void mousePressed(MouseEvent e) {

        if (SwingUtilities.isRightMouseButton(e)) {
            // Right-click to clear selection
            copySelectionToPermanentCanvas();
            selectionManager.clearSelection();
            isDragging = false;
            originalSelectionLocation = null;
            canvas.repaint();
            return;
        }

        Point screenStartPoint = e.getPoint();
        worldStartPoint = screenToWorld(canvas.getZoomFactor(), screenStartPoint);
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle != null) {
            if (selectionRectangle.contains(worldStartPoint)) {
                // Starting a drag operation
                isDragging = true;
                worldDragOffset = new Point(
                        (int) (worldStartPoint.getX() - selectionRectangle.x),
                        (int) (worldStartPoint.getY() - selectionRectangle.y));
                // Only set originalSelectionLocation if it hasn't been set yet
                if (originalSelectionLocation == null) {
                    originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                }
            } else {
                if (!selectionManager.getSelection().isEmpty()) {
                    // clicked outside of selection, finalize the current selection
                    isDragging = false;
                    copySelectionToPermanentCanvas();
                    selectionManager.clearSelection();
                    originalSelectionLocation = null;
                }
            }
        } else {
            // Starting a new selection
            selectionManager.getSelection().setRectangle(new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0));
            originalSelectionLocation = null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;

        Point worldEndPoint = screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (isDragging) {
            isDragging = false;  // End drag operation
            if (selectionRectangle.contains(worldEndPoint)) {
                canvas.setCursor(handCursor);
            } else {
                canvas.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            // Handling new selection
            canvas.notifyClipboardStateChanged();
            if (selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                BufferedImage selectionContent = new BufferedImage(selectionRectangle.width, selectionRectangle.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = selectionContent.createGraphics();
                g2d.drawImage(canvas.getImage(), -selectionRectangle.x, -selectionRectangle.y, null);
                g2d.dispose();


                // Apply transparency if enabled
                if (transparencyEnabled) {
                    applyTransparencyToContent(selectionContent, canvas.getFillColor());
                }

                selectionManager.getSelection().setContent(selectionContent);
                originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                clearSelectionOriginalLocation();
            } else {
                selectionManager.getSelection().setContent(null);
                originalSelectionLocation = null;
            }
        }
        canvas.repaint();
    }

    private void applyTransparencyToContent(BufferedImage content, Color transparentColor) {
        for (int y = 0; y < content.getHeight(); y++) {
            for (int x = 0; x < content.getWidth(); x++) {
                int rgba = content.getRGB(x, y);
                Color pixelColor = new Color(rgba, true);

                if (pixelColor.getRGB() == transparentColor.getRGB()) {
                    content.setRGB(x, y, 0x00000000);
                } else {
                    content.setRGB(x, y, rgba | 0xFF000000);
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point screenDragPoint = e.getPoint();
        Point worldDragPoint = screenToWorld(canvas.getZoomFactor(), screenDragPoint);
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;
        if (isDragging) {
            int newX = (int) (worldDragPoint.getX() - worldDragOffset.x);
            int newY = (int) (worldDragPoint.getY() - worldDragOffset.y);
            selectionRectangle.setLocation(newX, newY);
        } else {
            int x = (int) Math.min(worldStartPoint.x, worldDragPoint.getX());
            int y = (int) Math.min(worldStartPoint.y, worldDragPoint.getY());
            int width = (int) Math.abs(worldDragPoint.getX() - worldStartPoint.x);
            int height = (int) Math.abs(worldDragPoint.getY() - worldStartPoint.y);
            selectionRectangle.setBounds(x, y, width, height);
        }
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

    // Add to SelectionTool class

    public void rotateSelection(int degrees) {
        if (selectionManager.getSelection().getContent() == null) return;

        BufferedImage original = selectionManager.getSelection().getContent();
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

        // Save state before rotation
        canvas.saveToUndoStack();

        // Clear original area
        Rectangle rect = selectionManager.getSelection().getRectangle();

        // Update selection content and rectangle
        selectionManager.getSelection().setContent(rotated);
        rect.setSize(rotated.getWidth(), rotated.getHeight());

        canvas.repaint();
    }

    public void drawSelection(Graphics2D g2d) {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        Image selectionContent = selectionManager.getSelection().getContent();
        // If dragging, draw the selection content at current position
        if (selectionContent != null && selectionRectangle != null) {
            if (selectionContent.getWidth(null) > 0 &&
                    selectionContent.getHeight(null) > 0) {
                g2d.drawImage(selectionContent,
                        selectionRectangle.x,
                        selectionRectangle.y,
                        null);
            }
        }
    }

    public void drawSelectionRectangle(Graphics2D g2d) {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();

        if (selectionRectangle == null ||
                selectionRectangle.width <= 0 ||
                selectionRectangle.height <= 0) {
            return;
        }


        float[] dashPattern1 = {5, 5}; // Define the first pattern: 5px dash, 5px gap

        BasicStroke dottedStroke1 = new BasicStroke(
                1, // Line Width
                BasicStroke.CAP_BUTT, // End-cap style
                BasicStroke.JOIN_MITER, // Join style
                10.0f, // Miter limit
                dashPattern1, // Dash pattern (dotted line)
                0 // Dash phase
        );

        BasicStroke dottedStroke2 = new BasicStroke(
                1, // Line Width
                BasicStroke.CAP_BUTT, // End-cap style
                BasicStroke.JOIN_MITER, // Join style
                10.0f, // Miter limit
                dashPattern1, // Dash pattern (dotted line)
                5 // Dash phase
        );

        double zoomFactor = canvas.getZoomFactor();

        int x = (int) (selectionRectangle.x * zoomFactor);
        int y = (int) (selectionRectangle.y * zoomFactor);
        int width = (int) (selectionRectangle.width * zoomFactor);
        int height = (int) (selectionRectangle.height * zoomFactor);

        scaledSelectionRectangle.setBounds(x, y, width, height);
        // Draw the selection rectangle with the first dash pattern
        g2d.setColor(Color.BLACK);
        g2d.setStroke(dottedStroke1);
        g2d.draw(scaledSelectionRectangle);

        // Draw the selection rectangle with the second dash pattern
        g2d.setColor(Color.WHITE);
        g2d.setStroke(dottedStroke2);
        g2d.draw(scaledSelectionRectangle);
    }

    private void clearSelectionOriginalLocation() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;
        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getCanvasGraphics();
        g2d.setColor(canvas.getCanvasBackground());
        g2d.fillRect(originalSelectionLocation.x, originalSelectionLocation.y, selectionRectangle.width, selectionRectangle.height);
    }

    public void copySelectionToPermanentCanvas() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;
        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getCanvasGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2d.drawImage(selectionManager.getSelection().getContent(), selectionRectangle.x, selectionRectangle.y, null);
        g2d.dispose();
        canvas.repaint();
    }
}