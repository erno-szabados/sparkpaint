package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * Helper class to render fill previews for linear and circular gradients
 */
public class FillPreview {
    private final DrawingCanvas canvas;
    private BufferedImage smartGradientMask;
    private Point lastMaskClickPoint;

    public FillPreview(DrawingCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Renders a preview of a linear gradient
     */
    public void previewLinearGradient(Graphics2D g2d, Point start, Point end, GeneralPath clipPath) {
        // Apply selection clipping if provided
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Draw a line showing gradient direction with dashed style
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // Draw color markers
        drawColorMarkers(g2d, start, end);

        // Draw a gradient preview
        GradientPaint paint = new GradientPaint(
                start.x, start.y, canvas.getDrawingColor(),
                end.x, end.y, canvas.getFillColor()
        );
        g2d.setPaint(paint);

        // Preview the gradient with a semi-transparent overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        // Fill either the selection area or the entire canvas
        if (clipPath != null) {
            g2d.fill(clipPath);
        } else {
            g2d.fillRect(0, 0, canvas.getCurrentLayerImage().getWidth(),
                    canvas.getCurrentLayerImage().getHeight());
        }
    }

    /**
     * Renders a preview of a circular gradient
     */
    public void previewCircularGradient(Graphics2D g2d, Point center, Point radiusPoint, GeneralPath clipPath) {
        // Apply selection clipping if provided
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Draw radius line with dashed style
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(center.x, center.y, radiusPoint.x, radiusPoint.y);

        // Draw center marker
        int centerMarkerSize = 8;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(center.x - centerMarkerSize / 2 - 1, center.y - centerMarkerSize / 2 - 1,
                centerMarkerSize + 2, centerMarkerSize + 2);
        g2d.setColor(canvas.getDrawingColor());
        g2d.fillOval(center.x - centerMarkerSize / 2, center.y - centerMarkerSize / 2,
                centerMarkerSize, centerMarkerSize);

        // Calculate radius
        double radius = center.distance(radiusPoint);

        // Create and preview the gradient
        RadialGradientPaint paint = new RadialGradientPaint(
                center,
                (float) radius,
                new float[]{0.0f, 1.0f},
                new Color[]{
                        canvas.getDrawingColor(),
                        canvas.getFillColor()
                }
        );
        g2d.setPaint(paint);

        // Preview with semi-transparent overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        if (clipPath != null) {
            g2d.fill(clipPath);
        } else {
            g2d.fillRect(0, 0, canvas.getCurrentLayerImage().getWidth(),
                    canvas.getCurrentLayerImage().getHeight());
        }
    }

    /**
     * Renders a preview of a smart linear gradient
     */
    public void previewSmartLinear(Graphics2D g2d, Point clickPoint, Point start, Point end,
                                   GeneralPath clipPath, CoordinateContext ctx, int epsilon) {
        if (clickPoint == null) return;

        // Apply selection clipping if provided
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Generate the mask if needed
        if (smartGradientMask == null || lastMaskClickPoint == null ||
                !lastMaskClickPoint.equals(ctx.adjustedClickPoint)) {

            try {
                generateMaskForGradient(ctx, epsilon);
                lastMaskClickPoint = new Point(ctx.adjustedClickPoint);
            } catch (Exception ex) {
                System.err.println("Error in preview: " + ex.getMessage());
            }
        }

        // Draw gradient direction indicators
        drawColorMarkers(g2d, start, end);
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // Draw the gradient preview
        drawGradientWithMask(g2d, ctx, false);
    }

    /**
     * Renders a preview of a smart circular gradient
     */
    public void previewSmartCircular(Graphics2D g2d, Point clickPoint, Point center, Point radiusPoint,
                                     GeneralPath clipPath, CoordinateContext ctx, int epsilon) {
        if (clickPoint == null) return;

        // Apply selection clipping if provided
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Generate the mask if needed
        if (smartGradientMask == null || lastMaskClickPoint == null ||
                !lastMaskClickPoint.equals(ctx.adjustedClickPoint)) {

            try {
                generateMaskForGradient(ctx, epsilon);
                lastMaskClickPoint = new Point(ctx.adjustedClickPoint);
            } catch (Exception ex) {
                System.err.println("Error in preview: " + ex.getMessage());
            }
        }

        // Draw gradient direction indicators
        // Draw radius line with dashed style
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(center.x, center.y, radiusPoint.x, radiusPoint.y);

        // Draw center marker
        int centerMarkerSize = 8;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(center.x - centerMarkerSize / 2 - 1, center.y - centerMarkerSize / 2 - 1,
                centerMarkerSize + 2, centerMarkerSize + 2);
        g2d.setColor(canvas.getDrawingColor());
        g2d.fillOval(center.x - centerMarkerSize / 2, center.y - centerMarkerSize / 2,
                centerMarkerSize, centerMarkerSize);

        // Draw the gradient preview
        drawGradientWithMask(g2d, ctx, true);
    }

