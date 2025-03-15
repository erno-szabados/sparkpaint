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
    private Point startPoint;
    private boolean isDragging = false;
    private Point dragOffset = null;
    private Point originalSelectionLocation = null;
    private final SelectionManager selectionManager;
    private boolean transparencyEnabled = false;


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
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
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

        if (SwingUtilities.isRightMouseButton(e)) {
            // Right-click to clear selection
            copySelectionToPermanentCanvas();
            selectionManager.clearSelection();
            isDragging = false;
            originalSelectionLocation = null;
            canvas.repaint();
            return;
        }

        startPoint = e.getPoint();
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle != null) {
            if (selectionRectangle.contains(e.getPoint())) {
                // Starting a drag operation
                isDragging = true;
                dragOffset = new Point(e.getX() - selectionRectangle.x, e.getY() - selectionRectangle.y);
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
            selectionManager.getSelection().setRectangle(new Rectangle(startPoint.x, startPoint.y, 0, 0));
            originalSelectionLocation = null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (canvas.getZoomFactor() != 1) {
            return;
        }
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;

        if (isDragging) {
            isDragging = false;  // End drag operation
            if (selectionRectangle.contains(e.getPoint())) {
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
                    applyTransparencyToContent(selectionContent, canvas.getCanvasBackground());
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

    // Add this helper method
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
        if (canvas.getZoomFactor() != 1) {
            return;
        }
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;
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
        if (selectionContent != null) {
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
        float[] dashPattern = {5, 5}; // Define a pattern: 5px dash, 5px gap
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
        g2d.draw(selectionManager.getSelection().getRectangle());
    }

    private void clearSelectionOriginalLocation() {
        Rectangle selectionRectangle = selectionManager.getSelection().getRectangle();
        if (selectionRectangle == null) return;
        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getCanvasGraphics();
        g2d.setColor(canvas.getCanvasBackground());
        g2d.fillRect(originalSelectionLocation.x, originalSelectionLocation.y, selectionRectangle.width, selectionRectangle.height);
    }

    private void copySelectionToPermanentCanvas() {
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