package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.*;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class DrawingSettingsToolBox extends Box implements ToolChangeListener {
    public static final int MaxWidth = 200;
    private final Map<DrawingCanvas.Tool, ToolSettings> toolSettings;
    private final Map<DrawingCanvas.Tool, JComponent> settingsPanels;
    private final JPanel settingsPanel;


    public DrawingSettingsToolBox(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(BoxLayout.Y_AXIS);
        this.toolSettings = new EnumMap<>(DrawingCanvas.Tool.class);
        this.settingsPanels = new EnumMap<>(DrawingCanvas.Tool.class);
        this.settingsPanel = new JPanel();
        this.settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(this.settingsPanel, BorderLayout.CENTER);

        initializeToolSettings(canvas);
        add(containerPanel);

        canvas.addToolChangeListener(this);
    }

    private void initializeToolSettings(DrawingCanvas canvas) {
        toolSettings.put(DrawingCanvas.Tool.RECTANGLE, new RectangleToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.CIRCLE, new CircleToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.LINE, new LineToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.PENCIL, new PencilToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.BRUSH, new BrushToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.FILL, new FillToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.RECTANGLE_SELECTION, new SelectionToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.TEXT, new TextToolSettings(canvas));
        toolSettings.put(DrawingCanvas.Tool.FREEHAND_SELECTION, new FreehandSelectionToolSettings(canvas));


        // Add other tool settings
    }

    @Override
    public void onToolChanged(DrawingCanvas.Tool newTool) {
        settingsPanel.removeAll();
        JComponent panel = settingsPanels.get(newTool);
        if (panel == null) {
            ToolSettings settings = toolSettings.get(newTool);
            if (settings != null) {
                panel = settings.createSettingsPanel();
                settingsPanels.put(newTool, panel);
            }
        }
        if (panel != null) {
            settingsPanel.add(panel);
        }
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }
}