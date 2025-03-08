package com.esgdev.sparkpaint.engine.tools;

    import com.esgdev.sparkpaint.engine.DrawingCanvas;

    import java.awt.*;
    import java.awt.event.MouseEvent;
    import java.awt.image.BufferedImage;

    public class EllipseTool implements DrawingTool {
        private final DrawingCanvas canvas;
        private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        private Point startPoint;
        private boolean isFilled;

        public EllipseTool(DrawingCanvas canvas) {
            this.canvas = canvas;
            this.isFilled = true;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // No action needed for mouse moved
        }

        @Override
        public void mousePressed(MouseEvent e) {
            startPoint = e.getPoint();
            canvas.saveToUndoStack();
            canvas.saveCanvasState();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point point = e.getPoint();
            BufferedImage tempCanvas = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = tempCanvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(canvas.getImage(), 0, 0, null);
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setColor(canvas.getDrawingColor());
            int x = Math.min(startPoint.x, point.x);
            int y = Math.min(startPoint.y, point.y);
            int width = Math.abs(point.x - startPoint.x);
            int height = Math.abs(point.y - startPoint.y);
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(x, y, width, height);
            }
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, width, height);
            g2d.dispose();
            canvas.setTempCanvas(tempCanvas);
            canvas.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Point point = e.getPoint();
            Graphics2D g2d = canvas.getCanvasGraphics();
            if (g2d == null) {
                System.out.println("Graphics is null");
                return;
            }
            g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
            g2d.setColor(canvas.getDrawingColor());
            int x = Math.min(startPoint.x, point.x);
            int y = Math.min(startPoint.y, point.y);
            int width = Math.abs(point.x - startPoint.x);
            int height = Math.abs(point.y - startPoint.y);
            if (isFilled) {
                g2d.setColor(canvas.getFillColor());
                g2d.fillOval(x, y, width, height);
            }
            g2d.setColor(canvas.getDrawingColor());
            g2d.drawOval(x, y, width, height);
            canvas.setTempCanvas(null);
            canvas.repaint();
        }

        @Override
        public void setCursor() {
            canvas.setCursor(cursor);
        }

        @Override
        public String statusMessage() {
            return "Ellipse tool selected";
        }

        public void setFilled(boolean selected) {
            this.isFilled = selected;
        }
    }