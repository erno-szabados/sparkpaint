package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.DrawingTool;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * MouseAdapter for handling mouse events on the canvas.
 * Relies on the current tool to handle events.
 */
public class CanvasMouseAdapter extends MouseAdapter {
    private final DrawingCanvas canvas;

    public CanvasMouseAdapter(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        DrawingTool tool = canvas.getActiveTool();
        if (tool != null) {
            tool.mouseMoved(e);
        }
        canvas.setCursorShapeCenter(e.getPoint());
        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        DrawingTool tool = canvas.getActiveTool();
        if (tool != null) {
            tool.mouseDragged(e);
        }
        canvas.setCursorShapeCenter(e.getPoint());

        canvas.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {

        DrawingTool tool = canvas.getActiveTool();
        if (tool != null) {
            tool.setCursor();
            tool.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        DrawingTool tool = canvas.getActiveTool();
        if (tool != null) {
            tool.mouseReleased(e);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // Apply zoom
        float[] zoomLevels = {1.0f, 2.0f, 4.0f, 8.0f, 12.0f};
        double zoomFactor = canvas.getZoomFactor();

        if (e.getWheelRotation() < 0) {
            for (float zoomLevel : zoomLevels) {
                if (zoomFactor < zoomLevel) {
                    canvas.setZoomFactor(zoomLevel);
                    break;
                }
            }
        } else {
            for (int i = zoomLevels.length - 1; i >= 0; i--) {
                if (zoomFactor > zoomLevels[i]) {
                    canvas.setZoomFactor(zoomLevels[i]);
                    break;
                }
            }
        }

        canvas.updateCanvasAfterZoom();
        DrawingTool tool = canvas.getActiveTool();
        if (tool != null) {
            tool.mouseScrolled(e);
        }

        canvas.setCursorShapeCenter(e.getPoint());
        canvas.revalidate();
        canvas.repaint();
    }
}
