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

public class FreeHandSelectionTool extends AbstractSelectionTool {
    private final GeneralPath currentPath = new GeneralPath();
    private boolean isDrawingPath = false;
    private Rectangle selectionBounds = null;

    public FreeHandSelectionTool(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    protected void handleSelectionStart(MouseEvent e) {
        Selection selection = canvas.getSelection();

        if (selection == null || !selection.hasOutline()) {
            // Start new path selection
            startNewPath();
        } else if (selection.contains(worldStartPoint) && selection.isActive()) {
            // Start dragging existing selection
            startDragging(selection);
        } else {
            // Start new path at a different location
            startNewPath();
        }
    }

    private void startNewPath() {
        currentPath.reset();
        currentPath.moveTo(worldStartPoint.x, worldStartPoint.y);
        isDrawingPath = true;
        Selection selection = new Selection(currentPath, null);
        canvas.setSelection(selection);
    }

    private void startDragging(Selection selection) {
        isDragging = true;
        isDrawingPath = false;
        Rectangle bounds = selection.getBounds();
        if (originalSelectionLocation == null) {
            originalSelectionLocation = new Point(bounds.x, bounds.y);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            return;
        }

        if (isDrawingPath) {
            isDrawingPath = false;
            currentPath.closePath();
            finalizeSelection(selection);
        } else if (isDragging) {
            finalizeDrag(e, selection);
        }

        canvas.repaint();
    }

    @Override
    protected void finalizeSelection(Selection selection) {
        selectionBounds = currentPath.getBounds();

        // Check if selection is too small and clear if so
        if (isSelectionTooSmall(selectionBounds)) {
            canvas.clearSelection();
            originalSelectionLocation = null;
            return;
        }

        BufferedImage selectionContent = createSelectionImage();
        BufferedImage transparentContent = createTransparentSelectionImage(selectionContent);

        selection.setTransparent(transparencyEnabled);
        selection.setContent(transparentContent, canvas.getFillColor());
        originalSelectionLocation = new Point(selectionBounds.x, selectionBounds.y);

        canvas.notifyClipboardStateChanged();
    }

    private BufferedImage createSelectionImage() {
        BufferedImage selectionContent = new BufferedImage(
                selectionBounds.width, selectionBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = selectionContent.createGraphics();

        // Set the clip to the path (translated to image coordinates)
        GeneralPath translatedPath = new GeneralPath(currentPath);
        translatedPath.transform(AffineTransform.getTranslateInstance(-selectionBounds.x, -selectionBounds.y));
        g2d.setClip(translatedPath);

        // Draw the composite of all visible layers instead of just the canvas image
        List<Layer> layers = canvas.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), -selectionBounds.x, -selectionBounds.y, null);
            }
        }

        g2d.dispose();
        return selectionContent;
    }

    private void finalizeDrag(MouseEvent e, Selection selection) {
        isDragging = false;
        Point worldEndPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (selection.contains(worldEndPoint)) {
            canvas.setCursor(handCursor);
        } else {
            canvas.setCursor(crosshairCursor);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Selection selection = canvas.getSelection();

        if (selection == null) {
            return;
        }

        if (isDrawingPath) {
            currentPath.lineTo(worldDragPoint.x, worldDragPoint.y);
        } else if (isDragging) {
            updatePathLocation(worldDragPoint, selection);
        }

        canvas.repaint();
    }

    private void updatePathLocation(Point worldDragPoint, Selection selection) {
        // Calculate the new position
        int dx = worldDragPoint.x - worldStartPoint.x;
        int dy = worldDragPoint.y - worldStartPoint.y;

        // Translate the path
        GeneralPath path = selection.getPath();
        if (path != null) {
            AffineTransform transform = new AffineTransform();
            transform.translate(dx, dy);
            path.transform(transform);
            worldStartPoint = worldDragPoint;
        }
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // Handle zooming if needed
    }

    @Override
    public String statusMessage() {
        return "Freehand selection tool selected";
    }


    @Override
    protected void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content) {
        Rectangle bounds = selection.getPath().getBounds();
        g2d.drawImage(content, bounds.x, bounds.y, null);
    }
}