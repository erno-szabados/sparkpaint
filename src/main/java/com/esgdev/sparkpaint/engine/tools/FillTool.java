package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.selection.Selection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class FillTool implements DrawingTool {
    public static final int DEFAULT_FILL_EPSILON = 30;
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private int epsilon;
    private Point gradientStartPoint;
    private Point gradientEndPoint;
    private boolean isDrawingGradient = false;
    private Point initialClickPoint; // For smart gradient fill

    // Add the GradientPreview and GradientRenderer instances
    private final GradientPreview gradientPreview;
    private final GradientRenderer gradientRenderer;

    public enum FillMode {
        SMART_FILL("Smart Fill"),
        CANVAS_FILL("Entire region"),
        LINEAR_GRADIENT("Linear Gradient"),
        SMART_LINEAR("Smart Linear Gradient"),
        CIRCULAR_GRADIENT("Circular Gradient");;

        private final String displayName;

        FillMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private FillMode fillMode = FillMode.SMART_FILL;

    public FillTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.epsilon = DEFAULT_FILL_EPSILON;
        this.gradientPreview = new GradientPreview(canvas);
        this.gradientRenderer = new GradientRenderer(canvas);
    }

    public void setFillMode(FillMode mode) {
        this.fillMode = mode;
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    public void setEpsilon(int value) {
        this.epsilon = value;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No action needed for mouse moved
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (fillMode == FillMode.LINEAR_GRADIENT ||
                fillMode == FillMode.SMART_LINEAR ||
                fillMode == FillMode.CIRCULAR_GRADIENT) {
            // Save to undo stack before modifying
            canvas.saveToUndoStack();

            // Set start point and flag
            gradientStartPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            isDrawingGradient = true;

            // For smart gradient, we also need to store the click position
            if (fillMode == FillMode.SMART_LINEAR) {
                // Store initial click point for smart gradient
                initialClickPoint = gradientStartPoint;
            }

            return; // Skip other fill operations
        }


        //SelectionManager selectionManager = canvas.getSelectionManager();
        Selection selection = canvas.getSelection();

        // Convert to world coordinates to check selection containment
        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

        // If there's a selection with outline, only allow drawing inside it
        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
            return; // Don't fill outside selection when one exists
        }

        // Save to undo stack before modifying
        canvas.saveToUndoStack();

        // Get the target image for filling - either selection content or current layer
        BufferedImage targetImage;
        GeneralPath clipPath = null;
        Point fillPoint;

        if (selection != null && selection.hasOutline() && selection.contains(worldPoint)) {
            targetImage = selection.getContent();
            if (targetImage == null) return;
            selection.setModified(true);

            Rectangle bounds = selection.getBounds();

            // Calculate fill point in selection content coordinates
            fillPoint = new Point(
                    worldPoint.x - bounds.x,
                    worldPoint.y - bounds.y
            );

            // Check if the fill point is within the content bounds
            if (fillPoint.x < 0 || fillPoint.x >= targetImage.getWidth() ||
                    fillPoint.y < 0 || fillPoint.y >= targetImage.getHeight()) {
                return;
            }

            // If it's a path selection, create a clipping path
            // Create a copy of the path with adjusted coordinates
            GeneralPath originalPath = selection.getPath();
            clipPath = new GeneralPath(originalPath);
            AffineTransform transform = AffineTransform.getTranslateInstance(-bounds.x, -bounds.y);
            clipPath.transform(transform);
        } else {
            // Use the current layer instead of the main canvas image
            targetImage = canvas.getCurrentLayerImage();
            fillPoint = worldPoint;

            // Check if the fill point is within the canvas bounds
            if (fillPoint.x < 0 || fillPoint.x >= targetImage.getWidth() ||
                    fillPoint.y < 0 || fillPoint.y >= targetImage.getHeight()) {
                return;
            }
        }

        // Get target color at fill point
        int targetRGB = targetImage.getRGB(fillPoint.x, fillPoint.y);
        Color targetColor = new Color(targetRGB, true); // Include alpha

        // In the mousePressed method where it handles normal fill operations:
        Color replacementColor = SwingUtilities.isLeftMouseButton(e) ?
                canvas.getDrawingColor() : canvas.getFillColor();

        switch (fillMode) {
            case SMART_FILL:
                smartFill(targetImage, fillPoint.x, fillPoint.y, targetColor, replacementColor, epsilon, clipPath);
                break;
            case CANVAS_FILL:
                canvasFill(targetImage, replacementColor, clipPath);
                break;
        }


        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ((fillMode == FillMode.LINEAR_GRADIENT ||
                fillMode == FillMode.SMART_LINEAR ||
                fillMode == FillMode.CIRCULAR_GRADIENT) && isDrawingGradient) {
            // Get current point
            gradientEndPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Create temporary canvas for preview
            BufferedImage tempCanvas = canvas.getToolCanvas();
            if (tempCanvas == null) {
                int width = canvas.getCurrentLayerImage().getWidth();
                int height = canvas.getCurrentLayerImage().getHeight();
                tempCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                canvas.setToolCanvas(tempCanvas);
            }

            // Clear and prepare the preview
            Graphics2D g2d = tempCanvas.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);

            // Get selection information for preview
            Selection selection = canvas.getSelection();
            GeneralPath clipPath = null;
            if (selection != null && selection.hasOutline()) {
                clipPath = selection.getPath();
            }

            switch (fillMode) {
                case SMART_LINEAR:
                    CoordinateContext ctx = createCoordinateContext(selection, initialClickPoint, gradientStartPoint, gradientEndPoint);
                    gradientPreview.previewSmartLinear(g2d, initialClickPoint, gradientStartPoint, gradientEndPoint,
                            clipPath, ctx, epsilon);
                    break;
                case LINEAR_GRADIENT:
                    gradientPreview.previewLinearGradient(g2d, gradientStartPoint, gradientEndPoint, clipPath);
                    break;
                case CIRCULAR_GRADIENT:
                    gradientPreview.previewCircularGradient(g2d, gradientStartPoint, gradientEndPoint, clipPath);
                    break;
            }
            g2d.dispose();

            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ((fillMode == FillMode.LINEAR_GRADIENT ||
                fillMode == FillMode.SMART_LINEAR ||
                fillMode == FillMode.CIRCULAR_GRADIENT) && isDrawingGradient) {
            Selection selection = canvas.getSelection();

            // Get final gradient end point
            gradientEndPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Target image and clip path setup
            BufferedImage targetImage;
            GeneralPath clipPath = null;
            Point adjustedStart = gradientStartPoint;
            Point adjustedEnd = gradientEndPoint;
            Point adjustedClickPoint = initialClickPoint; // For smart gradient

            if (selection != null && selection.hasOutline()) {
                targetImage = selection.getContent();
                if (targetImage == null) return;
                selection.setModified(true);

                Rectangle bounds = selection.getBounds();

                // Adjust coordinates for selection
                adjustedStart = new Point(gradientStartPoint.x - bounds.x, gradientStartPoint.y - bounds.y);
                adjustedEnd = new Point(gradientEndPoint.x - bounds.x, gradientEndPoint.y - bounds.y);

                if (fillMode == FillMode.SMART_LINEAR) {
                    adjustedClickPoint = new Point(initialClickPoint.x - bounds.x, initialClickPoint.y - bounds.y);
                }

                // Create clip path
                GeneralPath originalPath = selection.getPath();
                clipPath = new GeneralPath(originalPath);
                AffineTransform transform = AffineTransform.getTranslateInstance(-bounds.x, -bounds.y);
                clipPath.transform(transform);
            } else {
                targetImage = canvas.getCurrentLayerImage();
            }

            // Apply the gradient using the renderer
            if (fillMode == FillMode.SMART_LINEAR) {
                try {
                    // Check if click point is within bounds
                    int width = targetImage.getWidth();
                    int height = targetImage.getHeight();

                    // Ensure all points are within bounds
                    adjustedClickPoint.x = Math.max(0, Math.min(width - 1, adjustedClickPoint.x));
                    adjustedClickPoint.y = Math.max(0, Math.min(height - 1, adjustedClickPoint.y));
                    adjustedStart.x = Math.max(0, Math.min(width - 1, adjustedStart.x));
                    adjustedStart.y = Math.max(0, Math.min(height - 1, adjustedStart.y));
                    adjustedEnd.x = Math.max(0, Math.min(width - 1, adjustedEnd.x));
                    adjustedEnd.y = Math.max(0, Math.min(height - 1, adjustedEnd.y));

                    // Get target color at initial click point
                    int targetRGB = targetImage.getRGB(adjustedClickPoint.x, adjustedClickPoint.y);
                    Color targetColor = new Color(targetRGB, true);

                    gradientRenderer.smartLinearGradientFill(targetImage, adjustedClickPoint.x, adjustedClickPoint.y,
                            targetColor, adjustedStart, adjustedEnd, epsilon, clipPath);
                } catch (Exception ex) {
                    // Log error and recover gracefully
                    System.err.println("Error applying smart gradient fill: " + ex.getMessage());
                }
            } else if (fillMode == FillMode.CIRCULAR_GRADIENT) {
                gradientRenderer.applyCircularGradient(targetImage, adjustedStart, adjustedEnd, clipPath);
            } else {
                gradientRenderer.applyLinearGradient(targetImage, adjustedStart, adjustedEnd, clipPath);
            }

            // Clean up
            isDrawingGradient = false;
            gradientStartPoint = null;
            gradientEndPoint = null;
            initialClickPoint = null;
            gradientPreview.clearMask();
            canvas.setToolCanvas(null);
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
        return "Fill tool selected";
    }

    public void setAntiAliasing(boolean useAntiAliasing) {
        gradientPreview.setAntiAliasing(useAntiAliasing);
        gradientRenderer.setAntiAliasing(useAntiAliasing);
    }

    private void canvasFill(BufferedImage image, Color replacementColor, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int replacementRGB = replacementColor.getRGB();
        boolean isTransparentFill = replacementColor.getAlpha() == 0;

        // Use clipPath if available, otherwise fill entire image
        if (clipPath != null) {
            // Fill only within the clip path
            Graphics2D g2d = image.createGraphics();
            g2d.setClip(clipPath);

            if (isTransparentFill) {
                // Clear to transparency
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, width, height);
            } else {
                // Normal fill
                g2d.setColor(replacementColor);
                g2d.fillRect(0, 0, width, height);
            }
            g2d.dispose();
        } else {
            // Fill the entire image
            if (isTransparentFill) {
                // Clear each pixel to transparency
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, image.getRGB(x, y) & 0x00FFFFFF);
                    }
                }
            } else {
                // Normal fill
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        image.setRGB(x, y, replacementRGB);
                    }
                }
            }
        }
    }

    private void smartFill(BufferedImage image, int x, int y, Color targetColor, Color replacementColor,
                           int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        // Check if the fill is actually setting transparency
        boolean isTransparentFill = replacementColor.getAlpha() == 0;

        if (targetRGB == replacementRGB) {
            return;
        }

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

            int currentRGB = image.getRGB(x, y);

            double distance = colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Handle transparency specially
            if (isTransparentFill) {
                // Set full transparency (alpha = 0), preserving RGB
                image.setRGB(x, y, currentRGB & 0x00FFFFFF);
            } else {
                image.setRGB(x, y, replacementRGB);
            }

            visited[x][y] = true;

            // Check neighbors
            if (x > 0) stack.push(new Point(x - 1, y));
            if (x < width - 1) stack.push(new Point(x + 1, y));
            if (y > 0) stack.push(new Point(x, y - 1));
            if (y < height - 1) stack.push(new Point(x, y + 1));
        }
    }

    private double colorDistance(int rgb1, int rgb2) {
        // Extract color components including alpha
        int a1 = (rgb1 >> 24) & 0xFF;
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int a2 = (rgb2 >> 24) & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        // Normalize alpha values
        double alpha1 = a1 / 255.0;
        double alpha2 = a2 / 255.0;

        // Calculate weighted color differences
        double deltaR = (r1 - r2) * alpha1 * alpha2;
        double deltaG = (g1 - g2) * alpha1 * alpha2;
        double deltaB = (b1 - b2) * alpha1 * alpha2;
        double deltaA = (a1 - a2);

        // Return the combined distance
        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB + deltaA * deltaA);
    }

    // Helper method for coordinate adjustment
    private Point adjustToImageBounds(Point p, int width, int height) {
        int x = Math.max(0, Math.min(width - 1, p.x));
        int y = Math.max(0, Math.min(height - 1, p.y));
        return new Point(x, y);
    }

    // Extract coordinate transformations for selection handling
    private CoordinateContext createCoordinateContext(Selection selection, Point clickPoint,
                                                      Point startPoint, Point endPoint) {
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
}