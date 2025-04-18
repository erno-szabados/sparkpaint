package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

public class RectangleSelectionTool extends AbstractSelectionTool {
    private Point worldDragOffset = null;

    public RectangleSelectionTool(DrawingCanvas canvas) {
        super(canvas);
    }

    /**
     * Creates a rectangular selection based on the given rectangle and content.
     *
     * @param rect    The rectangle defining the selection area.
     * @param content The image content of the selection.
     * @return A new Selection object representing the rectangular selection.
     */
    public static Selection createRectangularSelection(Rectangle rect, BufferedImage content) {
        GeneralPath path = new GeneralPath();
        path.moveTo(rect.x, rect.y);
        path.lineTo(rect.x + rect.width, rect.y);
        path.lineTo(rect.x + rect.width, rect.y + rect.height);
        path.lineTo(rect.x, rect.y + rect.height);
        path.closePath();

        return new Selection(path, content);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = canvas.getSelection();
        if (selection == null) return;

        Point worldEndPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        if (isDragging) {
            finalizeDrag(worldEndPoint, selection);
        } else {
            finalizeSelection(selection);
        }

        canvas.repaint();
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Selection selection = canvas.getSelection();

        Rectangle selectionRectangle = selection.getBounds();
        if (selectionRectangle == null) return;

        if (isDragging) {
            updateRectangleLocation(worldDragPoint, selection);
        } else {
            // size the rectangle
            updateRectangleSize(worldDragPoint, selection);
        }

        canvas.repaint();
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // Handle zooming if needed
    }

    @Override
    public String statusMessage() {
        return "Selection tool selected";
    }

    @Override
    protected void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content) {
        Rectangle selectionRectangle = selection.getBounds();
        g2d.drawImage(content, selectionRectangle.x, selectionRectangle.y, null);
    }

    @Override
    protected void handleSelectionStart(MouseEvent e) {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            selection = new Selection(new Rectangle(), null);
            canvas.setSelection(selection);
        }

        if (!selection.hasOutline()) {
            // Start new rectangle selection
            startNewRectangle();
        } else if (selection.contains(worldStartPoint) && selection.isActive()) {
            // Start dragging existing selection (only if the selection is active)
            startDragging(selection);
        } else {
            // Start new rectangle at different location
            startNewRectangle();
        }
    }

    @Override
    protected void finalizeSelection(Selection selection) {
        Rectangle selectionRectangle = selection.getBounds();
        if (selectionRectangle == null) return;

        // Check if selection is too small and clear if so
        if (isSelectionTooSmall(selectionRectangle)) {
            canvas.clearSelection();
            originalSelectionLocation = null;
            return;
        }

        canvas.notifyClipboardStateChanged();

        if (selectionRectangle.width > 0 && selectionRectangle.height > 0) {
            BufferedImage selectionContent = createSelectionImage(selectionRectangle);
            BufferedImage transparentContent = createTransparentSelectionImage(selectionContent);

            selection.setTransparent(transparencyEnabled);
            selection.setContent(transparentContent, canvas.getFillColor());
            originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
        } else {
            selection.setContent(null);
            originalSelectionLocation = null;
        }
    }

    private BufferedImage createSelectionImage(Rectangle selectionRectangle) {
        BufferedImage selectionContent = new BufferedImage(
                selectionRectangle.width,
                selectionRectangle.height,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = selectionContent.createGraphics();

        // Draw the composite of all visible layers instead of just the canvas image
        List<Layer> layers = canvas.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(),
                        -selectionRectangle.x,
                        -selectionRectangle.y,
                        null);
            }
        }
        g2d.dispose();

        return selectionContent;
    }

    private void updateRectangleLocation(Point worldDragPoint, Selection selection) {
        int newX = worldDragPoint.x - worldDragOffset.x;
        int newY = worldDragPoint.y - worldDragOffset.y;
        GeneralPath path = selection.getPath();
        Rectangle bounds = path.getBounds();
        int deltaX = newX - bounds.x;
        int deltaY = newY - bounds.y;
        AffineTransform transform = AffineTransform.getTranslateInstance(deltaX, deltaY);
        path.transform(transform);
    }

    private void updateRectangleSize(Point worldDragPoint, Selection selection) {
        int x = Math.min(worldStartPoint.x, worldDragPoint.x);
        int y = Math.min(worldStartPoint.y, worldDragPoint.y);
        int width = Math.abs(worldDragPoint.x - worldStartPoint.x);
        int height = Math.abs(worldDragPoint.y - worldStartPoint.y);
        Rectangle rect = new Rectangle(x, y, width, height);
        GeneralPath path = selection.getPath();
        path.reset();
        path.append(rect, false);
    }


    private void startNewRectangle() {
        Rectangle initialRect = new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0);
        Selection selection = createRectangularSelection(initialRect, null);
        canvas.setSelection(selection);
        originalSelectionLocation = null;
    }

    private void startDragging(Selection selection) {
        isDragging = true;
        Rectangle selectionRectangle = selection.getBounds();
        worldDragOffset = new Point(
                worldStartPoint.x - selectionRectangle.x,
                worldStartPoint.y - selectionRectangle.y);

        // Only set originalSelectionLocation if it hasn't been set yet
        if (originalSelectionLocation == null) {
            originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
        }
    }

    private void finalizeDrag(Point worldEndPoint, Selection selection) {
        isDragging = false;
        if (selection.contains(worldEndPoint)) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }
}