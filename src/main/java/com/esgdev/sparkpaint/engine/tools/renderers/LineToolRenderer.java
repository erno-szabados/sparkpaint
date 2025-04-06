package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.LineTool;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * LineToolRenderer handles the actual rendering of various line types for the LineTool.
 * This class is responsible for transparent drawing, path drawing, and curve calculations.
 */
public class LineToolRenderer {

    private boolean useAntiAliasing = true;
    private float curveTension = 0.5f;

    public LineToolRenderer(DrawingCanvas canvas) {
        // Canvas reference not needed for now but included for consistency with BrushToolRenderer
    }

    /**
     * Draws a single line between two points.
     */
    public void drawLine(BufferedImage targetImage, Graphics2D g2d, Point p1, Point p2, Color color, float lineThickness) {
        if (color.getAlpha() == 0) {
            drawTransparentLine(targetImage, g2d, p1, p2, lineThickness);
        } else {
            configureGraphics(g2d, color, lineThickness);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    /**
     * Draws a polyline using the provided points.
     */
    public void drawPolyline(BufferedImage targetImage, Graphics2D g2d, List<Point> points, Color color, float lineThickness) {
        if (points.size() < 2) return;

        if (color.getAlpha() == 0) {
            // Draw transparent lines for each segment
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                drawTransparentLine(targetImage, g2d, p1, p2, lineThickness);
            }
        } else {
            configureGraphics(g2d, color, lineThickness);
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    /**
     * Draws a curve using the provided control points.
     */
    public void drawCurve(BufferedImage targetImage, Graphics2D g2d, List<Point> controlPoints,
                          boolean closed, Color color, float lineThickness) {
        if (controlPoints.size() < 2) return;

        List<Point> curvePoints = calculateCurvePoints(controlPoints);
        if (curvePoints.size() <= 1) return;

        // Create a path for consistent rendering
        Path2D path = new Path2D.Float();
        path.moveTo(curvePoints.get(0).x, curvePoints.get(0).y);

        for (int i = 1; i < curvePoints.size(); i++) {
            path.lineTo(curvePoints.get(i).x, curvePoints.get(i).y);
        }

        if (closed) {
            path.closePath();
        }

        if (color.getAlpha() == 0) {
            drawTransparentPath(targetImage, g2d, path, lineThickness);
        } else {
            configureGraphics(g2d, color, lineThickness);
            g2d.draw(path);
        }
    }

    /**
     * Draws a filled curve using the provided control points.
     */
    public void drawFilledCurve(BufferedImage targetImage, Graphics2D g2d, List<Point> controlPoints,
                                Color fillColor, Color outlineColor, float lineThickness) {
        if (controlPoints.size() < 3) return;

        List<Point> curvePoints = calculateCurvePoints(controlPoints);
        if (curvePoints.size() <= 2) return;

        // Create a path for the shape
        Path2D path = new Path2D.Float();
        path.moveTo(curvePoints.get(0).x, curvePoints.get(0).y);

        for (int i = 1; i < curvePoints.size(); i++) {
            path.lineTo(curvePoints.get(i).x, curvePoints.get(i).y);
        }

        path.closePath();

        // Fill the path
        if (fillColor.getAlpha() == 0) {
            fillTransparentPath(targetImage, g2d, path);
        } else {
            configureGraphics(g2d, fillColor, lineThickness);
            g2d.fill(path);
        }

        // Draw the outline
        if (outlineColor.getAlpha() == 0) {
            drawTransparentPath(targetImage, g2d, path, lineThickness);
        } else {
            configureGraphics(g2d, outlineColor, lineThickness);
            g2d.draw(path);
        }
    }

    /**
     * Draws a preview of a line/curve on a temporary canvas.
     */
    public void drawPreview(BufferedImage tempCanvas, LineTool.LineMode mode, List<Point> points,
                            Point currentPoint, Color color, float lineThickness) {
        Graphics2D g2d = tempCanvas.createGraphics();

        // Clear the temp canvas
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);

        // Apply rendering settings
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Boolean for closed modes
        boolean closePath = mode == LineTool.LineMode.CLOSED_CURVE || mode == LineTool.LineMode.FILLED_CURVE;

        // Create temporary list including the current point
        List<Point> tempPoints = new java.util.ArrayList<>(points);
        tempPoints.add(currentPoint);

        // Close path if needed
        if (closePath && tempPoints.size() > 2) {
            tempPoints.add(tempPoints.get(0));
        }

        // Draw preview based on mode
        boolean isTransparentLine = color.getAlpha() == 0;
        float[] dashPattern = {8.0f, 8.0f};

        BasicStroke basicStroke = new BasicStroke(lineThickness + 2,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dashPattern, 0.0f);
        if (mode == LineTool.LineMode.SINGLE_LINE) {
            if (tempPoints.size() < 2) return;

            Point startPoint = tempPoints.get(0);
            Point endPoint = tempPoints.get(1);

            if (isTransparentLine) {
                // Define dash pattern for transparent preview
                // Draw white line first (wider)
                g2d.setColor(Color.WHITE);
                g2d.setStroke(basicStroke);
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);

                // Draw black line on top (narrower, offset dash pattern)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(lineThickness,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dashPattern, dashPattern[0]));
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            } else {
                // Non-transparent color - draw solid line
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(lineThickness));
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            }
        } else if (mode == LineTool.LineMode.POLYLINE) {
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
                    g2d.setColor(color);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        } else if (mode == LineTool.LineMode.FILLED_CURVE) {
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
                if (color.getAlpha() == 0) {
                    g2d.setColor(new Color(255, 0, 0, 32)); // Visual indicator for transparent fill
                } else {
                    g2d.setColor(new Color(color.getRed(), color.getGreen(),
                            color.getBlue(), 128)); // Semi-transparent preview
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
                    // Solid outline with actual drawing color
                    g2d.setColor(color);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.draw(path);
                }
            }
        } else {
            // CURVE or CLOSED_CURVE mode
            List<Point> curvePoints = calculateCurvePoints(tempPoints);

            if (curvePoints.size() > 1) {
                // Create a path for the curve
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
                    g2d.setColor(color);
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.draw(path);
                }
            }
        }

        g2d.dispose();
    }

    /**
     * Configure the graphics context with color, stroke and rendering hints.
     */
    private void configureGraphics(Graphics2D g2d, Color color, float lineThickness) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineThickness));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Draw a line by setting alpha=0 for pixels along the line path.
     */
    private void drawTransparentLine(BufferedImage image, Graphics2D g2d, Point p1, Point p2, float lineThickness) {
        // Check for null parameters
        if (image == null || g2d == null || p1 == null || p2 == null) {
            System.err.println("Null parameter in drawTransparentLine");
            return;
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
     * Draw a path outline by setting alpha=0 for pixels along the path.
     */
    private void drawTransparentPath(BufferedImage image, Graphics2D g2d, Path2D path, float lineThickness) {
        // Check for null parameters
        if (image == null || g2d == null || path == null) {
            System.err.println("Null parameter in drawTransparentPath");
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
     * Fill a path by setting alpha=0 for pixels inside the path.
     */
    private void fillTransparentPath(BufferedImage image, Graphics2D g2d, Path2D path) {
        // Check for null parameters
        if (image == null || g2d == null || path == null) {
            System.err.println("Null parameter in fillTransparentPath");
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

    /**
     * Calculate points along a Catmull-Rom spline.
     */
    public List<Point> calculateCurvePoints(List<Point> controlPoints) {
        if (controlPoints.size() < 2) {
            return new java.util.ArrayList<>(controlPoints);
        }

        List<Point> curvePoints = new java.util.ArrayList<>();

        // Create phantom points at the ends if needed
        List<Point> points = new java.util.ArrayList<>();

        // Add first control point as phantom start point
        Point p0 = controlPoints.get(0);
        Point p1 = controlPoints.get(1);
        // Reflect p1 around p0
        int dx = p0.x - p1.x;
        int dy = p0.y - p1.y;
        points.add(new Point(p0.x + dx, p0.y + dy));

        // Add all control points
        points.addAll(controlPoints);

        // Add last control point as phantom end point
        Point pn = controlPoints.get(controlPoints.size() - 1);
        Point pn_1 = controlPoints.get(controlPoints.size() - 2);
        // Reflect pn_1 around pn
        int dx2 = pn.x - pn_1.x;
        int dy2 = pn.y - pn_1.y;
        points.add(new Point(pn.x + dx2, pn.y + dy2));

        // Sample curve segments
        int segments = 20; // Number of line segments to draw between control points

        // Go through all actual control points (skipping phantom ones)
        for (int i = 1; i < points.size() - 2; i++) {
            Point cp0 = points.get(i - 1);
            Point cp1 = points.get(i);
            Point cp2 = points.get(i + 1);
            Point cp3 = points.get(i + 2);

            // Add the starting point of this segment
            if (i == 1) {
                curvePoints.add(cp1);
            }

            // Calculate intermediate curve points
            for (int j = 1; j <= segments; j++) {
                float t = j / (float) segments;
                Point pt = interpolateCatmullRom(cp0, cp1, cp2, cp3, t, curveTension);
                curvePoints.add(pt);
            }
        }

        return curvePoints;
    }

    /**
     * Interpolate a point on a Catmull-Rom curve.
     */
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

    /**
     * Draw control points for curve editing.
     */
    public void drawControlPoints(Graphics2D g2d, List<Point> points, float zoomFactor, float lineThickness) {
        if (points.isEmpty()) return;

        // Calculate proper size based on line thickness but not affected by zoom
        int markerSize = Math.max(6, Math.round(lineThickness) + 2);
        int halfMarker = markerSize / 2;

        // Draw the control points with the original scale
        g2d.setColor(Color.WHITE);
        for (Point p : points) {
            int x = (int) (p.x * zoomFactor);
            int y = (int) (p.y * zoomFactor);
            g2d.fillOval(x - halfMarker, y - halfMarker, markerSize, markerSize);
        }

        g2d.setColor(Color.BLACK);
        for (Point p : points) {
            int x = (int) (p.x * zoomFactor);
            int y = (int) (p.y * zoomFactor);
            g2d.drawOval(x - halfMarker, y - halfMarker, markerSize, markerSize);
        }

        // Draw dotted connecting lines for curve modes
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{3.0f}, 0.0f));
        g2d.setColor(new Color(100, 100, 100, 150)); // Semi-transparent gray

        // Draw control polygon
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            g2d.drawLine(
                    (int) (p1.x * zoomFactor), (int) (p1.y * zoomFactor),
                    (int) (p2.x * zoomFactor), (int) (p2.y * zoomFactor)
            );
        }
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }

    public void setCurveTension(float tension) {
        this.curveTension = Math.max(0.0f, Math.min(1.0f, tension));
    }
}