package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.Layer;
import com.esgdev.sparkpaint.engine.LayerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;

public class LayerPanel extends JPanel {
    private final DrawingCanvas canvas;
    private final LayerManager layerManager;
    private final JPanel layersContainer;
    private final StatusMessageHandler statusMessageHandler;

    public LayerPanel(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        this.canvas = canvas;
        this.layerManager = canvas.getLayerManager();
        this.statusMessageHandler = statusMessageHandler;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Layers"));
        setPreferredSize(new Dimension(200, 250));

        // Panel to hold the list of layers
        layersContainer = new JPanel();
        layersContainer.setLayout(new BoxLayout(layersContainer, BoxLayout.Y_AXIS));

        // Scroll pane for the layers list
        JScrollPane scrollPane = new JScrollPane(layersContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 4, 5, 0)); // 1 row, 4 columns, with 5px horizontal gap



        JButton addButton = createButton(IconLoader.loadAndScaleIcon("add.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Add new layer", e -> addLayer());
        JButton deleteButton = createButton(IconLoader.loadAndScaleIcon("remove.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Remove layer", e -> deleteLayer());
        JButton moveUpButton = createButton(IconLoader.loadAndScaleIcon("arrow_up.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Move layer up", e -> moveLayerUp());
        JButton moveDownButton = createButton(IconLoader.loadAndScaleIcon("arrow_down.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Move layer down", e -> moveLayerDown());

        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(moveUpButton);
        buttonsPanel.add(moveDownButton);
        buttonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add components to this panel
        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        // Initial rendering of layers
        refreshLayerList();
    }

    private JButton createButton(Icon icon, String tooltip, Consumer<ActionEvent> action) {
        JButton button = new JButton(icon);
        button.addActionListener(action::accept);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        return button;
    }

    private void addLayer() {
        layerManager.addNewLayer();
        refreshLayerList();
        statusMessageHandler.setStatusMessage("New layer added");
    }

    private void deleteLayer() {
        if (layerManager.getLayerCount() <= 1) {
            statusMessageHandler.setStatusMessage("Cannot delete the only layer");
            return;
        }

        layerManager.deleteCurrentLayer();
        refreshLayerList();
        statusMessageHandler.setStatusMessage("Layer deleted");
    }

    private void moveLayerUp() {
        boolean success = layerManager.moveCurrentLayerUp();
        if (success) {
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer moved up");
        } else {
            statusMessageHandler.setStatusMessage("Cannot move layer further up");
        }
    }

    private void moveLayerDown() {
        boolean success = layerManager.moveCurrentLayerDown();
        if (success) {
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer moved down");
        } else {
            statusMessageHandler.setStatusMessage("Cannot move layer further down");
        }
    }

    public void refreshLayerList() {
        layersContainer.removeAll();

        List<Layer> layers = layerManager.getLayers();
        int currentLayerIndex = layerManager.getCurrentLayerIndex();

        // Add layers from top to bottom (reverse order)
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            final int layerIndex = i;

            JPanel layerPanel = new JPanel();
            layerPanel.setLayout(new BorderLayout());
            layerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    new EmptyBorder(3, 3, 3, 3)));

            // Visibility toggle checkbox
            JCheckBox visibilityCheckBox = new JCheckBox();
            visibilityCheckBox.setSelected(layer.isVisible());
            visibilityCheckBox.addActionListener(e -> {
                layer.setVisible(visibilityCheckBox.isSelected());
                canvas.repaint();
            });

            // Layer name and selection
            JPanel namePanel = new JPanel();
            namePanel.setLayout(new BorderLayout());
            JLabel nameLabel = new JLabel(layer.getName() + " " + (layerIndex + 1));
            namePanel.add(nameLabel, BorderLayout.CENTER);

            // Make the layer panel selectable
            Color defaultColor = layerPanel.getBackground();
            Color selectedColor = new Color(173, 216, 230); // Light blue

            if (layerIndex == currentLayerIndex) {
                layerPanel.setBackground(selectedColor);
            }

            layerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    layerManager.setCurrentLayerIndex(layerIndex);
                    refreshLayerList();
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (layerIndex != layerManager.getCurrentLayerIndex()) {
                        layerPanel.setBackground(defaultColor.darker());
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (layerIndex != layerManager.getCurrentLayerIndex()) {
                        layerPanel.setBackground(defaultColor);
                    }
                }
            });

            layerPanel.add(visibilityCheckBox, BorderLayout.WEST);
            layerPanel.add(namePanel, BorderLayout.CENTER);

            layersContainer.add(layerPanel);
        }

        layersContainer.revalidate();
        layersContainer.repaint();
    }
}