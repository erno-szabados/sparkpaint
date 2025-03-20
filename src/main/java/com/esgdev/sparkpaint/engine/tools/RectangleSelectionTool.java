package com.esgdev.sparkpaint.engine.tools;

                import com.esgdev.sparkpaint.engine.DrawingCanvas;
                import com.esgdev.sparkpaint.engine.Layer;
                import com.esgdev.sparkpaint.engine.selection.PathSelection;
                import com.esgdev.sparkpaint.engine.selection.Selection;

                import java.awt.*;
                import java.awt.event.MouseEvent;
                import java.awt.event.MouseWheelEvent;
                import java.awt.geom.AffineTransform;
                import java.awt.geom.GeneralPath;
                import java.awt.image.BufferedImage;
                import java.util.List;

                public class RectangleSelectionTool extends AbstractSelectionTool {
                    private Point worldDragOffset = null;

                    public RectangleSelectionTool(DrawingCanvas canvas) {
                        super(canvas);
                    }

                    @Override
                    protected boolean isValidSelectionType(Selection selection) {
                        return selection instanceof PathSelection;
                    }

                    @Override
                    protected void handleSelectionStart(MouseEvent e) {
                        Selection selection = selectionManager.getSelection();
                        if (selection == null) {
                            selection = new PathSelection(new Rectangle(), null);
                            selectionManager.setSelection(selection);
                        }

                        if (!selection.hasOutline()) {
                            // Start new rectangle selection
                            startNewRectangle();
                        } else if (selection.contains(worldStartPoint)) {
                            // Start dragging existing selection
                            startDragging(selection);
                        } else {
                            // Start new rectangle at different location
                            startNewRectangle();
                        }
                    }

                    private void startNewRectangle() {
                        Rectangle initialRect = new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0);
                        Selection selection = PathSelection.createRectangular(initialRect, null);
                        selectionManager.setSelection(selection);
                        originalSelectionLocation = null;
                    }

                    private void startDragging(Selection selection) {
                        isDragging = true;
                        Rectangle selectionRectangle = selection.getBounds();
                        worldDragOffset = new Point(
                                worldStartPoint.x - selectionRectangle.x,
                                worldStartPoint.y - selectionRectangle.y);

                        // Only set originalSelectionLocation if it hasn't been set yet
                        if (originalSelectionLocation == null) {
                            originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Selection selection = selectionManager.getSelection();

                        Rectangle selectionRectangle = selection.getBounds();
                        if (selectionRectangle == null) return;

                        Point worldEndPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

                        if (isDragging) {
                            finalizeDrag(worldEndPoint, selection);
                        } else {
                            finalizeNewSelection(selection, selectionRectangle);
                        }

                        canvas.repaint();
                    }

                    private void finalizeDrag(Point worldEndPoint, Selection selection) {
                        isDragging = false;
                        if (selection.contains(worldEndPoint)) {
                            canvas.setCursor(handCursor);
                        } else {
                            canvas.setCursor(crosshairCursor);
                        }
                    }

                    private void finalizeNewSelection(Selection selection, Rectangle selectionRectangle) {
                        canvas.notifyClipboardStateChanged();

                        if (selectionRectangle.width > 0 && selectionRectangle.height > 0) {
                            BufferedImage selectionContent = createSelectionImage(selectionRectangle);

                            // Apply transparency if enabled
                            if (transparencyEnabled) {
                                applyTransparencyToContent(selectionContent, canvas.getFillColor());
                            }
                            selection.setTransparent(transparencyEnabled);

                            selection.setContent(selectionContent);
                            originalSelectionLocation = new Point(selectionRectangle.x, selectionRectangle.y);
                            clearSelectionOriginalLocation((transparencyEnabled ? canvas.getFillColor() : canvas.getCanvasBackground()));
                        } else {
                            selection.setContent(null);
                            originalSelectionLocation = null;
                        }
                    }

                    private BufferedImage createSelectionImage(Rectangle selectionRectangle) {
                        BufferedImage selectionContent = new BufferedImage(
                                selectionRectangle.width,
                                selectionRectangle.height,
                                BufferedImage.TYPE_INT_ARGB);

                        Graphics2D g2d = selectionContent.createGraphics();

                        // Draw the composite of all visible layers instead of just the canvas image
                        List<Layer> layers = canvas.getLayerManager().getLayers();
                        for (int i = 0; i < layers.size(); i++) {
                            Layer layer = layers.get(i);
                            if (layer.isVisible()) {
                                g2d.drawImage(layer.getImage(),
                                        -selectionRectangle.x,
                                        -selectionRectangle.y,
                                        null);
                            }
                        }

                        g2d.dispose();

                        return selectionContent;
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
                        PathSelection selection = (PathSelection) selectionManager.getSelection();

                        Rectangle selectionRectangle = selection.getBounds();
                        if (selectionRectangle == null) return;

                        if (isDragging) {
                            updateRectangleLocation(worldDragPoint, selection);
                        } else {
                            // size the rectangle
                            updateRectangleSize(worldDragPoint, selection);
                        }

                        canvas.repaint();
                    }

                    private void updateRectangleLocation(Point worldDragPoint, PathSelection selection) {
                        int newX = worldDragPoint.x - worldDragOffset.x;
                        int newY = worldDragPoint.y - worldDragOffset.y;
                        GeneralPath path = selection.getPath();
                        Rectangle bounds = path.getBounds();
                        int deltaX = newX - bounds.x;
                        int deltaY = newY - bounds.y;
                        AffineTransform transform = AffineTransform.getTranslateInstance(deltaX, deltaY);
                        path.transform(transform);
                    }

                    private void updateRectangleSize(Point worldDragPoint, PathSelection selection) {
                        int x = Math.min(worldStartPoint.x, worldDragPoint.x);
                        int y = Math.min(worldStartPoint.y, worldDragPoint.y);
                        int width = Math.abs(worldDragPoint.x - worldStartPoint.x);
                        int height = Math.abs(worldDragPoint.y - worldStartPoint.y);
                        Rectangle rect = new Rectangle(x, y, width, height);
                        GeneralPath path = selection.getPath();
                        path.reset();
                        path.append(rect, false);
                    }

                    @Override
                    public void mouseScrolled(MouseWheelEvent e) {
                        // Handle zooming if needed
                    }

                    @Override
                    public String statusMessage() {
                        return "Selection tool selected";
                    }

                    @Override
                    protected void clearSelectionOriginalLocation(Color color) {
                        Selection selection = selectionManager.getSelection();
                        if (originalSelectionLocation == null) {
                            return;
                        }

                        Rectangle selectionRectangle = selection.getBounds();
                        if (selectionRectangle == null) return;

                        // Get the current layer and save to undo stack
                        canvas.saveToUndoStack();
                        BufferedImage currentLayer = canvas.getLayerManager().getCurrentLayerImage();
                        Graphics2D g2d = currentLayer.createGraphics();

                        Rectangle originalRect = new Rectangle(
                                originalSelectionLocation.x,
                                originalSelectionLocation.y,
                                selectionRectangle.width,
                                selectionRectangle.height);
                        g2d.setColor(color);
                        g2d.fill(originalRect);
                        g2d.dispose();
                    }

                    @Override
                    protected void drawSelectionToCanvas(Graphics2D g2d, Selection selection, BufferedImage content) {
                        Rectangle selectionRectangle = selection.getBounds();
                        g2d.drawImage(content, selectionRectangle.x, selectionRectangle.y, null);
                    }
                }