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
            SelectionManager selectionManager = canvas.getSelectionManager();
            Selection selection = selectionManager.getSelection();

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
                GeneralPath originalPath = ((Selection) selection).getPath();
                clipPath = new GeneralPath(originalPath);
                AffineTransform transform = AffineTransform.getTranslateInstance(-bounds.x, -bounds.y);
                clipPath.transform(transform);
            } else {
                // Use the current layer instead of the main canvas image
                targetImage = canvas.getLayerManager().getCurrentLayerImage();
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
            BufferedImage edgeMap = SobelFilter.applySobelFilter(image);

            // Use scanline fill algorithm with edge detection
            boolean[][] visited = new boolean[width][height];
            Stack<Point> stack = new Stack<>();
            stack.push(new Point(x, y));

            while (!stack.isEmpty()) {
                Point p = stack.pop();
                x = p.x;
                y = p.y;

                // Find left boundary
                int leftX = x;
                while (leftX >= 0 &&
                       !visited[leftX][y] &&
                       colorDistance(image.getRGB(leftX, y), targetRGB) <= epsilon &&
                       !isEdge(edgeMap, leftX, y) &&
                       (clipPath == null || clipPath.contains(leftX, y))) {
                    leftX--;
                }
                leftX++;

                // Find right boundary
                int rightX = x;
                while (rightX < width &&
                       !visited[rightX][y] &&
                       colorDistance(image.getRGB(rightX, y), targetRGB) <= epsilon &&
                       !isEdge(edgeMap, rightX, y) &&
                       (clipPath == null || clipPath.contains(rightX, y))) {
                    rightX++;
                }
                rightX--;

                // Fill the scan line
                for (int i = leftX; i <= rightX; i++) {
                    image.setRGB(i, y, replacementRGB);
                    visited[i][y] = true;

                    // Check pixels above and below for next lines to fill
                    if (y > 0 && !visited[i][y-1] &&
                        colorDistance(image.getRGB(i, y-1), targetRGB) <= epsilon &&
                        !isEdge(edgeMap, i, y-1)) {
                        stack.push(new Point(i, y-1));
                    }
                    if (y < height-1 && !visited[i][y+1] &&
                        colorDistance(image.getRGB(i, y+1), targetRGB) <= epsilon &&
                        !isEdge(edgeMap, i, y+1)) {
                        stack.push(new Point(i, y+1));
                    }
                }
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

            // Handle transparency special cases
            if (a1 == 0 && a2 == 0) {
                return 0; // Both fully transparent, treat as identical
            }

            // If one is fully transparent and the other isn't, treat them as very different
            if ((a1 == 0 && a2 > 0) || (a2 == 0 && a1 > 0)) {
                return 255.0;
            }

            // For partially transparent pixels, adjust RGB based on alpha
            double alphaFactor = Math.abs(a1 - a2) / 255.0;

            // Calculate weighted color distance considering alpha
            double deltaA = a1 - a2;
            double deltaR = r1 - r2;
            double deltaG = g1 - g2;
            double deltaB = b1 - b2;

            // Weight alpha less than color components to allow similar colors with slightly different alpha to match
            double alphaWeight = 0.5;

            return Math.sqrt(
                (deltaR * deltaR) +
                (deltaG * deltaG) +
                (deltaB * deltaB) +
                (alphaWeight * deltaA * deltaA)
            );
        }

        private boolean isEdge(BufferedImage edgeMap, int x, int y) {
            // Get grayscale value from edge map (all channels have same value)
            int edgeValue = (edgeMap.getRGB(x, y) >> 16) & 0xFF;
            return edgeValue > edgeThreshold;
        }

        public void setEdgeThreshold(int threshold) {
            this.edgeThreshold = threshold;
        }
    }