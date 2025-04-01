package com.esgdev.sparkpaint.engine.tools;

                import com.esgdev.sparkpaint.engine.DrawingCanvas;
                import com.esgdev.sparkpaint.engine.selection.Selection;

                import java.awt.*;
                import java.awt.event.MouseEvent;
                import java.awt.event.MouseWheelEvent;
                import java.awt.font.FontRenderContext;
                import java.awt.font.TextLayout;
                import java.awt.image.BufferedImage;

                public class TextTool implements DrawingTool {
                    private final DrawingCanvas canvas;
                    private final Cursor cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
                    private String text = "Sample Text";
                    private Font font = new Font("Arial", Font.PLAIN, 24);
                    private boolean useAntiAliasing = true;
                    private Point previewPoint = null;

                    public TextTool(DrawingCanvas canvas) {
                        this.canvas = canvas;
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        // Update preview position
                        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

                        // Check if there's a selection and we're inside it
                        Selection selection = canvas.getSelection();
                        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
                            // Clear preview if outside selection
                            clearPreview();
                            return;
                        }

                        // Update preview position
                        previewPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());
                        updatePreview();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        canvas.saveToUndoStack();
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        // No action needed for mouse dragged
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Selection selection = canvas.getSelection();

                        // Convert screen point to world coordinates
                        Point worldPoint = DrawingTool.screenToWorld(canvas.getZoomFactor(), e.getPoint());

                        // If there's a selection, only proceed if clicking inside it
                        if (selection != null && selection.hasOutline() && !selection.contains(worldPoint)) {
                            return; // Don't draw outside selection when one exists
                        }

                        // Get the point in the appropriate coordinate system
                        Point drawPoint = canvas.getDrawingCoordinates(e.getPoint(), canvas.getZoomFactor());

                        // Check if using transparency
                        boolean isTransparent = canvas.getDrawingColor().getAlpha() == 0;

                        if (isTransparent) {
                            drawTransparentText(drawPoint);
                        } else {
                            drawNormalText(drawPoint);
                        }

                        // Clear preview
                        clearPreview();
                        canvas.repaint();
                    }

                    private void drawNormalText(Point drawPoint) {
                        Selection selection = canvas.getSelection();
                        Graphics2D g2d;
                        Point adjustedDrawPoint = drawPoint;

                        if (selection != null && selection.hasOutline()) {
                            // Get drawing graphics from the selection manager
                            g2d = canvas.getDrawingGraphics();
                            selection.setModified(true);

                            // Get selection bounds to adjust coordinates
                            Rectangle bounds = selection.getBounds();

                            // Adjust coordinates relative to the selection bounds
                            adjustedDrawPoint = new Point(drawPoint.x - bounds.x, drawPoint.y - bounds.y);
                        } else {
                            // Draw on current layer
                            g2d = canvas.getCurrentLayerImage().createGraphics();
                        }

                        // Configure and draw text
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
                        g2d.setFont(font);
                        g2d.setColor(canvas.getDrawingColor());
                        g2d.drawString(text, adjustedDrawPoint.x, adjustedDrawPoint.y);
                        g2d.dispose();
                    }

                    private void drawTransparentText(Point drawPoint) {
                        Selection selection = canvas.getSelection();
                        BufferedImage targetImage;
                        Point adjustedDrawPoint;

                        if (selection != null && selection.hasOutline()) {
                            targetImage = selection.getContent();
                            Rectangle bounds = selection.getBounds();
                            adjustedDrawPoint = new Point(drawPoint.x - bounds.x, drawPoint.y - bounds.y);
                            selection.setModified(true);
                        } else {
                            targetImage = canvas.getCurrentLayerImage();
                            adjustedDrawPoint = drawPoint;
                        }

                        // Create a mask image for transparency
                        BufferedImage maskImage = new BufferedImage(targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D maskG2d = maskImage.createGraphics();

                        // Apply same rendering hints
                        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

                        // Draw text onto mask
                        maskG2d.setFont(font);
                        maskG2d.setColor(Color.WHITE);
                        maskG2d.drawString(text, adjustedDrawPoint.x, adjustedDrawPoint.y);
                        maskG2d.dispose();

                        // Apply transparency where mask is white
                        Graphics2D g2d = (Graphics2D) targetImage.getGraphics();
                        applyTransparencyMask(targetImage, maskImage, g2d.getClip());
                        g2d.dispose();
                    }

                    /**
                     * Applies transparency to pixels in the image where the mask is non-zero
                     */
                    private void applyTransparencyMask(BufferedImage image, BufferedImage maskImage, Shape clip) {
                        // Apply transparency to pixels where the mask is non-zero
                        for (int y = 0; y < image.getHeight(); y++) {
                            for (int x = 0; x < image.getWidth(); x++) {
                                // Check if this pixel is within clip region
                                if (clip == null || clip.contains(x, y)) {
                                    int maskRGB = maskImage.getRGB(x, y);
                                    // Only process pixels where the mask is non-zero
                                    if ((maskRGB & 0xFF000000) != 0) {
                                        // Set full transparency (alpha = 0)
                                        int newRGB = image.getRGB(x, y) & 0x00FFFFFF;
                                        image.setRGB(x, y, newRGB);
                                    }
                                }
                            }
                        }
                    }

                    private void updatePreview() {
                        if (previewPoint == null) return;

                        // Create temporary canvas for preview
                        BufferedImage tempCanvas = canvas.getToolCanvas();
                        Graphics2D g2d = tempCanvas.createGraphics();

                        // Clear the temp canvas
                        g2d.setComposite(AlphaComposite.Clear);
                        g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                        g2d.setComposite(AlphaComposite.SrcOver);

                        // Apply rendering settings
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

                        // Apply selection clip if needed
                        Selection selection = canvas.getSelection();
                        applySelectionClip(g2d, selection);

                        // Calculate adjusted draw point for selection
                        Point adjustedPoint = previewPoint;
                        if (selection != null && selection.hasOutline()) {
                            Rectangle bounds = selection.getBounds();
                            adjustedPoint = new Point(previewPoint.x, previewPoint.y);
                        }

                        // Check if we're using transparent color
                        boolean isTransparent = canvas.getDrawingColor().getAlpha() == 0;

                        if (isTransparent) {
                            // Draw dotted/dashed outline for transparent text
                            g2d.setFont(font);

                            // Create a TextLayout to get the outline shape
                            FontRenderContext frc = g2d.getFontRenderContext();
                            TextLayout textLayout = new TextLayout(text, font, frc);
                            Shape textShape = textLayout.getOutline(null);

                            // Translate to the correct position
                            g2d.translate(adjustedPoint.x, adjustedPoint.y);

                            // Draw the outline with a dashed pattern
                            float[] dashPattern = {3.0f, 3.0f};
                            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

                            // Draw white line first
                            g2d.setColor(Color.WHITE);
                            g2d.draw(textShape);

                            // Draw black line with offset dash pattern
                            g2d.setColor(Color.BLACK);
                            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER, 10.0f, dashPattern, dashPattern[0]));
                            g2d.draw(textShape);
                        } else {
                            // For non-transparent text, show the text with semi-transparency
                            Color previewColor = canvas.getDrawingColor();
                            g2d.setColor(new Color(previewColor.getRed(), previewColor.getGreen(),
                                    previewColor.getBlue(), 128));
                            g2d.setFont(font);
                            g2d.drawString(text, adjustedPoint.x, adjustedPoint.y);
                        }

                        g2d.dispose();
                        canvas.repaint();
                    }

                    private void clearPreview() {
                        previewPoint = null;
                        BufferedImage tempCanvas = canvas.getToolCanvas();
                        if (tempCanvas != null) {
                            Graphics2D g2d = tempCanvas.createGraphics();
                            g2d.setComposite(AlphaComposite.Clear);
                            g2d.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
                            g2d.dispose();
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
                        return "Text tool selected";
                    }

                    public void setText(String text) {
                        this.text = text;
                        updatePreview();
                    }

                    public void setFont(Font font) {
                        this.font = font;
                        updatePreview();
                    }

                    public void setAntiAliasing(boolean useAntiAliasing) {
                        this.useAntiAliasing = useAntiAliasing;
                        updatePreview();
                    }
                }