package com.esgdev.sparkpaint.engine.tools;

            import com.esgdev.sparkpaint.engine.DrawingCanvas;
            import com.esgdev.sparkpaint.engine.selection.Selection;
            import com.esgdev.sparkpaint.engine.selection.SelectionManager;

            import javax.swing.*;
            import java.awt.*;
            import java.awt.event.MouseEvent;
            import java.awt.event.MouseWheelEvent;
            import java.awt.image.BufferedImage;

            public class PencilTool implements DrawingTool {
                private final DrawingCanvas canvas;
                private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                private Point startPoint;
                private boolean useAntiAliasing = true;
                private boolean isDrawing = false;

                public PencilTool(DrawingCanvas canvas) {
                    this.canvas = canvas;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    // No action needed for mouse moved
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    //SelectionManager selectionManager = canvas.getSelectionManager();
                    Selection selection = canvas.getSelection();

                    // Convert screen point to world coordinates
                    Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

                    // If there's a selection with outline, only allow drawing inside it
                    if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
                        isDrawing = false;
                        return; // Don't draw outside selection
                    }

                    // Save start point and update canvas
                    startPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
                    isDrawing = true;
                    canvas.saveToUndoStack();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    //SelectionManager selectionManager = canvas.getSelectionManager();
                    Selection selection = canvas.getSelection();

                    // Convert screen point to world coordinates
                    Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

                    // Check if we're crossing a selection boundary
                    boolean isInsideSelection = selection == null || !selection.hasOutline() || selection.contains(worldPoint);

                    if (!isInsideSelection) {
                        // We're outside the selection, stop drawing
                        isDrawing = false;
                        return;
                    }

                    // If we weren't drawing before, but now we can, set a new start point
                    if (!isDrawing) {
                        startPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
                        isDrawing = true;
                        return;
                    }

                    // Get current point in appropriate coordinate system
                    Point point = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

                    // Get appropriate graphics context and draw
                    Graphics2D g2d;

                    if (selection != null && selection.hasOutline()) {
                        g2d = canvas.getDrawingGraphics();
                    } else {
                        // Draw on current layer
                        BufferedImage currentLayerImage = canvas.getCurrentLayerImage();
                        g2d = (Graphics2D) currentLayerImage.getGraphics();
                    }

                    g2d.setStroke(new BasicStroke(canvas.getLineThickness()));
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

                    if (SwingUtilities.isLeftMouseButton(e)) {
                        g2d.setColor(canvas.getDrawingColor());
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        g2d.setColor(canvas.getFillColor());
                    }

                    g2d.drawLine(startPoint.x, startPoint.y, point.x, point.y);
                    g2d.dispose();

                    startPoint = point;
                    canvas.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isDrawing = false;  // Reset drawing state when mouse is released
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
                    return "Pencil tool selected";
                }

                public void setAntiAliasing(boolean useAntiAliasing) {
                    this.useAntiAliasing = useAntiAliasing;
                }
            }