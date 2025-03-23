package com.esgdev.sparkpaint.engine.tools;

    import com.esgdev.sparkpaint.engine.DrawingCanvas;
    import com.esgdev.sparkpaint.engine.selection.Selection;
    import com.esgdev.sparkpaint.engine.selection.SelectionManager;

    import java.awt.*;
    import java.awt.event.MouseEvent;
    import java.awt.event.MouseWheelEvent;
    import java.awt.image.BufferedImage;

    public class CircleTool implements DrawingTool {
        private final DrawingCanvas canvas;
        private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        private Point startPoint;
        private boolean isFilled;
        private boolean useAntiAliasing = true;
        private boolean isCenterBased = false;

        public CircleTool(DrawingCanvas canvas) {
            this.canvas = canvas;
            this.isFilled = false;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // No action needed for mouse moved
        }

        @Override
        public void mousePressed(MouseEvent e) {
            //SelectionManager selectionManager = canvas.getSelectionManager();
            Selection selection = canvas.getSelection();

            // Convert to world coordinates and check if we're in a selection
            Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
            if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
                return; // Don't start drawing outside selection
            }

            // Save start point using appropriate coordinate system
            startPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
            canvas.saveToUndoStack();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (startPoint == null) return;

            //SelectionManager selectionManager = canvas.getSelectionManager();
            Selection selection = canvas.getSelection();

            // Get current point in appropriate coordinate system
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Create temporary canvas for preview
            BufferedImage tempCanvas = canvas.getTempCanvas();
            Graphics2D g2d = tempCanvas.createGraphics();

            // Clear the temp canvas
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);

            // Apply rendering settings
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Set color
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
            }
            g2d.setColor(canvas.getDrawingColor());

            // Draw the circle
            if (isCenterBased) {
                drawCenterBasedCircle(g2d, startPoint, point);
            } else {
                drawCornerBasedCircle(g2d, startPoint, point);
            }

            g2d.dispose();
            canvas.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (startPoint == null) return;

            //SelectionManager selectionManager = canvas.getSelectionManager();
            Selection selection = canvas.getSelection();

            // Get current point in appropriate coordinate system
            Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

            // Get appropriate graphics context for drawing
            Graphics2D g2d;
            if (selection != null && selection.hasOutline()) {
                g2d = canvas.getDrawingGraphics();
            } else {
                // Draw on current layer instead of main image
                BufferedImage currentLayerImage = canvas.getLayerManager().getCurrentLayerImage();
                g2d = (Graphics2D) currentLayerImage.getGraphics();
            }

            // Apply rendering settings
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            // Draw the final circle
            if (isCenterBased) {
                drawCenterBasedCircle(g2d, startPoint, point);
            } else {
                drawCornerBasedCircle(g2d, startPoint, point);
            }

            g2d.dispose();

            // Clear the temp canvas and reset state
            canvas.setTempCanvas(null);
            startPoint = null;
            canvas.repaint();
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
            if (isCenterBased) {
                return isFilled ? "Circle tool (center-based, filled)" : "Circle tool (center-based, outline)";
            } else {
                return isFilled ? "Circle tool (filled)" : "Circle tool (outline)";
            }
        }

        private void drawCornerBasedCircle(Graphics2D g2d, Point start, Point end) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(end.x - start.x);
            int height = Math.abs(end.y - start.y);

            if (width > height) {
                height = width;
                if (end.y < start.y) {
                    y = start.y - height;
                }
            } else {
                width = height;
                if (end.x < start.x) {
                    x = start.x - width;
                }
            }

            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(x, y, width, height);
            }

            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, width, height);
        }

        private void drawCenterBasedCircle(Graphics2D g2d, Point center, Point edge) {
            int radius = (int) Math.sqrt(Math.pow(edge.x - center.x, 2) + Math.pow(edge.y - center.y, 2));
            int x = center.x - radius;
            int y = center.y - radius;
            int diameter = radius * 2;

            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(x, y, diameter, diameter);
            }

            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, diameter, diameter);
        }

        public boolean isFilled() {
            return isFilled;
        }

        public void setFilled(boolean filled) {
            isFilled = filled;
        }

        public void setCenterBased(boolean centerBased) {
            isCenterBased = centerBased;
        }

        public boolean isCenterBased() {
            return isCenterBased;
        }

        public void setAntiAliasing(boolean useAntiAliasing) {
            this.useAntiAliasing = useAntiAliasing;
        }
    }