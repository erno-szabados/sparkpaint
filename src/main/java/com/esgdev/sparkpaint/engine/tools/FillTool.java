package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.filters.SobelFilter;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.selection.SelectionManager;

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
    public static final int DEFAULT_EDGE_THRESHOLD = 50;
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private int epsilon;
    private int edgeThreshold; // Adjustable threshold for edge detection
    private boolean useAntiAliasing = true;

    public FillTool(DrawingCanvas canvas) {
        this.canvas = canvas;
        this.epsilon = DEFAULT_FILL_EPSILON;
        this.edgeThreshold = DEFAULT_EDGE_THRESHOLD; // Default edge threshold
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

        Color replacementColor = SwingUtilities.isLeftMouseButton(e) ?
                canvas.getDrawingColor() : canvas.getFillColor();

        // Perform the fill operation on the appropriate target
        floodFill(targetImage, fillPoint.x, fillPoint.y, targetColor, replacementColor, epsilon, clipPath);

        canvas.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // No action needed for mouse dragged
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No action needed for mouse released
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
        this.useAntiAliasing = useAntiAliasing;
    }

   private void floodFill(BufferedImage image, int x, int y, Color targetColor, Color replacementColor,
                          int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        if (targetRGB == replacementRGB) {
            return;
        }

        // Generate Sobel edge map
        BufferedImage edgeMap = SobelFilter.apply(image);

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

            int currentAlpha = (currentRGB >> 24) & 0xFF;
            int effectiveThreshold = currentAlpha < 128 ?
                    Math.max(20, edgeThreshold / 2) : edgeThreshold;

            // Check edge status - prioritize this over color distance
            int edgeValue = (edgeMap.getRGB(x, y) >> 16) & 0xFF;
            if (edgeValue > effectiveThreshold) {
                // This is a strong edge - never fill regardless of color tolerance
                visited[x][y] = true;
                continue;
            }

            // Now check color distance
            double distance = colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Pixel passes both edge detection and color distance tests - fill it
            if (useAntiAliasing && edgeValue > edgeThreshold / 2) {
                // For pixels near edges, apply blended color
                float blendFactor = Math.max(0.2f, 1.0f - (edgeValue / 255.0f));
                Color blended = blendColors(new Color(currentRGB, true),
                        new Color(replacementRGB, true), blendFactor);
                image.setRGB(x, y, blended.getRGB());
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

        // Fully transparent pixels
        if (a1 == 0 && a2 == 0) {
            return 0; // Both fully transparent
        }

        // Calculate weighted distance with higher alpha weight
        double alphaWeight = 2.0;  // Increased for better transparency boundary detection
        double colorWeight = 0.8;  // Slightly reduced to prioritize alpha

        double deltaA = Math.abs(a1 - a2) * alphaWeight;
        double deltaR = Math.abs(r1 - r2) * colorWeight;
        double deltaG = Math.abs(g1 - g2) * colorWeight;
        double deltaB = Math.abs(b1 - b2) * colorWeight;

        return Math.sqrt(deltaR*deltaR + deltaG*deltaG + deltaB*deltaB + deltaA*deltaA);
    }

    // Helper method to blend colors for antialiasing
    private Color blendColors(Color c1, Color c2, float blendFactor) {
        float invBlendFactor = 1.0f - blendFactor;

        int r = Math.round(c1.getRed() * invBlendFactor + c2.getRed() * blendFactor);
        int g = Math.round(c1.getGreen() * invBlendFactor + c2.getGreen() * blendFactor);
        int b = Math.round(c1.getBlue() * invBlendFactor + c2.getBlue() * blendFactor);
        int a = Math.round(c1.getAlpha() * invBlendFactor + c2.getAlpha() * blendFactor);

        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b)),
                Math.min(255, Math.max(0, a))
        );
    }

    public void setEdgeThreshold(int threshold) {
        this.edgeThreshold = threshold;
    }
}