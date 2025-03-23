package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class TextTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private String text = "Sample Text";
    private Font font = new Font("Arial", Font.PLAIN, 24);
    private boolean useAntiAliasing = true;

    public TextTool(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
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
        //SelectionManager selectionManager = canvas.getSelectionManager();
        Selection selection = canvas.getSelection();

        // Convert screen point to world coordinates
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection, only proceed if clicking inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't draw outside selection when one exists
        }

        // Get the point in the appropriate coordinate system
        Point drawPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Get appropriate graphics context for drawing
        Graphics2D g2d;
        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
        } else {
            // Draw on current layer
            g2d = canvas.getLayerManager().getCurrentLayerImage().createGraphics();
        }

        // Configure and draw text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setFont(font);
        g2d.setColor(canvas.getDrawingColor());
        g2d.drawString(text, drawPoint.x, drawPoint.y);
        g2d.dispose();

        canvas.repaint();
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
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }
}