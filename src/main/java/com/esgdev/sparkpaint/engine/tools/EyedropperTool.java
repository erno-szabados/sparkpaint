package com.esgdev.sparkpaint.engine.tools;

    import com.esgdev.sparkpaint.engine.DrawingCanvas;
    import com.esgdev.sparkpaint.engine.Layer;

    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.MouseEvent;
    import java.awt.event.MouseWheelEvent;
    import java.awt.image.BufferedImage;
    import java.util.List;

    public class EyedropperTool implements DrawingTool {
        private final DrawingCanvas canvas;
        private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

        public EyedropperTool(DrawingCanvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // No action needed for mouse moved
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Point point = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

            // Get all layers from the layer manager
            List<Layer> layers = canvas.getLayerManager().getLayers();
            int currentLayerIndex = canvas.getLayerManager().getCurrentLayerIndex();

            // Start with current layer
            Color pickedColor = null;

            // First try current layer
            pickedColor = getColorFromLayer(layers.get(currentLayerIndex), point);

            // If transparent, search through other visible layers from top to bottom
            if (pickedColor == null || pickedColor.getAlpha() == 0) {
                // First check layers above current layer (from top to current)
                for (int i = layers.size() - 1; i >= 0; i--) {
                    if (i == currentLayerIndex) continue; // Skip current layer (already checked)

                    Layer layer = layers.get(i);
                    if (!layer.isVisible()) continue;

                    Color color = getColorFromLayer(layer, point);
                    if (color != null && color.getAlpha() > 0) {
                        pickedColor = color;
                        break;
                    }
                }
            }

            // If still transparent or null, don't update colors
            if (pickedColor == null || pickedColor.getAlpha() == 0) {
                return;
            }

            if (SwingUtilities.isLeftMouseButton(e)) {
                canvas.setDrawingColor(pickedColor);
            } else if (SwingUtilities.isRightMouseButton(e)) {
                canvas.setFillColor(pickedColor);
            }
        }

        private Color getColorFromLayer(Layer layer, Point point) {
            if (!layer.isVisible()) return null;

            BufferedImage layerImage = layer.getImage();
            if (layerImage == null || point.x < 0 || point.y < 0 ||
                    point.x >= layerImage.getWidth() || point.y >= layerImage.getHeight()) {
                return null;
            }

            int rgb = layerImage.getRGB(point.x, point.y);
            return new Color(rgb, true);
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
            // No action needed for mouse scroll
        }

        @Override
        public void setCursor() {
            canvas.setCursor(cursor);
        }

        @Override
        public String statusMessage() {
            return "Eyedropper tool selected";
        }
    }