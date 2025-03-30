package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.filters.SobelFilter;
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
    public static final int DEFAULT_EDGE_THRESHOLD = 50;
    private final DrawingCanvas canvas;
    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private int epsilon;
    private int edgeThreshold; // Adjustable threshold for edge detection
    private boolean useAntiAliasing = true;
    private Point gradientStartPoint;
    private Point gradientEndPoint;
    private boolean isDrawingGradient = false;
    private Point initialClickPoint; // For smart gradient fill

    public enum FillMode {
        SMART_FILL("Smart Fill"),
        CANVAS_FILL("Entire region"),
        GRADIENT_FILL("Gradient Fill"),
        SMART_GRADIENT_FILL("Smart Gradient Fill");

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
        this.edgeThreshold = DEFAULT_EDGE_THRESHOLD; // Default edge threshold
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
        if (fillMode == FillMode.GRADIENT_FILL || fillMode == FillMode.SMART_GRADIENT_FILL) {
            // Save to undo stack before modifying
            canvas.saveToUndoStack();

            // Set start point and flag
            gradientStartPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            isDrawingGradient = true;

            // For smart gradient, we also need to store the click position
            if (fillMode == FillMode.SMART_GRADIENT_FILL) {
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

        Color replacementColor = SwingUtilities.isLeftMouseButton(e) ?
                canvas.getDrawingColor() : canvas.getFillColor();


        // Perform the appropriate fill operation based on the mode
        switch (fillMode) {
            case SMART_FILL:

                // Perform the fill operation on the appropriate target
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
        if ((fillMode == FillMode.GRADIENT_FILL || fillMode == FillMode.SMART_GRADIENT_FILL) && isDrawingGradient) {
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

            // Draw gradient preview
            if (fillMode == FillMode.SMART_GRADIENT_FILL) {
                previewSmartGradient(g2d, initialClickPoint, gradientStartPoint, gradientEndPoint);
            } else {
                previewGradient(g2d, gradientStartPoint, gradientEndPoint);
            }
            g2d.dispose();

            canvas.repaint();
        }
    }

   @Override
    public void mouseReleased(MouseEvent e) {
        if ((fillMode == FillMode.GRADIENT_FILL || fillMode == FillMode.SMART_GRADIENT_FILL) && isDrawingGradient) {
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

                if (fillMode == FillMode.SMART_GRADIENT_FILL) {
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

            // Apply the gradient
            if (fillMode == FillMode.SMART_GRADIENT_FILL) {
                // Get target color at initial click point
                int targetRGB = targetImage.getRGB(adjustedClickPoint.x, adjustedClickPoint.y);
                Color targetColor = new Color(targetRGB, true);

                smartGradientFill(targetImage, adjustedClickPoint.x, adjustedClickPoint.y,
                                targetColor, adjustedStart, adjustedEnd, epsilon, clipPath);
            } else {
                applyGradient(targetImage, adjustedStart, adjustedEnd, clipPath);
            }

            // Clean up
            isDrawingGradient = false;
            gradientStartPoint = null;
            gradientEndPoint = null;
            initialClickPoint = null;
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
        this.useAntiAliasing = useAntiAliasing;
    }

    private void canvasFill(BufferedImage image, Color replacementColor, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int replacementRGB = replacementColor.getRGB();

        // Use clipPath if available, otherwise fill entire image
        if (clipPath != null) {
            // Fill only within the clip path
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(replacementColor);
            g2d.setClip(clipPath);
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();
        } else {
            // Fill the entire image
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, replacementRGB);
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

        return Math.sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB + deltaA * deltaA);
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

   private void previewGradient(Graphics2D g2d, Point start, Point end) {
        Selection selection = canvas.getSelection();
        GeneralPath clipPath = null;

        // Apply selection clipping if it exists
        if (selection != null && selection.hasOutline()) {
            clipPath = selection.getPath();
            g2d.setClip(clipPath);
        }

        // Draw a line showing gradient direction with dashed style
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // Draw start marker with white outline
        int startMarkerSize = 8;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(start.x - startMarkerSize/2 - 1, start.y - startMarkerSize/2 - 1,
                    startMarkerSize + 2, startMarkerSize + 2);
        g2d.setColor(canvas.getDrawingColor());
        g2d.fillOval(start.x - startMarkerSize/2, start.y - startMarkerSize/2,
                    startMarkerSize, startMarkerSize);

        // Draw end marker with white outline (larger than start marker)
        int endMarkerSize = 12;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(end.x - endMarkerSize/2 - 1, end.y - endMarkerSize/2 - 1,
                    endMarkerSize + 2, endMarkerSize + 2);
        g2d.setColor(canvas.getFillColor());
        g2d.fillOval(end.x - endMarkerSize/2, end.y - endMarkerSize/2,
                    endMarkerSize, endMarkerSize);

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

    private void previewSmartGradient(Graphics2D g2d, Point clickPoint, Point start, Point end) {
        Selection selection = canvas.getSelection();
        GeneralPath clipPath = null;
        Point adjustedClickPoint = clickPoint;

        // Apply selection clipping if it exists
        if (selection != null && selection.hasOutline()) {
            clipPath = selection.getPath();
            g2d.setClip(clipPath);

            if (clickPoint != null) {
                Rectangle bounds = selection.getBounds();
                adjustedClickPoint = new Point(clickPoint.x - bounds.x, clickPoint.y - bounds.y);
            }
        }

        // Draw the gradient direction line
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // Draw start marker with white outline
        int startMarkerSize = 8;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(start.x - startMarkerSize/2 - 1, start.y - startMarkerSize/2 - 1,
                    startMarkerSize + 2, startMarkerSize + 2);
        g2d.setColor(canvas.getDrawingColor());
        g2d.fillOval(start.x - startMarkerSize/2, start.y - startMarkerSize/2,
                    startMarkerSize, startMarkerSize);

        // Draw end marker with white outline (larger than start marker)
        int endMarkerSize = 12;
        g2d.setColor(Color.WHITE);
        g2d.fillOval(end.x - endMarkerSize/2 - 1, end.y - endMarkerSize/2 - 1,
                    endMarkerSize + 2, endMarkerSize + 2);
        g2d.setColor(canvas.getFillColor());
        g2d.fillOval(end.x - endMarkerSize/2, end.y - endMarkerSize/2,
                    endMarkerSize, endMarkerSize);

        // Can't generate full preview of smart fill during drag
        // Instead, show the gradient vector and a partially transparent gradient overlay
        // in areas that would likely be filled
        BufferedImage currentImage = canvas.getCurrentLayerImage();
        if (clickPoint != null && currentImage != null) {
            try {
                // Get target color at initial click point
                int targetRGB = currentImage.getRGB(adjustedClickPoint.x, adjustedClickPoint.y);
                Color targetColor = new Color(targetRGB, true);

                // Create a simple gradient for preview
                GradientPaint paint = new GradientPaint(
                        start.x, start.y, canvas.getDrawingColor(),
                        end.x, end.y, canvas.getFillColor()
                );
                g2d.setPaint(paint);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));

                // Show a reduced opacity indicator where the fill will likely go
                if (clipPath != null) {
                    g2d.fill(clipPath);
                } else {
                    g2d.fillRect(0, 0, currentImage.getWidth(), currentImage.getHeight());
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // Handle possible out of bounds issues during preview
            }
        }
    }

    private void applyGradient(BufferedImage image, Point start, Point end, GeneralPath clipPath) {
        Graphics2D g2d = image.createGraphics();

        // Set up quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON :
                        RenderingHints.VALUE_ANTIALIAS_OFF);

        // Set up gradient
        GradientPaint gradient = new GradientPaint(
                start.x, start.y, canvas.getDrawingColor(),
                end.x, end.y, canvas.getFillColor(),
                false // Don't cycle the gradient
        );
        g2d.setPaint(gradient);

        // Apply clipping if needed
        if (clipPath != null) {
            g2d.setClip(clipPath);
        }

        // Fill the area with gradient
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
    }

    private void smartGradientFill(BufferedImage image, int x, int y, Color targetColor,
                                  Point startPoint, Point endPoint, int epsilon, GeneralPath clipPath) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRGB = targetColor.getRGB();

        // Generate Sobel edge map
        BufferedImage edgeMap = SobelFilter.apply(image);

        // Create a mask image to store which pixels should be filled
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D maskG2d = mask.createGraphics();
        maskG2d.setColor(Color.BLACK);
        maskG2d.fillRect(0, 0, width, height);
        maskG2d.dispose();

        // Use the smart fill algorithm to determine which pixels to fill
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

            // Check color distance
            double distance = colorDistance(currentRGB, targetRGB);
            if (distance > epsilon || (clipPath != null && !clipPath.contains(x, y))) {
                continue;
            }

            // Mark this pixel as one to fill in the mask
            mask.setRGB(x, y, Color.WHITE.getRGB());
            visited[x][y] = true;

            // Check neighbors
            if (x > 0) stack.push(new Point(x - 1, y));
            if (x < width - 1) stack.push(new Point(x + 1, y));
            if (y > 0) stack.push(new Point(x, y - 1));
            if (y < height - 1) stack.push(new Point(x, y + 1));
        }

        // Apply gradient to the masked area
        Graphics2D g2d = image.createGraphics();

        // Set up quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Create the gradient
        GradientPaint gradient = new GradientPaint(
                startPoint.x, startPoint.y, canvas.getDrawingColor(),
                endPoint.x, endPoint.y, canvas.getFillColor(),
                false // Don't cycle the gradient
        );
        g2d.setPaint(gradient);

        // Apply the mask
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f));
        g2d.drawImage(mask, 0, 0, null);

        // Fill with gradient
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
    }
}