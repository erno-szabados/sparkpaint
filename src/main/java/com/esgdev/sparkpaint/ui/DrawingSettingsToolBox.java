package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;

public class DrawingSettingsToolBox extends javax.swing.Box implements ToolChangeListener {

    public static final int MaxWidth = 200;
    public static final int MaxHeight = 1000;
    private static final int IconWidth = 24;
    private static final int IconHeight = 24;
    private final DrawingCanvas canvas;
    private final StatusMessageHandler statusMessageHandler;
    private Box lineThicknessBox;

    public DrawingSettingsToolBox(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        super(BoxLayout.Y_AXIS);
        this.canvas = canvas;
        this.statusMessageHandler = statusMessageHandler;
        canvas.addToolChangeListener(this);
        initializeToolBox();
    }

    private void initializeToolBox() {
        lineThicknessBox = createLineThicknessBox();
        this.add(lineThicknessBox);
        setPreferredSize(new Dimension(200, 0));
    }

    private Box createLineThicknessBox() {
        // Create a panel to hold the slider and the line sample
        Box box = Box.createHorizontalBox();
        //CompoundBorder border = BorderFactory.createCompoundBorder(new EmptyBorder(2,14,2,14), new LineBorder(Color.GRAY, 1, true));
        //box.setBorder(border);

        // Create a vertical JSlider for line thickness
        JSlider lineThicknessSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, (int) canvas.getLineThickness());
        lineThicknessSlider.setMajorTickSpacing(5);
        lineThicknessSlider.setMinorTickSpacing(1);
        lineThicknessSlider.setPaintTicks(true);
        lineThicknessSlider.setPaintLabels(false);
        lineThicknessSlider.setAlignmentX(Component.CENTER_ALIGNMENT); // Center-align the slider
        lineThicknessSlider.setMaximumSize(new Dimension(150, IconHeight + 28));
        box.add(lineThicknessSlider);

        //box.add(Box.createVerticalStrut(20));
        // Create a JLabel to render the line sample
        JLabel lineSampleLabel = getLineSampleLabel();
        box.add(lineSampleLabel);

        // Add a listener to update the line thickness and repaint the line sample
        lineThicknessSlider.addChangeListener(e -> {
            int thickness = lineThicknessSlider.getValue();
            canvas.setLineThickness(thickness);
            lineSampleLabel.setIcon(getLineIcon(Color.BLACK, thickness));
            lineSampleLabel.repaint();
        });

        return box;
    }

    private JLabel getLineSampleLabel() {
        JLabel lineSampleLabel = new JLabel();
        lineSampleLabel.setIcon(getLineIcon(Color.BLACK, canvas.getLineThickness()));
        lineSampleLabel.setPreferredSize(new Dimension(IconWidth, IconHeight));
        lineSampleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lineSampleLabel;
    }

    private Icon getLineIcon(Color color, float thickness) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g;
                g.setColor(color); // Assuming canvas provides the current color
                g2d.setStroke(new BasicStroke(thickness));
                int centerY = y + IconHeight / 2; // Center the line vertically.
                g2d.drawLine(x, centerY, x + IconWidth, centerY);
            }

            @Override
            public int getIconWidth() {
                return IconWidth;
            }

            @Override
            public int getIconHeight() {
                return IconHeight;
            }
        };
    }

    @Override
    public void onToolChanged(DrawingCanvas.Tool newTool) {
        updateSettingsForTool(newTool);
    }

    private void updateSettingsForTool(DrawingCanvas.Tool tool) {
        // TODO tools will provide their own settings
        lineThicknessBox.setVisible(true);

        revalidate();
        repaint();
    }

}
