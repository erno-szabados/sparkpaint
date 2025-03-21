package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
            import com.esgdev.sparkpaint.engine.Layer;
            import com.esgdev.sparkpaint.engine.LayerManager;

import javax.swing.*;
            import javax.swing.border.EmptyBorder;
            import java.awt.*;
            import java.awt.event.ActionEvent;
            import java.awt.event.MouseAdapter;
            import java.awt.event.MouseEvent;
import java.util.function.Consumer;

            public class LayerPanel extends JPanel {
                private final LayerManager layerManager;
                private final JList<Layer> layerList;
                private final StatusMessageHandler statusMessageHandler;
                private final DefaultListModel<Layer> listModel;

                public LayerPanel(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
                    this.layerManager = canvas.getLayerManager();
                    this.statusMessageHandler = statusMessageHandler;

                    setLayout(new BorderLayout());
                    setBorder(BorderFactory.createTitledBorder("Layers"));
                    setPreferredSize(new Dimension(200, 250));

                    // Create list model and list
                    listModel = new DefaultListModel<>();
                    layerList = new JList<>(listModel);
                    LayerListCellRenderer cellRenderer = new LayerListCellRenderer();
                    layerList.setCellRenderer(cellRenderer);

                    layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    layerList.setFixedCellHeight(36); // Set a reasonable cell height

                    // In the LayerPanel constructor, after initializing layerList
                    layerList.addListSelectionListener(e -> {
                        if (!e.getValueIsAdjusting()) {
                            int selectedIndex = layerList.getSelectedIndex();
                            if (selectedIndex != -1) {
                                // Convert visual index to model index (reverse order)
                                int modelIndex = listModel.getSize() - 1 - selectedIndex;

                                // Update current layer in LayerManager
                                layerManager.setCurrentLayerIndex(modelIndex);

                                // Update UI
                                Layer selectedLayer = layerManager.getLayers().get(layerManager.getCurrentLayerIndex());
                                statusMessageHandler.setStatusMessage("Selected layer: " + selectedLayer.getName());
                            }
                        }
                    });

                    // Scroll pane for the layers list
                    JScrollPane scrollPane = new JScrollPane(layerList);
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

                    // Mouse listener for delete button clicks
                    layerList.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            int index = layerList.locationToIndex(e.getPoint());
                            if (index != -1) {
                                Layer layer = listModel.getElementAt(index);
                                Rectangle cellBounds = layerList.getCellBounds(index, index);

                                // Get the cell renderer component for the given index
                                Component rendererComponent = layerList.getCellRenderer().getListCellRendererComponent(layerList, layer, index, false, false);

                                // Check if the renderer component is an instance of LayerListCellRenderer
                                if (rendererComponent instanceof LayerListCellRenderer) {
                                    LayerListCellRenderer renderer = (LayerListCellRenderer) rendererComponent;
                                    handleDeleteButton(e, renderer, cellBounds, layer, index);
                                    handleVisibilityCheckBox(e, renderer, cellBounds, layer, index);
                                }
                            }
                        }
                    });
                }

                private void handleVisibilityCheckBox(MouseEvent e, LayerListCellRenderer renderer, Rectangle cellBounds, Layer layer, int index) {
                    JCheckBox checkBox = renderer.getVisibilityCheckBox();
                    Rectangle bounds = checkBox.getBounds();
                    Point absoluteCheckBoxLocation = SwingUtilities.convertPoint(checkBox, 0, 0, renderer);
                    Rectangle checkBoxRect = new Rectangle(
                            cellBounds.x + absoluteCheckBoxLocation.x,
                            cellBounds.y + absoluteCheckBoxLocation.y,
                            bounds.width,
                            bounds.height
                    );
                    if (checkBoxRect.contains(e.getPoint())) {
                        checkBox.setSelected(!checkBox.isSelected());
                        layer.setVisible(checkBox.isSelected());
                        statusMessageHandler.setStatusMessage("Layer visibility toggled for " + layer.getName());
                        layerList.repaint();
                        layerManager.getCanvas().repaint();
                    }
                }

                private void handleDeleteButton(MouseEvent e, LayerListCellRenderer renderer, Rectangle cellBounds, Layer layer, int index) {
                    JButton deleteButton = renderer.getDeleteButton();

                    // Get the bounds of the delete button relative to the renderer
                    Rectangle deleteButtonBounds = deleteButton.getBounds();

                    // Convert the delete button's bounds to screen coordinates
                    Point absoluteDeleteButtonLocation = SwingUtilities.convertPoint(deleteButton, 0, 0, renderer);

                    // Create a rectangle for the delete button in the JList's coordinate system
                    Rectangle deleteButtonRect = new Rectangle(
                            cellBounds.x + absoluteDeleteButtonLocation.x,
                            cellBounds.y + absoluteDeleteButtonLocation.y,
                            deleteButtonBounds.width,
                            deleteButtonBounds.height
                    );

                    if (deleteButtonRect.contains(e.getPoint())) {
                        System.out.println("Delete button clicked for layer: " + layer.getName());
                        layerManager.deleteLayer(listModel.getSize() - 1 - index);
                        refreshLayerList();
                    }
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
                    listModel.clear();
                    listModel.addAll(layerManager.getLayers().reversed());
                    int index = layerManager.getLayerCount() - layerManager.getCurrentLayerIndex() - 1;
                    layerList.setSelectedIndex(index);
                    layerList.repaint();
                    layerList.ensureIndexIsVisible(index);
                }
            }