package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.*;

import javax.swing.*;
import java.util.EnumMap;
import java.util.Map;

public class DrawingSettingsToolBox extends Box implements ToolChangeListener {
    public static final int MaxWidth = 200;
    private final Map<DrawingCanvas.Tool, ToolSettings> toolSettings;
    private final JPanel settingsPanel;


    public DrawingSettingsToolBox(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(BoxLayout.Y_AXIS);
        this.toolSettings = new EnumMap<>(DrawingCanvas.Tool.class);
        this.settingsPanel = new JPanel();
        this.settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));

        initializeToolSettings(canvas);
        add(settingsPanel);

        canvas.addToolChangeListener(this);
    }

    private void initializeToolSettings(DrawingCanvas canvas) {
        toolSettings.put(DrawingCanvas.Tool.RECTANGLE, new RectangleToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.CIRCLE, new CircleToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.ELLIPSE, new EllipseToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.LINE, new LineToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.PENCIL, new PencilToolSettings(canvas));

        // Add other tool settings
    }

    @Override
    public void onToolChanged(DrawingCanvas.Tool newTool) {
        settingsPanel.removeAll();
        ToolSettings settings = toolSettings.get(newTool);
        if (settings != null) {
            settingsPanel.add(settings.createSettingsPanel());
        }
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }
}