package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

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
    private boolean useAntiAliasing = true;
    private List<Point> polylinePoints;
    private float curveTension = 0.5f; // Default tension (0.5 is standard Catmull-Rom)
    private LineMode mode = LineMode.SINGLE_LINE;

    public enum LineMode {
        SINGLE_LINE("Single Line"),
        POLYLINE("Polyline"),
        CURVE("Smooth Curve"),
        CLOSED_CURVE("Closed Curve");

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
    }

    public float getCurveTension() {
        return curveTension;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ((mode == LineMode.POLYLINE || mode == LineMode.CURVE || mode == LineMode.CLOSED_CURVE) && !polylinePoints.isEmpty()) {
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            if (mode == LineMode.POLYLINE) {
                drawPolylinePreview(point);
            } else {
                drawCurvePreview(point);
            }
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
        } else if (mode == LineMode.POLYLINE || mode == LineMode.CURVE || mode == LineMode.CLOSED_CURVE) {
            // Check if right-click to complete polyline/curve
            if (SwingUtilities.isRightMouseButton(e)) {
                if (polylinePoints.size() >= 2) {
                    // Complete the polyline/curve
                    if (mode == LineMode.POLYLINE) {
                        finalizePolyline();
                    } else {
                        finalizeCurve();
                    }
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
            if (mode == LineMode.POLYLINE) {
                drawPolylinePreview(point);
            } else {
                drawCurvePreview(point);
            }
        }
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

            // Apply rendering settings
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Set color based on mouse button
            if (SwingUtilities.isLeftMouseButton(e)) {
                g2d.setColor(canvas.getDrawingColor());
            } else if (SwingUtilities.isRightMouseButton(e)) {
                g2d.setColor(canvas.getFillColor());
            }

            applySelectionClip(g2d, selection);

            // Draw the preview line
            g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
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

            // Get appropriate graphics context for drawing
            Graphics2D g2d;
            if (selection != null && selection.hasOutline()) {
                // Get drawing graphics from the selection manager
                g2d = canvas.getDrawingGraphics();
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
                BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
                g2d = (Graphics2D) currentLayerImage.getGraphics();
            }

            // Apply rendering settings
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Set color based on mouse button
            if (SwingUtilities.isLeftMouseButton(e)) {
                g2d.setColor(canvas.getDrawingColor());
            } else if (SwingUtilities.isRightMouseButton(e)) {
                g2d.setColor(canvas.getFillColor());
            }

            // Draw the final line
            g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
            g2d.dispose();

            // Reset state
            startPoint = null;

            // Clear the temp canvas and repaint
            canvas.setToolCanvas(null);
            canvas.repaint();
        }
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
        this.useAntiAliasing = useAntiAliasing;
    }

    // Method to calculate points along a Catmull-Rom spline
    private List<Point> calculateCurvePoints(List<Point> controlPoints) {
        if (controlPoints.size() < 2) {
            return new ArrayList<>(controlPoints);
        }

        List<Point> curvePoints = new ArrayList<>();

        // Create phantom points at the ends if needed
        List<Point> points = new ArrayList<>();

        // Add first control point as phantom start point
        if (controlPoints.size() >= 2) {
            Point p0 = controlPoints.get(0);
            Point p1 = controlPoints.get(1);
            // Reflect p1 around p0
            int dx = p0.x - p1.x;
            int dy = p0.y - p1.y;
            points.add(new Point(p0.x + dx, p0.y + dy));
        } else {
            points.add(controlPoints.get(0));
        }

        // Add all control points
        points.addAll(controlPoints);

        // Add last control point as phantom end point
        if (controlPoints.size() >= 2) {
            Point pn = controlPoints.get(controlPoints.size() - 1);
            Point pn_1 = controlPoints.get(controlPoints.size() - 2);
            // Reflect pn_1 around pn
            int dx = pn.x - pn_1.x;
            int dy = pn.y - pn_1.y;
            points.add(new Point(pn.x + dx, pn.y + dy));
        } else {
            points.add(controlPoints.get(controlPoints.size() - 1));
        }

        // Sample curve segments
        int segments = 20; // Number of line segments to draw between control points

        // Go through all actual control points (skipping phantom ones)
        for (int i = 1; i < points.size() - 2; i++) {
            Point p0 = points.get(i - 1);
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            Point p3 = points.get(i + 2);

            // Add the starting point of this segment
            if (i == 1) {
                curvePoints.add(p1);
            }

            // Calculate intermediate curve points
            for (int j = 1; j <= segments; j++) {
                float t = j / (float) segments;
                Point pt = interpolateCatmullRom(p0, p1, p2, p3, t, curveTension);
                curvePoints.add(pt);
            }
        }

        return curvePoints;
    }

    private Point interpolateCatmullRom(Point p0, Point p1, Point p2, Point p3, float t, float tension) {
        // Convert tension from 0-1 range to alpha value (0-1)
        // Where 0 is a uniform Catmull-Rom spline, 0.5 is centripetal, 1.0 is chordal
        float alpha = 1.0f - tension;

        // Catmull-Rom basis matrix coefficients
        float t2 = t * t;
        float t3 = t2 * t;

        float b0 = -alpha * t + 2 * alpha * t2 - alpha * t3;
        float b1 = 1 + (alpha - 3) * t2 + (2 - alpha) * t3;
        float b2 = alpha * t + (3 - 2 * alpha) * t2 + (alpha - 2) * t3;
        float b3 = -alpha * t2 + alpha * t3;

        // Calculate interpolated point
        int x = Math.round(
                b0 * p0.x +
                        b1 * p1.x +
                        b2 * p2.x +
                        b3 * p3.x
        );

        int y = Math.round(
                b0 * p0.y +
                        b1 * p1.y +
                        b2 * p2.y +
                        b3 * p3.y
        );

        return new Point(x, y);
    }

    // Method to draw curve preview
    private void drawCurvePreview(Point currentPoint) {
        if (polylinePoints.isEmpty()) return;

        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply settings
        float lineThickness = canvas.getLineThickness();
        g2d.setStroke(new BasicStroke(lineThickness));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(canvas.getDrawingColor());

        // Create temporary list including current point
        List<Point> tempPoints = new ArrayList<>(polylinePoints);
        tempPoints.add(currentPoint);

        // For closed curve mode, add the first point to the end to close the shape
        if (mode == LineMode.CLOSED_CURVE && tempPoints.size() > 2) {
            tempPoints.add(tempPoints.get(0));
        }

        // Calculate and draw the curve
        List<Point> curvePoints = calculateCurvePoints(tempPoints);
        if (curvePoints.size() > 1) {
            for (int i = 0; i < curvePoints.size() - 1; i++) {
                Point p1 = curvePoints.get(i);
                Point p2 = curvePoints.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // Determine point marker size based on line thickness
        int markerSize = Math.max(6, Math.round(lineThickness) + 2);
        int halfMarker = markerSize / 2;

        // Add markers for control points
        g2d.setColor(Color.WHITE);
        for (Point p : polylinePoints) {
            g2d.fillOval(p.x - halfMarker, p.y - halfMarker, markerSize, markerSize);
        }
        g2d.setColor(Color.BLACK);
        for (Point p : polylinePoints) {
            g2d.drawOval(p.x - halfMarker, p.y - halfMarker, markerSize, markerSize);
        }

        // Draw dotted line connecting control points
        g2d.setStroke(new BasicStroke(1.0f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, new float[]{3.0f}, 0.0f));
        g2d.setColor(new Color(100, 100, 100, 150)); // Semi-transparent gray

        // Draw control polygon
        for (int i = 0; i < tempPoints.size() - 1; i++) {
            Point p1 = tempPoints.get(i);
            Point p2 = tempPoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        g2d.dispose();
        canvas.repaint();
    }

    // Method to finalize curve drawing
    private void finalizeCurve() {
        if (polylinePoints.size() < 2) {
            resetPoints();
            return;
        }

        Selection selection = canvas.getSelection();

        // Get appropriate graphics context
        Graphics2D g2d;
        List<Point> adjustedPoints = new ArrayList<>(polylinePoints);

        // For closed curve mode, add the first point to the end to close the shape
        if (mode == LineMode.CLOSED_CURVE && adjustedPoints.size() > 2) {
            adjustedPoints.add(adjustedPoints.get(0));
        }

        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
            selection.setModified(true);

            // Adjust points relative to selection
            Rectangle bounds = selection.getBounds();
            for (int i = 0; i < adjustedPoints.size(); i++) {
                Point p = adjustedPoints.get(i);
                adjustedPoints.set(i, new Point(p.x - bounds.x, p.y - bounds.y));
            }
        } else {
            BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) currentLayerImage.getGraphics();
        }

        // Apply settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(canvas.getDrawingColor());

        // Calculate and draw the final curve
        List<Point> curvePoints = calculateCurvePoints(adjustedPoints);
        if (curvePoints.size() > 1) {
            for (int i = 0; i < curvePoints.size() - 1; i++) {
                Point p1 = curvePoints.get(i);
                Point p2 = curvePoints.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        g2d.dispose();

        // Reset and clean up
        resetPoints();
        canvas.setToolCanvas(null);
        canvas.repaint();
    }

    private void drawPolylinePreview(Point currentPoint) {
        if (polylinePoints.isEmpty()) return;

        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply settings
        float lineThickness = canvas.getLineThickness();
        g2d.setStroke(new BasicStroke(lineThickness));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(canvas.getDrawingColor());

        // Draw all completed segments
        for (int i = 0; i < polylinePoints.size() - 1; i++) {
            Point p1 = polylinePoints.get(i);
            Point p2 = polylinePoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Draw preview segment from last point to current mouse position with dash pattern scaled to line thickness
        Point lastPoint = polylinePoints.get(polylinePoints.size() - 1);

        // Scale the dash pattern based on line thickness to prevent intermeshing
        float dashSize = Math.max(5, lineThickness * 2);
        float[] dashPattern = {dashSize, dashSize};

        g2d.setStroke(new BasicStroke(
                lineThickness,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                0,
                dashPattern,
                0
        ));

        g2d.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);

        // Determine point marker size based on line thickness
        int markerSize = Math.max(6, Math.round(lineThickness) + 2);
        int halfMarker = markerSize / 2;

        // Add markers for points
        g2d.setColor(Color.WHITE);
        for (Point p : polylinePoints) {
            g2d.fillOval(p.x - halfMarker, p.y - halfMarker, markerSize, markerSize);
        }
        g2d.setColor(Color.BLACK);
        for (Point p : polylinePoints) {
            g2d.drawOval(p.x - halfMarker, p.y - halfMarker, markerSize, markerSize);
        }

        g2d.dispose();
        canvas.repaint();
    }

    // Finalize and draw the polyline to the actual canvas
    private void finalizePolyline() {
        Selection selection = canvas.getSelection();

        // Get appropriate graphics context
        Graphics2D g2d;
        List<Point> adjustedPoints = new ArrayList<>(polylinePoints);

        if (selection != null && selection.hasOutline()) {
            g2d = canvas.getDrawingGraphics();
            selection.setModified(true);

            // Adjust points relative to selection
            Rectangle bounds = selection.getBounds();
            for (int i = 0; i < adjustedPoints.size(); i++) {
                Point p = adjustedPoints.get(i);
                adjustedPoints.set(i, new Point(p.x - bounds.x, p.y - bounds.y));
            }
        } else {
            BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
            g2d = (Graphics2D) currentLayerImage.getGraphics();
        }

        // Apply settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setColor(canvas.getDrawingColor());

        // Draw all line segments
        for (int i = 0; i < adjustedPoints.size() - 1; i++) {
            Point p1 = adjustedPoints.get(i);
            Point p2 = adjustedPoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
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