    /**
     * Helper method to generate mask for gradient
     */
    private void generateMaskForGradient(CoordinateContext ctx, int epsilon) {
        int width = ctx.currentImage.getWidth();
        int height = ctx.currentImage.getHeight();

        // Create new mask
        smartGradientMask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get target color and generate mask
        int targetRGB = ctx.currentImage.getRGB(ctx.adjustedClickPoint.x, ctx.adjustedClickPoint.y);
        Color targetColor = new Color(targetRGB, true);

        generateSmartFillMask(smartGradientMask, ctx.currentImage,
                ctx.adjustedClickPoint.x, ctx.adjustedClickPoint.y,
                targetColor, epsilon, ctx.clipPath);
    }

    /**
     * Helper method to generate smart fill mask
     */
    private void generateSmartFillMask(BufferedImage mask, BufferedImage source,
                                       int x, int y, Color targetColor,
                                       int epsilon, GeneralPath clipPath) {
        int width = source.getWidth();
        int height = source.getHeight();
        int targetRGB = targetColor.getRGB();

        // Clear the mask
        Graphics2D maskG2d = mask.createGraphics();
        maskG2d.setComposite(AlphaComposite.Clear);
        maskG2d.fillRect(0, 0, width, height);
        maskG2d.dispose();

        // Use flood fill to identify pixels to include
        boolean[][] visited = new boolean[width][height];
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            x = p.x;
            y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height || visited[x][y]) {
                continue;
            }

            int currentRGB = source.getRGB(x, y);

            // Check color distance
            double distance = FillTool.colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Mark this pixel in the mask
            mask.setRGB(x, y, 0xFFFFFFFF); // Opaque white
            visited[x][y] = true;

            // Check neighbors
            if (x > 0) stack.push(new Point(x - 1, y));
            if (x < width - 1) stack.push(new Point(x + 1, y));
            if (y > 0) stack.push(new Point(x, y - 1));
            if (y < height - 1) stack.push(new Point(x, y + 1));
        }
    }

    /**
     * Helper method to draw gradient with mask
     */
    private void drawGradientWithMask(Graphics2D g2d, CoordinateContext ctx, boolean circular) {
        if (smartGradientMask == null) return;

        // Create preview image
        BufferedImage previewImage = new BufferedImage(
                ctx.currentImage.getWidth(), ctx.currentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D previewG2d = previewImage.createGraphics();

        if (circular) {
            // Calculate radius for circular gradient
            double radius = ctx.adjustedStart.distance(ctx.adjustedEnd);

            // Create radial gradient paint
            RadialGradientPaint paint = new RadialGradientPaint(
                    ctx.adjustedStart,
                    (float) radius,
                    new float[]{0.0f, 1.0f},
                    new Color[]{
                            canvas.getDrawingColor(),
                            canvas.getFillColor()
                    }
            );
            previewG2d.setPaint(paint);
        } else {
            // Create linear gradient paint
            GradientPaint paint = new GradientPaint(
                    ctx.adjustedStart.x, ctx.adjustedStart.y, canvas.getDrawingColor(),
                    ctx.adjustedEnd.x, ctx.adjustedEnd.y, canvas.getFillColor(), false
            );
            previewG2d.setPaint(paint);
        }

        previewG2d.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
        previewG2d.dispose();

        // Apply mask with proper transformation
        if (ctx.bounds != null) {
            g2d.translate(ctx.bounds.x, ctx.bounds.y);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2d.drawImage(smartGradientMask, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f));
        g2d.drawImage(previewImage, 0, 0, null);

        if (ctx.bounds != null) {
            g2d.translate(-ctx.bounds.x, -ctx.bounds.y);
        }
    }

    /**
     * Helper method to draw color markers
     */
    private void drawColorMarkers(Graphics2D g2d, Point start, Point end) {
        // Draw start marker with white outline
        int startMarkerSize = 8;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(start.x - startMarkerSize / 2 - 1, start.y - startMarkerSize / 2 - 1,
                startMarkerSize + 2, startMarkerSize + 2);
        g2d.setColor(canvas.getDrawingColor());
        g2d.fillOval(start.x - startMarkerSize / 2, start.y - startMarkerSize / 2,
                startMarkerSize, startMarkerSize);

        // Draw end marker with white outline (larger than start marker)
        int endMarkerSize = 12;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(end.x - endMarkerSize / 2 - 1, end.y - endMarkerSize / 2 - 1,
                endMarkerSize + 2, endMarkerSize + 2);
        g2d.setColor(canvas.getFillColor());
        g2d.fillOval(end.x - endMarkerSize / 2, end.y - endMarkerSize / 2,
                endMarkerSize, endMarkerSize);
    }

    /**
     * Clear any stored mask data
     */
    public void clearMask() {
        smartGradientMask = null;
        lastMaskClickPoint = null;
    }
}