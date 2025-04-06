package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.LineToolRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LineTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private final List<Point> polylinePoints;
    private float curveTension = 0.5f; // Default tension (0.5 is standard Catmull-Rom)
    private LineMode mode = LineMode.SINGLE_LINE;

    // Add LineToolRenderer field
    private final LineToolRenderer renderer;

    public enum LineMode {
        SINGLE_LINE("Single Line"),
        POLYLINE("Polyline"),
        CURVE("Smooth Curve"),
        CLOSED_CURVE("Closed Curve"),
        FILLED_CURVE("Filled Curve");

        private final String displayName;

        LineMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public LineTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.polylinePoints = new ArrayList<>();
        // Initialize the renderer
        this.renderer = new LineToolRenderer();
    }

    public void setMode(LineMode mode) {
        this.mode = mode;
        // Reset points when changing modes
        resetPoints();
    }

    public LineMode getMode() {
        return mode;
    }

    public void setCurveTension(float tension) {
        this.curveTension = Math.max(0.0f, Math.min(1.0f, tension));
        // Forward to renderer
        renderer.setCurveTension(tension);
    }

    public float getCurveTension() {
        return curveTension;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ((mode == LineMode.POLYLINE || mode == LineMode.CURVE ||
                mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE) &&
                !polylinePoints.isEmpty()) {
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            boolean closePath = mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE;
            drawLinePreview(point, closePath);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Selection selection = canvas.getSelection();

        // Convert to world coordinates and check if we're in a selection
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't start drawing outside selection
        }

        Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

        // Handle based on mode and mouse button
        if (mode == LineMode.SINGLE_LINE) {
            // Single line mode - save start point
            startPoint = point;
            canvas.saveToUndoStack();
        } else if (mode == LineMode.POLYLINE ||
                mode == LineMode.CURVE ||
                mode == LineMode.CLOSED_CURVE ||
                mode == LineMode.FILLED_CURVE) {
            // Check if right-click
            if (SwingUtilities.isRightMouseButton(e)) {
                // If we have enough points, finalize the line
                if (polylinePoints.size() >= minRequiredPoints()) {
                    finalizeLine();
                } else {
                    // Not enough points, just reset/cancel
                    resetPoints();
                }
                return;
            }

            // First point - save to undo stack
            if (polylinePoints.isEmpty()) {
                canvas.saveToUndoStack();
            }

            // Add point to polyline/curve
            polylinePoints.add(point);

            // Update preview
            boolean closePath = mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE;
            drawLinePreview(point, closePath);
        }
    }

    // Helper method to determine minimum required points based on mode
    private int minRequiredPoints() {
        return (mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE) ? 3 : 2;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mode == LineMode.SINGLE_LINE) {
            if (startPoint == null) return;

            Selection selection = canvas.getSelection();

            // Get current point in appropriate coordinate system
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Create temporary canvas for preview
            BufferedImage tempCanvas = canvas.getToolCanvas();
            Graphics2D g2d = tempCanvas.createGraphics();

            // Clear the temp canvas
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);

            applySelectionClip(g2d, selection);

            // Delegate to renderer for preview drawing
            renderer.drawLine(tempCanvas, g2d, startPoint, point,
                    canvas.getDrawingColor(), canvas.getLineThickness());

            g2d.dispose();
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mode == LineMode.SINGLE_LINE) {
            if (startPoint == null) return;

            Selection selection = canvas.getSelection();

            // Get current point in appropriate coordinate system
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Determine target image and graphics context
            BufferedImage targetImage;
            Graphics2D g2d;

            if (selection != null && selection.hasOutline()) {
                // Get drawing graphics from the selection manager
                g2d = canvas.getDrawingGraphics();
                if (g2d == null) return; // Safety check

                targetImage = selection.getContent();
                if (targetImage == null) return; // Safety check

                selection.setModified(true);

                // Get selection bounds to adjust coordinates
                Rectangle bounds = selection.getBounds();

                // Adjust coordinates relative to the selection bounds
                Point selectionStartPoint = new Point(startPoint.x - bounds.x, startPoint.y - bounds.y);
                Point selectionEndPoint = new Point(point.x - bounds.x, point.y - bounds.y);

                // Use adjusted points for drawing in selection
                startPoint = selectionStartPoint;
                point = selectionEndPoint;
            } else {
                // Draw on current layer
                targetImage = canvas.getCurrentLayerImage();
                if (targetImage == null) return; // Safety check

                g2d = (Graphics2D) targetImage.getGraphics();
                if (g2d == null) return; // Safety check
            }

            // Set color based on mouse button
            Color drawColor;
            if (SwingUtilities.isLeftMouseButton(e)) {
                drawColor = canvas.getDrawingColor();
            } else if (SwingUtilities.isRightMouseButton(e)) {
                drawColor = canvas.getFillColor();
            } else {
                g2d.dispose();
                return;
            }

            // Delegate to renderer for final line drawing
            renderer.drawLine(targetImage, g2d, startPoint, point,
                    drawColor, canvas.getLineThickness());

            g2d.dispose();

            // Reset state
            startPoint = null;

            // Clear the temp canvas and repaint
            canvas.setToolCanvas(null);
            canvas.repaint();
        }
    }

    // In LineTool.java
    // Add a new method to get the current points for external drawing
    public List<Point> getControlPoints() {
        return new ArrayList<>(polylinePoints);
    }

    // Add a method to draw control points after zoom reset
    public void drawControlPointsOverlay(Graphics2D g2d, float zoomFactor) {
        if (polylinePoints.isEmpty()) return;

        // Delegate to renderer
        renderer.drawControlPoints(g2d, polylinePoints, zoomFactor, canvas.getLineThickness());
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // No action needed for mouse scroll
    }

    @Override
    public void setCursor() {
        canvas.setCursor(cursor);
    }

    @Override
    public String statusMessage() {
        return mode == LineMode.SINGLE_LINE ? "Line tool selected" : "Polyline tool selected";
    }

    // Setter for anti-aliasing
    public void setAntiAliasing(boolean useAntiAliasing) {
        // Forward to renderer
        renderer.setAntiAliasing(useAntiAliasing);
    }

    private void drawLinePreview(Point currentPoint, boolean closePath) {
        if (polylinePoints.isEmpty()) return;

        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply selection clip if needed
        Selection selection = canvas.getSelection();
        applySelectionClip(g2d, selection);

        // Delegate to renderer for preview drawing
        renderer.drawPreview(tempCanvas, mode, polylinePoints, currentPoint,
                canvas.getDrawingColor(), canvas.getLineThickness());

        g2d.dispose();
        canvas.repaint();
    }

    private void finalizeLine() {
        if (polylinePoints.size() < minRequiredPoints()) {
            resetPoints();
            return;
        }

        Selection selection = canvas.getSelection();
        BufferedImage targetImage = selection != null && selection.hasOutline()
                ? selection.getContent()
                : canvas.getCurrentLayerImage();

        // Early exit if we can't get a valid target image
        if (targetImage == null) {
            System.err.println("Target image is null in finalizeLine");
            resetPoints();
            return;
        }

        Graphics2D g2d;
        List<Point> adjustedPoints = new ArrayList<>(polylinePoints);

        // Close the path for closed curve or filled curve mode
        boolean isClosed = mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE;
        if (isClosed && adjustedPoints.size() > 2) {
            adjustedPoints.add(adjustedPoints.get(0));
        }

        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
            if (g2d == null) {
                System.err.println("Graphics context is null in finalizeLine");
                resetPoints();
                return;
            }

            selection.setModified(true);

            // Adjust points relative to selection
            Rectangle bounds = selection.getBounds();
            for (int i = 0; i < adjustedPoints.size(); i++) {
                Point p = adjustedPoints.get(i);
                adjustedPoints.set(i, new Point(p.x - bounds.x, p.y - bounds.y));
            }
        } else {
            g2d = (Graphics2D) targetImage.getGraphics();
            if (g2d == null) {
                System.err.println("Graphics context is null in finalizeLine");
                resetPoints();
                return;
            }
        }

        if (mode == LineMode.FILLED_CURVE) {
            // Delegate to renderer for filled curve
            renderer.drawFilledCurve(targetImage, g2d, adjustedPoints,
                    canvas.getFillColor(), canvas.getDrawingColor(), canvas.getLineThickness());
        } else if (mode == LineMode.POLYLINE) {
            // Delegate to renderer for polyline
            renderer.drawPolyline(targetImage, g2d, adjustedPoints,
                    canvas.getDrawingColor(), canvas.getLineThickness());
        } else {
            // Delegate to renderer for curve or closed curve
            renderer.drawCurve(targetImage, g2d, adjustedPoints, isClosed,
                    canvas.getDrawingColor(), canvas.getLineThickness());
        }

        g2d.dispose();

        // Reset and clean up
        resetPoints();
        canvas.setToolCanvas(null);
        canvas.repaint();
    }

    // Reset polyline points
    private void resetPoints() {
        polylinePoints.clear();
        canvas.setToolCanvas(null);
        canvas.repaint();
    }
}