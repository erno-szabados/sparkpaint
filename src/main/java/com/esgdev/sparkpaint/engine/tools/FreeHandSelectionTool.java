package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.PathSelection;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class FreeHandSelectionTool extends AbstractSelectionTool {
    private final GeneralPath currentPath = new GeneralPath();
    private boolean isDrawingPath = false;
    private Rectangle selectionBounds = null;

    public FreeHandSelectionTool(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    protected boolean isValidSelectionType(Selection selection) {
        return selection instanceof PathSelection;
    }

    @Override
    protected void handleSelectionStart(MouseEvent e) {
        Selection selection = selectionManager.getSelection();

        if (!(selection instanceof PathSelection) || !selection.hasOutline()) {
            // Start new path selection
            startNewPath();
        } else if (selection.contains(worldStartPoint)) {
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
        Selection selection = new PathSelection(currentPath, null);
        selectionManager.setSelection(selection);
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
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection)) {
            return;
        }

        if (isDrawingPath) {
            finalizeDrawnPath(selection);
        } else if (isDragging) {
            finalizeDrag(e, selection);
        }

        canvas.repaint();
    }

    private void finalizeDrawnPath(Selection selection) {
        isDrawingPath = false;
        currentPath.closePath();
        selectionBounds = currentPath.getBounds();

        if (selectionBounds.width > 0 && selectionBounds.height > 0) {
            BufferedImage selectionContent = createSelectionImage();

            if (transparencyEnabled) {
                applyTransparencyToContent(selectionContent, canvas.getFillColor());
            }
            selection.setTransparent(transparencyEnabled);

            selection.setContent(selectionContent);
            originalSelectionLocation = new Point(selectionBounds.x, selectionBounds.y);
            clearSelectionOriginalLocation((transparencyEnabled ? canvas.getFillColor() : canvas.getCanvasBackground()));
            canvas.notifyClipboardStateChanged();
        } else {
            selectionManager.getSelection().setContent(null);
            originalSelectionLocation = null;
        }
    }

    private BufferedImage createSelectionImage() {
        BufferedImage selectionContent = new BufferedImage(
                selectionBounds.width, selectionBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = selectionContent.createGraphics();

        // Set the clip to the path (translated to image coordinates)
        GeneralPath translatedPath = new GeneralPath(currentPath);
        translatedPath.transform(AffineTransform.getTranslateInstance(-selectionBounds.x, -selectionBounds.y));
        g2d.setClip(translatedPath);

        // Draw the canvas content
        g2d.drawImage(canvas.getImage(), -selectionBounds.x, -selectionBounds.y, null);
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
        Selection selection = selectionManager.getSelection();

        if (!(selection instanceof PathSelection)) {
            return;
        }

        if (isDrawingPath) {
            currentPath.lineTo(worldDragPoint.x, worldDragPoint.y);
        } else if (isDragging) {
            updatePathLocation(worldDragPoint, (PathSelection)selection);
        }

        canvas.repaint();
    }

    private void updatePathLocation(Point worldDragPoint, PathSelection selection) {
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
    protected void clearSelectionOriginalLocation(Color color) {
        Selection selection = selectionManager.getSelection();
        if (!(selection instanceof PathSelection) || originalSelectionLocation == null) {
            return;
        }

        canvas.saveToUndoStack();
        Graphics2D g2d = canvas.getImage().createGraphics();
        g2d.setColor(color);

        GeneralPath originalPath = ((PathSelection) selection).getPath();
        if (originalPath != null) {
            g2d.fill(originalPath);
        }

        g2d.dispose();
    }

    @Override
    protected void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content) {
        Rectangle bounds = ((PathSelection) selection).getPath().getBounds();
        g2d.drawImage(content, bounds.x, bounds.y, null);
    }
}