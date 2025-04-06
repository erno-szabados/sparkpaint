package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.TextToolRenderer;
import com.esgdev.sparkpaint.ui.ToolChangeListener;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class TextTool implements DrawingTool, ToolChangeListener {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private String text = "Sample Text";
    private Font font = new Font("Arial", Font.PLAIN, 24);
    private Point previewPoint = null;

    // Add TextToolRenderer field
    private final TextToolRenderer renderer;

    public TextTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        canvas.addToolChangeListener(this);
        // Initialize the renderer
        this.renderer = new TextToolRenderer();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Update preview position
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // Check if there's a selection and we're inside it
        Selection selection = canvas.getSelection();
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            // Clear preview if outside selection
            clearPreview();
            return;
        }

        // Update preview position
        previewPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
        updatePreview();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        canvas.saveToUndoStack();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // No action needed for mouse dragged
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Get the point in the appropriate coordinate system
        Point drawPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get drawing context based on selection
        DrawContext drawContext = prepareDrawContext(selection, drawPoint);

        // Draw the text using the renderer
        renderer.drawText(drawContext.targetImage, drawContext.g2d, drawContext.position,
                text, font, canvas.getDrawingColor(), false);

        drawContext.g2d.dispose();

        // Clear preview
        clearPreview();
        canvas.repaint();
    }

    // Helper class for drawing context
    private static class DrawContext {
        final Graphics2D g2d;
        final BufferedImage targetImage;
        final Point position;

        DrawContext(Graphics2D g2d, BufferedImage targetImage, Point position) {
            this.g2d = g2d;
            this.targetImage = targetImage;
            this.position = position;
        }
    }

    private DrawContext prepareDrawContext(Selection selection, Point drawPoint) {
        Graphics2D g2d;
        Point adjustedPosition = drawPoint;
        BufferedImage targetImage;

        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
            selection.setModified(true);
            targetImage = selection.getContent();

            // Adjust coordinates relative to selection bounds
            Rectangle bounds = selection.getBounds();
            adjustedPosition = new Point(drawPoint.x - bounds.x, drawPoint.y - bounds.y);
        } else {
            targetImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) targetImage.getGraphics();
        }

        return new DrawContext(g2d, targetImage, adjustedPosition);
    }

    private void updatePreview() {
        if (previewPoint == null) return;

        // Create temporary canvas for preview
        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear the temp canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply rendering settings
        renderer.configureGraphics(g2d);

        // Apply selection clip if needed
        Selection selection = canvas.getSelection();
        applySelectionClip(g2d, selection);

        // Calculate adjusted draw point for selection
        Point adjustedPoint = previewPoint;
        if (selection != null && selection.hasOutline()) {
            // This point is already in world coordinates, so no need to adjust for selection
            adjustedPoint = new Point(previewPoint.x, previewPoint.y);
        }

        // Draw text preview using the renderer
        renderer.drawText(tempCanvas, g2d, adjustedPoint, text, font,
                canvas.getDrawingColor(), true);

        g2d.dispose();
        canvas.repaint();
    }

    private void clearPreview() {
        previewPoint = null;
        BufferedImage tempCanvas = canvas.getToolCanvas();
        if (tempCanvas != null) {
            Graphics2D g2d = tempCanvas.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            g2d.dispose();
            canvas.repaint();
        }
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // No action needed for mouse scrolled
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        return "Text tool selected";
    }

    public void setText(String text) {
        this.text = text;
        updatePreview();
    }

    public void setFont(Font font) {
        this.font = font;
        updatePreview();
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        // Forward to renderer
        renderer.setAntiAliasing(useAntiAliasing);
        updatePreview();
    }

    @Override
    public void onToolChanged(ToolManager.Tool newTool) {
        // If switching away from text tool, clear the preview
        if (newTool != ToolManager.Tool.TEXT) {
            clearPreview();
        }
    }
}