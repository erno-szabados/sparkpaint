package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PreviewDialog extends JDialog {
    private final PreviewPanel previewPanel;
    private final Timer updateTimer;

    public PreviewDialog(Frame owner, DrawingCanvas canvas, JScrollPane scrollPane) {
        super(owner, "Image Preview", false);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        previewPanel = new PreviewPanel(canvas, scrollPane);
        add(previewPanel);

        // Set initial size and position
        setSize(250, 200);
        setLocationRelativeTo(owner);

        // Setup timer to update preview
        updateTimer = new Timer(500, e -> previewPanel.updatePreview());
        updateTimer.setRepeats(false);

        // Add listeners to canvas and viewport changes
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePreview();
            }
        });

        scrollPane.getViewport().addChangeListener(e -> updatePreview());

        // Update when layers change
        canvas.addLayerChangeListener(() -> updatePreview());
    }

    public void updatePreview() {
        // Use timer to avoid too frequent updates during resizing/scrolling
        updateTimer.restart();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            previewPanel.updatePreview();
        }
    }
}