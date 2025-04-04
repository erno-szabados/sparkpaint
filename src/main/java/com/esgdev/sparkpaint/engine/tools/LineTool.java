package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LineTool implements DrawingTool {
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Point startPoint;
    private boolean useAntiAliasing = true;
    private final List<Point> polylinePoints;
    private float curveTension = 0.5f; // Default tension (0.5 is standard Catmull-Rom)
    private LineMode mode = LineMode.SINGLE_LINE;

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

            // Apply rendering settings
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            applySelectionClip(g2d, selection);

            // Get drawing color and check if it's transparent
            Color drawingColor = canvas.getDrawingColor();
            boolean isTransparentLine = drawingColor.getAlpha() == 0;
            float lineThickness = canvas.getLineThickness();

            if (isTransparentLine) {
                // Define dash pattern for transparent preview
                float[] dashPattern = {8.0f, 8.0f};

                // Draw white line first (wider)
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(lineThickness + 2,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dashPattern, 0.0f));
                g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);

                // Draw black line on top (narrower, offset dash pattern)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(lineThickness,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dashPattern, dashPattern[0]));
                g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
            } else {
                // Non-transparent color - draw solid line with actual drawing color
                g2d.setColor(drawingColor);
                g2d.setStroke(new BasicStroke(lineThickness));
                g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
            }

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
            if (g2d.getColor().getAlpha() == 0) {
                drawTransparentLine(targetImage, g2d, startPoint, point, canvas.getLineThickness());
            } else {
                // Original drawing code
                g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
            }
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

        // Calculate proper size based on line thickness but not affected by zoom
        int markerSize = Math.max(6, Math.round(canvas.getLineThickness()) + 2);
        int halfMarker = markerSize / 2;

        // Draw the control points with the original scale
        g2d.setColor(Color.WHITE);
        for (Point p : polylinePoints) {
            int x = (int) (p.x * zoomFactor);
            int y = (int) (p.y * zoomFactor);
            g2d.fillOval(x - halfMarker, y - halfMarker, markerSize, markerSize);
        }

        g2d.setColor(Color.BLACK);
        for (Point p : polylinePoints) {
            int x = (int) (p.x * zoomFactor);
            int y = (int) (p.y * zoomFactor);
            g2d.drawOval(x - halfMarker, y - halfMarker, markerSize, markerSize);
        }

        // Draw dotted connecting lines for curve modes
        if (mode == LineMode.CURVE || mode == LineMode.CLOSED_CURVE || mode == LineMode.FILLED_CURVE) {
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{3.0f}, 0.0f));
            g2d.setColor(new Color(100, 100, 100, 150)); // Semi-transparent gray

            // Draw control polygon
            for (int i = 0; i < polylinePoints.size() - 1; i++) {
                Point p1 = polylinePoints.get(i);
                Point p2 = polylinePoints.get(i + 1);
                g2d.drawLine(
                        (int) (p1.x * zoomFactor), (int) (p1.y * zoomFactor),
                        (int) (p2.x * zoomFactor), (int) (p2.y * zoomFactor)
                );
            }
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
        {
            Point p0 = controlPoints.get(0);
            Point p1 = controlPoints.get(1);
            // Reflect p1 around p0
            int dx = p0.x - p1.x;
            int dy = p0.y - p1.y;
            points.add(new Point(p0.x + dx, p0.y + dy));
        }

        // Add all control points
        points.addAll(controlPoints);

        // Add last control point as phantom end point
        Point pn = controlPoints.get(controlPoints.size() - 1);
        Point pn_1 = controlPoints.get(controlPoints.size() - 2);
        // Reflect pn_1 around pn
        int dx = pn.x - pn_1.x;
        int dy = pn.y - pn_1.y;
        points.add(new Point(pn.x + dx, pn.y + dy));

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

    private void drawLinePreview(Point currentPoint, boolean closePath) {
        if (polylinePoints.isEmpty()) return;

        BufferedImage tempCanvas = canvas.getToolCanvas();
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply settings
        float lineThickness = canvas.getLineThickness();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Create temporary list including current point
        List<Point> tempPoints = new ArrayList<>(polylinePoints);
        tempPoints.add(currentPoint);

        // Close path if needed
        if (closePath && tempPoints.size() > 2) {
            tempPoints.add(tempPoints.get(0));
        }

        // Get drawing and fill colors
        Color drawingColor = canvas.getDrawingColor();
        boolean isTransparentLine = drawingColor.getAlpha() == 0;
        Color fillColor = canvas.getFillColor();
        boolean isTransparentFill = fillColor.getAlpha() == 0;

        // Define dash pattern for transparent lines
        float[] dashPattern = {8.0f, 8.0f};

        BasicStroke basicStroke = new BasicStroke(lineThickness + 2,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 0.0f);
        if (mode == LineMode.FILLED_CURVE) {
            // Calculate curve points
            List<Point> curvePoints = calculateCurvePoints(tempPoints);

            if (curvePoints.size() > 1) {
                // Create a path for the filled shape
                Path2D path = new Path2D.Float();
                path.moveTo(curvePoints.get(0).x, curvePoints.get(0).y);

                for (int i = 1; i < curvePoints.size(); i++) {
                    path.lineTo(curvePoints.get(i).x, curvePoints.get(i).y);
                }

                path.closePath();

                // Fill with semi-transparent fill color for preview
                if (isTransparentFill) {
                    g2d.setColor(new Color(255, 0, 0, 32)); // Visual indicator for transparent fill
                } else {
                    g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(),
                            fillColor.getBlue(), 128)); // Semi-transparent preview
                }
                g2d.fill(path);

                // Draw outline
                if (isTransparentLine) {
                    // Two-color dashed outline for transparent color
                    // First pass: White dashes
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(basicStroke);
                    g2d.draw(path);

                    // Second pass: Black dashes with offset
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(lineThickness,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, dashPattern[0]));
                    g2d.draw(path);
                } else {
                    // Solid outline with actual drawing color for non-transparent
                    g2d.setColor(drawingColor);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.draw(path);
                }
            }
        } else if (mode == LineMode.POLYLINE) {
            // Draw straight line segments
            for (int i = 0; i < tempPoints.size() - 1; i++) {
                Point p1 = tempPoints.get(i);
                Point p2 = tempPoints.get(i + 1);

                if (isTransparentLine) {
                    // Two-color dashed lines for transparent
                    // First pass: White dashes
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(basicStroke);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

                    // Second pass: Black dashes with offset
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(lineThickness,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, dashPattern[0]));
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                } else {
                    // Solid line with actual drawing color
                    g2d.setColor(drawingColor);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        } else {
            // Calculate curve points for CURVE or CLOSED_CURVE
            List<Point> curvePoints = calculateCurvePoints(tempPoints);

            if (curvePoints.size() > 1) {
                // Create a path for the curve (for consistent rendering)
                Path2D path = new Path2D.Float();
                path.moveTo(curvePoints.get(0).x, curvePoints.get(0).y);

                for (int i = 1; i < curvePoints.size(); i++) {
                    path.lineTo(curvePoints.get(i).x, curvePoints.get(i).y);
                }

                if (isTransparentLine) {
                    // First pass: White dashes
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(basicStroke);
                    g2d.draw(path);

                    // Second pass: Black dashes with offset
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(lineThickness,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, dashPattern, dashPattern[0]));
                    g2d.draw(path);
                } else {
                    // Solid line with actual drawing color
                    g2d.setColor(drawingColor);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.draw(path);
                }
            }
        }

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

        // Apply settings
        g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        if (mode == LineMode.FILLED_CURVE) {
            // Create a path for the filled shape
            Path2D path = new Path2D.Float();

            // Use curve points for smoother outline
            List<Point> curvePoints = calculateCurvePoints(adjustedPoints);

            if (curvePoints.size() > 1) {
                path.moveTo(curvePoints.get(0).x, curvePoints.get(0).y);

                for (int i = 1; i < curvePoints.size(); i++) {
                    path.lineTo(curvePoints.get(i).x, curvePoints.get(i).y);
                }

                path.closePath();

                // Fill with fill color
                if (canvas.getFillColor().getAlpha() == 0) {
                    fillTransparentPath(targetImage, g2d, path);
                } else {
                    // Original fill code
                    g2d.setColor(canvas.getFillColor());
                    g2d.fill(path);
                }

                // Draw outline with drawing color
                Color drawingColor = canvas.getDrawingColor();
                if (drawingColor.getAlpha() == 0) {
                    // Handle transparent outline
                    drawTransparentPath(targetImage, g2d, path, canvas.getLineThickness());
                } else {
                    // Original outline code
                    g2d.setColor(drawingColor);
                    g2d.draw(path);
                }
            }
        } else if (mode == LineMode.POLYLINE) {
            // Check if using transparent color
            if (canvas.getDrawingColor().getAlpha() == 0) {
                // Draw transparent lines for each segment
                for (int i = 0; i < adjustedPoints.size() - 1; i++) {
                    Point p1 = adjustedPoints.get(i);
                    Point p2 = adjustedPoints.get(i + 1);
                    drawTransparentLine(targetImage, g2d, p1, p2, canvas.getLineThickness());
                }
            } else {
                // Original polyline drawing code
                g2d.setColor(canvas.getDrawingColor());
                for (int i = 0; i < adjustedPoints.size() - 1; i++) {
                    Point p1 = adjustedPoints.get(i);
                    Point p2 = adjustedPoints.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        } else {
            // Curve or closed curve mode
            List<Point> curvePoints = calculateCurvePoints(adjustedPoints);

            if (curvePoints.size() > 1) {
                // Check if using transparent color
                if (canvas.getDrawingColor().getAlpha() == 0) {
                    // Draw transparent lines for each curve segment
                    for (int i = 0; i < curvePoints.size() - 1; i++) {
                        Point p1 = curvePoints.get(i);
                        Point p2 = curvePoints.get(i + 1);
                        drawTransparentLine(targetImage, g2d, p1, p2, canvas.getLineThickness());
                    }
                } else {
                    // Original curve drawing code
                    g2d.setColor(canvas.getDrawingColor());
                    for (int i = 0; i < curvePoints.size() - 1; i++) {
                        Point p1 = curvePoints.get(i);
                        Point p2 = curvePoints.get(i + 1);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
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

    /**
     * Special handling for transparent drawing - clears pixels along the line path
     */
    private void drawTransparentLine(BufferedImage image, Graphics2D g2d, Point p1, Point p2, float lineThickness) {
        // Check for null parameters
        if (image == null || g2d == null || p1 == null || p2 == null) {
            System.err.println("Null parameter in drawTransparentLine: " +
                    (image == null ? "image " : "") +
                    (g2d == null ? "g2d " : "") +
                    (p1 == null ? "p1 " : "") +
                    (p2 == null ? "p2" : ""));
            return;
        }

        // Check bounds
        if (p1.x < 0 || p1.y < 0 || p2.x < 0 || p2.y < 0 ||
                p1.x >= image.getWidth() || p1.y >= image.getHeight() ||
                p2.x >= image.getWidth() || p2.y >= image.getHeight()) {
            // Points are out of bounds, but we could still proceed with clipping
            // Just log a warning
            System.err.println("Warning: Points out of bounds in drawTransparentLine");
        }

        try {
            // Create a temporary mask image for the line
            BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Set up the mask with the same stroke settings
            maskG2d.setStroke(new BasicStroke(lineThickness));
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Draw white line on mask
            maskG2d.setColor(Color.WHITE);
            maskG2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            maskG2d.dispose();

            // Calculate bounds of affected area for efficiency
            int lineWidth = (int) Math.ceil(lineThickness);
            int minX = Math.max(0, Math.min(p1.x, p2.x) - lineWidth);
            int minY = Math.max(0, Math.min(p1.y, p2.y) - lineWidth);
            int maxX = Math.min(image.getWidth(), Math.max(p1.x, p2.x) + lineWidth);
            int maxY = Math.min(image.getHeight(), Math.max(p1.y, p2.y) + lineWidth);

            // Apply transparency to pixels where the mask is non-zero
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Check if this pixel is within clip region
                    Shape clip = g2d.getClip();
                    if (clip == null || clip.contains(x, y)) {
                        int maskRGB = maskImage.getRGB(x, y);
                        // Only process pixels where the mask is non-zero
                        if ((maskRGB & 0xFF000000) != 0) {
                            // Set full transparency (alpha = 0)
                            int newRGB = image.getRGB(x, y) & 0x00FFFFFF;
                            image.setRGB(x, y, newRGB);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in drawTransparentLine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Special handling for transparent path outlines - clears pixels along the path outline
     */
    private void drawTransparentPath(BufferedImage image, Graphics2D g2d, Path2D path, float lineThickness) {
        // Check for null parameters
        if (image == null || g2d == null || path == null) {
            System.err.println("Null parameter in drawTransparentPath: " +
                    (image == null ? "image " : "") +
                    (g2d == null ? "g2d " : "") +
                    (path == null ? "path" : ""));
            return;
        }

        try {
            // Create a temporary mask image for the path outline
            BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Set up the mask with the same stroke settings
            maskG2d.setStroke(new BasicStroke(lineThickness));
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Draw white path outline on mask
            maskG2d.setColor(Color.WHITE);
            maskG2d.draw(path);
            maskG2d.dispose();

            // Get path bounds
            Rectangle bounds = path.getBounds();
            int padding = (int) Math.ceil(lineThickness);
            int minX = Math.max(0, bounds.x - padding);
            int minY = Math.max(0, bounds.y - padding);
            int maxX = Math.min(image.getWidth(), bounds.x + bounds.width + padding);
            int maxY = Math.min(image.getHeight(), bounds.y + bounds.height + padding);

            // Apply transparency to pixels where the mask is non-zero
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Check if this pixel is within clip region
                    if (g2d.getClip() == null || g2d.getClip().contains(x, y)) {
                        int maskRGB = maskImage.getRGB(x, y);
                        // Only process pixels where the mask is non-zero
                        if ((maskRGB & 0xFF000000) != 0) {
                            // Set full transparency (alpha = 0)
                            int newRGB = image.getRGB(x, y) & 0x00FFFFFF;
                            image.setRGB(x, y, newRGB);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in drawTransparentPath: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Special handling for transparent fill - clears pixels inside the path
     */
    private void fillTransparentPath(BufferedImage image, Graphics2D g2d, Path2D path) {
        // Check for null parameters
        if (image == null || g2d == null || path == null) {
            System.err.println("Null parameter in fillTransparentPath: " +
                    (image == null ? "image " : "") +
                    (g2d == null ? "g2d " : "") +
                    (path == null ? "path" : ""));
            return;
        }

        try {
            // Create a temporary mask image for the path
            BufferedImage maskImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = maskImage.createGraphics();

            // Set up the mask with same settings
            maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Fill the path with white on the mask
            maskG2d.setColor(Color.WHITE);
            maskG2d.fill(path);
            maskG2d.dispose();

            // Get path bounds
            Rectangle bounds = path.getBounds();
            int minX = Math.max(0, bounds.x);
            int minY = Math.max(0, bounds.y);
            int maxX = Math.min(image.getWidth(), bounds.x + bounds.width);
            int maxY = Math.min(image.getHeight(), bounds.y + bounds.height);

            // Apply transparency to pixels where the mask is non-zero
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    // Check if this pixel is within clip region
                    if (g2d.getClip() == null || g2d.getClip().contains(x, y)) {
                        int maskRGB = maskImage.getRGB(x, y);
                        // Only process pixels where the mask is non-zero
                        if ((maskRGB & 0xFF000000) != 0) {
                            // Set full transparency (alpha = 0)
                            int newRGB = image.getRGB(x, y) & 0x00FFFFFF;
                            image.setRGB(x, y, newRGB);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in fillTransparentPath: " + e.getMessage());
            e.printStackTrace();
        }
    }
}