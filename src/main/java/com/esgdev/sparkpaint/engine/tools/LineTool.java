package com.esgdev.sparkpaint.engine.tools;

    import com.esgdev.sparkpaint.engine.DrawingCanvas;

    import java.awt.*;
    import java.awt.event.MouseEvent;
    import java.awt.image.BufferedImage;

    public class LineTool implements DrawingTool {
        private final DrawingCanvas canvas;
        private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        private Point startPoint;

        public LineTool(DrawingCanvas canvas) {
            this.canvas = canvas;
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
            g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
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
            g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
            canvas.setTempCanvas(null);
            canvas.repaint();
        }

        @Override
        public void setCursor() {
            canvas.setCursor(cursor);
        }

        @Override
        public String statusMessage() {
            return "Line tool selected";
        }
    }