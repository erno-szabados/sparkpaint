package com.esgdev.sparkpaint.engine.tools;

        import com.esgdev.sparkpaint.engine.DrawingCanvas;
        import com.esgdev.sparkpaint.engine.selection.RectangleSelection;
        import com.esgdev.sparkpaint.engine.selection.Selection;

        import java.awt.*;
        import java.awt.event.MouseEvent;
        import java.awt.event.MouseWheelEvent;
        import java.awt.image.BufferedImage;

        public class RectangleSelectionTool extends AbstractSelectionTool {
            private Point worldDragOffset = null;

            public RectangleSelectionTool(DrawingCanvas canvas) {
                super(canvas);
            }

            @Override
            protected boolean isValidSelectionType(Selection selection) {
                return selection instanceof RectangleSelection;
            }

            @Override
            protected void handleSelectionStart(MouseEvent e) {
                Selection selection = selectionManager.getSelection();

                if (!(selection instanceof RectangleSelection) || !selection.hasOutline()) {
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
                Selection selection = new RectangleSelection(
                        new Rectangle(worldStartPoint.x, worldStartPoint.y, 0, 0), null);
                selectionManager.setSelection(selection);
                originalSelectionLocation = null;
            }

            private void startDragging(Selection selection) {
                isDragging = true;
                Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
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
                if (!(selection instanceof RectangleSelection)) {
                    return;
                }

                Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
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
                g2d.drawImage(canvas.getImage(),
                        -selectionRectangle.x,
                        -selectionRectangle.y,
                        null);
                g2d.dispose();

                return selectionContent;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point worldDragPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());
                Selection selection = selectionManager.getSelection();

                if (!(selection instanceof RectangleSelection)) {
                    return;
                }

                Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
                if (selectionRectangle == null) return;

                if (isDragging) {
                    updateRectangleLocation(worldDragPoint, selectionRectangle);
                } else {
                    updateRectangleSize(worldDragPoint, selectionRectangle);
                }

                canvas.repaint();
            }

            private void updateRectangleLocation(Point worldDragPoint, Rectangle selectionRectangle) {
                int newX = worldDragPoint.x - worldDragOffset.x;
                int newY = worldDragPoint.y - worldDragOffset.y;
                selectionRectangle.setLocation(newX, newY);
            }

            private void updateRectangleSize(Point worldDragPoint, Rectangle selectionRectangle) {
                int x = Math.min(worldStartPoint.x, worldDragPoint.x);
                int y = Math.min(worldStartPoint.y, worldDragPoint.y);
                int width = Math.abs(worldDragPoint.x - worldStartPoint.x);
                int height = Math.abs(worldDragPoint.y - worldStartPoint.y);
                selectionRectangle.setBounds(x, y, width, height);
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
                if (!(selection instanceof RectangleSelection) || originalSelectionLocation == null) {
                    return;
                }

                Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
                if (selectionRectangle == null) return;

                canvas.saveToUndoStack();
                Graphics2D g2d = canvas.getImage().createGraphics();
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
                Rectangle selectionRectangle = ((RectangleSelection) selection).getRectangle();
                g2d.drawImage(content, selectionRectangle.x, selectionRectangle.y, null);
            }
        }