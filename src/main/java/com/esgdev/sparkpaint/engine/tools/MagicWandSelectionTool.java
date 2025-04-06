package com.esgdev.sparkpaint.engine.tools;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.selection.Selection;
import com.esgdev.sparkpaint.engine.tools.renderers.RenderUtils;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MagicWandSelectionTool extends AbstractSelectionTool {
    private int tolerance = 32; // Default tolerance value (0-255)
    Point worldDragOffset;

    public MagicWandSelectionTool(DrawingCanvas canvas) {
        super(canvas);
    }

    @Override
    protected void handleSelectionStart(MouseEvent e) {
        Selection selection = canvas.getSelection();

        if (selection == null || !selection.hasOutline() || !selection.contains(worldStartPoint)) {
            // Start new magic wand selection
            createMagicWandSelection(worldStartPoint);
        } else if (selection.contains(worldStartPoint) && selection.isActive()) {
            // Start dragging existing selection
            startDragging(selection);
        }
    }

    @Override
    protected void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content) {

    }

    private void createMagicWandSelection(Point point) {
        // Create a mask using the smart fill algorithm
        BufferedImage compositeLayers = createCompositeLayersImage();
        BufferedImage mask = new BufferedImage(
                compositeLayers.getWidth(),
                compositeLayers.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        // Get target color from composite image
        int x = point.x;
        int y = point.y;

        // Ensure coordinates are within bounds
        if (x < 0 || y < 0 || x >= compositeLayers.getWidth() || y >= compositeLayers.getHeight()) {
            return;
        }

        Color targetColor = new Color(compositeLayers.getRGB(x, y), true);

        // Create mask with smart fill algorithm
        RenderUtils.generateSmartFillMask(mask, compositeLayers, x, y, targetColor, tolerance, null);

        // Convert mask to selection path
        GeneralPath selectionPath = createPathFromMask(mask);

        if (selectionPath != null) {
            Selection selection = new Selection(selectionPath, null);
            canvas.setSelection(selection);
            finalizeSelection(selection);
        }
    }

    private BufferedImage createCompositeLayersImage() {
        // Get canvas dimensions
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Create composite image
        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        // Draw all visible layers
        List<Layer> layers = canvas.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }

        g2d.dispose();
        return composite;
    }

    private GeneralPath createPathFromMask(BufferedImage mask) {
        // Find the contour of the mask
        List<Point> contourPoints = traceContour(mask);

        if (contourPoints.isEmpty()) {
            return null;
        }

        // Create path from contour points
        GeneralPath path = new GeneralPath();
        Point first = contourPoints.get(0);
        path.moveTo(first.x, first.y);

        for (int i = 1; i < contourPoints.size(); i++) {
            Point p = contourPoints.get(i);
            path.lineTo(p.x, p.y);
        }

        path.closePath();
        return path;
    }

    private List<Point> traceContour(BufferedImage mask) {
        List<Point> contourPoints = new ArrayList<>();
        int width = mask.getWidth();
        int height = mask.getHeight();

        // Find the first non-transparent pixel (starting point)
        Point start = null;
        for (int y = 0; y < height && start == null; y++) {
            for (int x = 0; x < width; x++) {
                if ((mask.getRGB(x, y) & 0xFF000000) != 0) {
                    start = new Point(x, y);
                    break;
                }
            }
        }

        if (start == null) {
            return contourPoints; // Empty mask
        }

        // Directions for 8-connected neighbors (clockwise from right)
        int[][] directions = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
        };

        Point current = start;
        int dir = 0; // Start by looking right

        do {
            contourPoints.add(new Point(current.x, current.y));

            // Look for next contour point
            boolean found = false;
            for (int i = 0; i < 8; i++) {
                int nextDir = (dir + i) % 8;
                int nx = current.x + directions[nextDir][0];
                int ny = current.y + directions[nextDir][1];

                // Check if in bounds
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    // Check if it's a filled pixel
                    if ((mask.getRGB(nx, ny) & 0xFF000000) != 0) {
                        current = new Point(nx, ny);
                        dir = (nextDir + 6) % 8; // Start looking 90° counter-clockwise
                        found = true;
                        break;
                    }
                }
            }

            if (!found) break;

            // Prevent infinite loops with a reasonable limit
            if (contourPoints.size() > width * height) break;

        } while (!current.equals(start) && !contourPoints.isEmpty());

        // Simplify the path by removing some points
        return simplifyPath(contourPoints);
    }

    private List<Point> simplifyPath(List<Point> points) {
        // Simple point reduction - keep only every N points
        if (points.size() <= 100) return points;

        List<Point> simplified = new ArrayList<>();
        int step = points.size() / 100;

        for (int i = 0; i < points.size(); i += step) {
            simplified.add(points.get(i));
        }

        return simplified;
    }

    private void startDragging(Selection selection) {
        isDragging = true;
        Rectangle bounds = selection.getBounds();
        worldDragOffset = new Point(
                worldStartPoint.x - bounds.x,
                worldStartPoint.y - bounds.y);

        if (originalSelectionLocation == null) {
            originalSelectionLocation = new Point(bounds.x, bounds.y);
        }
    }

    @Override
    protected void finalizeSelection(Selection selection) {
        if (selection == null || !selection.hasOutline()) {
            return;
        }

        Rectangle bounds = selection.getBounds();

        // Check if selection is too small
        if (isSelectionTooSmall(bounds)) {
            canvas.clearSelection();
            originalSelectionLocation = null;
            return;
        }

        BufferedImage selectionContent = createSelectionImage(bounds);
        BufferedImage transparentContent = createTransparentSelectionImage(selectionContent);

        selection.setTransparent(transparencyEnabled);
        selection.setContent(transparentContent, canvas.getFillColor());
        originalSelectionLocation = new Point(bounds.x, bounds.y);

        canvas.notifyClipboardStateChanged();
    }

    private BufferedImage createSelectionImage(Rectangle bounds) {
        BufferedImage selectionContent = new BufferedImage(
                bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = selectionContent.createGraphics();

        // Get the selection path and translate it to image coordinates
        GeneralPath path = new GeneralPath(canvas.getSelection().getPath());
        path.transform(java.awt.geom.AffineTransform.getTranslateInstance(-bounds.x, -bounds.y));
        g2d.setClip(path);

        // Draw all visible layers
        List<Layer> layers = canvas.getLayers();
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), -bounds.x, -bounds.y, null);
            }
        }

        g2d.dispose();
        return selectionContent;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
        Selection selection = canvas.getSelection();

        if (selection == null || !isDragging) {
            return;
        }

        // Update the position of the selection
        int newX = worldDragPoint.x - worldDragOffset.x;
        int newY = worldDragPoint.y - worldDragOffset.y;
        GeneralPath path = selection.getPath();
        Rectangle bounds = path.getBounds();
        int deltaX = newX - bounds.x;
        int deltaY = newY - bounds.y;

        if (deltaX != 0 || deltaY != 0) {
            java.awt.geom.AffineTransform transform =
                java.awt.geom.AffineTransform.getTranslateInstance(deltaX, deltaY);
            path.transform(transform);
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Selection selection = canvas.getSelection();
        if (selection == null) {
            return;
        }

        if (isDragging) {
            isDragging = false;
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (selection.contains(worldPoint)) {
                canvas.setCursor(handCursor);
            } else {
                canvas.setCursor(crosshairCursor);
            }
        }

        canvas.repaint();
    }

    @Override
    public void mouseScrolled(MouseWheelEvent e) {
        // Handle zooming if needed
    }

    @Override
    public String statusMessage() {
        return "Magic Wand selection tool: Click to select similar colors";
    }

    public void setTolerance(int tolerance) {
        this.tolerance = Math.max(0, Math.min(255, tolerance));
    }

    public int getTolerance() {
        return tolerance;
    }
}