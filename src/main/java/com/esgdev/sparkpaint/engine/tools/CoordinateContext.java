package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

/**
 * Helper class to store coordinate context information for gradient operations
 */
public class CoordinateContext {
    public BufferedImage currentImage;
    public GeneralPath clipPath;
    public Rectangle bounds;
    public Point adjustedClickPoint;
    public Point adjustedStart;
    public Point adjustedEnd;

    /**
     * Creates a coordinate context for gradient operations
     */
    public static CoordinateContext create(DrawingCanvas canvas, Selection selection,
                                           Point clickPoint, Point startPoint, Point endPoint) {
        CoordinateContext ctx = new CoordinateContext();
        ctx.currentImage = canvas.getCurrentLayerImage();
        ctx.adjustedClickPoint = clickPoint;
        ctx.adjustedStart = startPoint;
        ctx.adjustedEnd = endPoint;

        if (selection != null && selection.hasOutline()) {
            ctx.clipPath = selection.getPath();
            ctx.bounds = selection.getBounds();

            BufferedImage selectionContent = selection.getContent();
            if (selectionContent != null) {
                ctx.currentImage = selectionContent;

                // Adjust all coordinates to selection's local coordinate system
                ctx.adjustedClickPoint = new Point(clickPoint.x - ctx.bounds.x, clickPoint.y - ctx.bounds.y);
                ctx.adjustedStart = new Point(startPoint.x - ctx.bounds.x, startPoint.y - ctx.bounds.y);
                ctx.adjustedEnd = new Point(endPoint.x - ctx.bounds.x, endPoint.y - ctx.bounds.y);

                // Transform clip path to selection's coordinate system
                if (ctx.clipPath != null) {
                    ctx.clipPath = new GeneralPath(ctx.clipPath);
                    AffineTransform transform = AffineTransform.getTranslateInstance(-ctx.bounds.x, -ctx.bounds.y);
                    ctx.clipPath.transform(transform);
                }
            }
        }

        int width = ctx.currentImage.getWidth();
        int height = ctx.currentImage.getHeight();

        // Apply bounds checking to all adjusted points
        ctx.adjustedClickPoint = adjustToImageBounds(ctx.adjustedClickPoint, width, height);
        ctx.adjustedStart = adjustToImageBounds(ctx.adjustedStart, width, height);
        ctx.adjustedEnd = adjustToImageBounds(ctx.adjustedEnd, width, height);

        return ctx;
    }

    /**
     * Helper method for coordinate adjustment
     */
    private static Point adjustToImageBounds(Point p, int width, int height) {
        int x = Math.max(0, Math.min(width - 1, p.x));
        int y = Math.max(0, Math.min(height - 1, p.y));
        return new Point(x, y);
    }
}