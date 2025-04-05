package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.CanvasChangeListener;
import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

public class PreviewPanel extends JPanel implements CanvasChangeListener {
    private static final int PREVIEW_WIDTH = 200;
    private static final int PREVIEW_HEIGHT = 150;
    private static final Color VIEWPORT_COLOR = new Color(0, 0, 255, 32);
    private static final Color VIEWPORT_BORDER_COLOR = new Color(0, 0, 255, 128);

    private final DrawingCanvas canvas;
    private final JScrollPane scrollPane;
    private BufferedImage previewImage;
    private final Rectangle viewportRect = new Rectangle();
    private boolean isDragging = false;

    public PreviewPanel(DrawingCanvas canvas, JScrollPane scrollPane) {
        this.canvas = canvas;
        this.scrollPane = scrollPane;
        setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateViewportPosition(e.getPoint());
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    updateViewportPosition(e.getPoint());
                }
            }
        });
        canvas.addCanvasChangeListener(this);
    }

    public void updatePreview() {
        // Get all visible layers
        BufferedImage canvasContent = createCompositeImage();

        // Scale to preview size
        float scaleX = (float) PREVIEW_WIDTH / canvasContent.getWidth();
        float scaleY = (float) PREVIEW_HEIGHT / canvasContent.getHeight();
        float scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (canvasContent.getWidth() * scale);
        int scaledHeight = (int) (canvasContent.getHeight() * scale);

        previewImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = previewImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(canvasContent, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        updateViewportRect();
        repaint();
    }

    private BufferedImage createCompositeImage() {
        int width = canvas.getLayers().get(0).getImage().getWidth();
        int height = canvas.getLayers().get(0).getImage().getHeight();

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        // Draw background
        g2d.setColor(canvas.getCanvasBackground());
        g2d.fillRect(0, 0, width, height);

        // Draw all visible layers
        for (int i = 0; i < canvas.getLayerCount(); i++) {
            if (canvas.getLayers().get(i).isVisible()) {
                g2d.drawImage(canvas.getLayers().get(i).getImage(), 0, 0, null);
            }
        }

        g2d.dispose();
        return composite;
    }

    private void updateViewportRect() {
        if (previewImage == null) return;

        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        float zoomFactor = canvas.getZoomFactor();

        float scaleX = (float) previewImage.getWidth() / (canvas.getLayers().get(0).getImage().getWidth() * zoomFactor);
        float scaleY = (float) previewImage.getHeight() / (canvas.getLayers().get(0).getImage().getHeight() * zoomFactor);

        int x = (int) (viewRect.x * scaleX);
        int y = (int) (viewRect.y * scaleY);
        int width = (int) (viewRect.width * scaleX);
        int height = (int) (viewRect.height * scaleY);

        viewportRect.setBounds(x, y, width, height);
    }

    private void updateViewportPosition(Point point) {
        if (previewImage == null) return;

        // Calculate the center position of the viewport
        int centerX = point.x;
        int centerY = point.y;

        // Calculate the scroll position based on the preview scale
        float zoomFactor = canvas.getZoomFactor();
        float scaleX = (float) (canvas.getLayers().get(0).getImage().getWidth() * zoomFactor) / previewImage.getWidth();
        float scaleY = (float) (canvas.getLayers().get(0).getImage().getHeight() * zoomFactor) / previewImage.getHeight();

        int scrollX = (int) (centerX * scaleX - (float) scrollPane.getViewport().getWidth() / 2);
        int scrollY = (int) (centerY * scaleY - (float) scrollPane.getViewport().getHeight() / 2);

        // Ensure scroll position is within bounds
        scrollX = Math.max(0, Math.min(scrollX,
                (int) (canvas.getLayers().get(0).getImage().getWidth() * zoomFactor - scrollPane.getViewport().getWidth())));
        scrollY = Math.max(0, Math.min(scrollY,
                (int) (canvas.getLayers().get(0).getImage().getHeight() * zoomFactor - scrollPane.getViewport().getHeight())));

        // Update the scroll position
        scrollPane.getViewport().setViewPosition(new Point(scrollX, scrollY));

        // Update the viewport rectangle
        updateViewportRect();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw gray background
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (previewImage == null) {
            return;
        }

        // Center the preview image
        int x = (getWidth() - previewImage.getWidth()) / 2;
        int y = (getHeight() - previewImage.getHeight()) / 2;

        g2d.drawImage(previewImage, x, y, null);

        BufferedImage layerImage = canvas.getLayers().get(0).getImage();
        // Only draw viewport rectangle if canvas is larger than preview
        if (!(layerImage.getWidth() * canvas.getZoomFactor() > PREVIEW_WIDTH) &&
                !(layerImage.getHeight() * canvas.getZoomFactor() > PREVIEW_HEIGHT)) {
            return;
        }

        // Draw viewport rectangle
        Rectangle adjustedRect = new Rectangle(
                x + viewportRect.x,
                y + viewportRect.y,
                viewportRect.width,
                viewportRect.height
        );

        g2d.setColor(VIEWPORT_COLOR);
        g2d.fill(adjustedRect);
        g2d.setColor(VIEWPORT_BORDER_COLOR);
        g2d.draw(adjustedRect);
    }

    @Override
    public void onCanvasChanged() {
        updatePreview();
    }
}