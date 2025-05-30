package com.esgdev.sparkpaint.ui.layer;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.layer.LayerChangeListener;
import com.esgdev.sparkpaint.engine.layer.LayerManager;
import com.esgdev.sparkpaint.ui.DrawingToolbar;
import com.esgdev.sparkpaint.ui.IconLoader;
import com.esgdev.sparkpaint.ui.StatusMessageHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class LayerPanel extends JPanel implements LayerChangeListener {
    private final JList<Layer> layerList;
    private final StatusMessageHandler statusMessageHandler;
    private final DefaultListModel<Layer> listModel;
    private final DrawingCanvas canvas;

    public LayerPanel(DrawingCanvas canvas, StatusMessageHandler statusMessageHandler) {
        this.statusMessageHandler = statusMessageHandler;
        this.canvas = canvas;

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

        layerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = layerList.getSelectedIndex();
                if (selectedIndex != -1) {
                    // Convert visual index to model index (reverse order)
                    int modelIndex = listModel.getSize() - 1 - selectedIndex;

                    // Update current layer in LayerManager
                    canvas.setCurrentLayerIndex(modelIndex);

                    // Update UI
                    Layer selectedLayer = canvas.getLayers().get(canvas.getCurrentLayerIndex());
                    statusMessageHandler.setStatusMessage("Selected layer: " + selectedLayer.getName());
                }
            }
        });
        setupDragAndDrop();

        // Scroll pane for the layers list
        JScrollPane scrollPane = new JScrollPane(layerList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 4, 5, 0)); // 1 row, 4 columns, with 5px horizontal gap

        JButton addButton = createButton(IconLoader.loadAndScaleIcon("add.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Add new layer", e -> addLayer());
        JButton duplicateButton = createButton(IconLoader.loadAndScaleIcon("copy.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Duplicate layer", e -> duplicateLayer());
        JButton flattenButton = createButton(IconLoader.loadAndScaleIcon("flatten.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Flatten all layers", e -> flattenLayers());
        JButton mergeDownButton = createButton(IconLoader.loadAndScaleIcon("layers.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight)
                , "Merge with layer below", e -> mergeLayerDown());

        buttonsPanel.add(addButton);
        buttonsPanel.add(duplicateButton);
        buttonsPanel.add(mergeDownButton);
        buttonsPanel.add(flattenButton);
        buttonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create a compound panel for all controls
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Name editing panel
        JPanel nameEditPanel = new JPanel(new BorderLayout(5, 0));
        JTextField nameField = new JTextField();
        JButton applyButton = createButton(
                IconLoader.loadAndScaleIcon("done.png", DrawingToolbar.IconWidth, DrawingToolbar.IconHeight),
                "Apply name change", e -> applyNameChange(nameField.getText()));

        // Set current layer name in field when selection changes
        layerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && layerList.getSelectedIndex() != -1) {
                int selectedIndex = layerList.getSelectedIndex();
                // Convert visual index to model index (reverse order)
                int modelIndex = listModel.getSize() - 1 - selectedIndex;
                Layer selectedLayer = canvas.getLayers().get(modelIndex);
                nameField.setText(selectedLayer.getName());
            }
        });
        // Apply changes when Enter is pressed
        nameField.addActionListener(e -> applyNameChange(nameField.getText()));

        nameEditPanel.add(new JLabel("Name: "), BorderLayout.WEST);
        nameEditPanel.add(nameField, BorderLayout.CENTER);
        nameEditPanel.add(applyButton, BorderLayout.EAST);
        nameEditPanel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Add some padding below

        // Add both panels to the compound panel
        controlsPanel.add(nameEditPanel, BorderLayout.NORTH);
        controlsPanel.add(buttonsPanel, BorderLayout.CENTER);

        // Add compound panel to the main panel
        add(scrollPane, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

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
        canvas.addLayerChangeListener(this);
    }

    /**
     * Refreshes the layer list by clearing and re-adding all layers in reverse order.
     * Also selects the current layer based on the current layer index.
     */
    public void refreshLayerList() {
        listModel.clear();
        java.util.List<Layer> layers = canvas.getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            listModel.addElement(layers.get(i));
        }
        int index = canvas.getLayerCount() - canvas.getCurrentLayerIndex() - 1;
        layerList.setSelectedIndex(index);
        layerList.repaint();
        layerList.ensureIndexIsVisible(index);
    }

    /**
     * Handles the visibility checkbox click event.
     * Toggles the layer's visibility and updates the status message.
     *
     * @param e          The mouse event
     * @param renderer   The cell renderer for the layer list
     * @param cellBounds The bounds of the cell
     * @param layer      The layer associated with the clicked cell
     * @param index      The index of the clicked cell
     */
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
            canvas.repaint();
        }
    }

    /**
     * Handles the delete button click event.
     * Deletes the layer and refreshes the layer list.
     *
     * @param e          The mouse event
     * @param renderer   The cell renderer for the layer list
     * @param cellBounds The bounds of the cell
     * @param layer      The layer associated with the clicked cell
     * @param index      The index of the clicked cell
     */
    private void handleDeleteButton(MouseEvent e, LayerListCellRenderer renderer, Rectangle cellBounds, Layer layer, int index) {
        JButton deleteButton = renderer.getDeleteButton();
        Rectangle deleteButtonBounds = deleteButton.getBounds();
        Point absoluteDeleteButtonLocation = SwingUtilities.convertPoint(deleteButton, 0, 0, renderer);

        Rectangle deleteButtonRect = new Rectangle(
                cellBounds.x + absoluteDeleteButtonLocation.x,
                cellBounds.y + absoluteDeleteButtonLocation.y,
                deleteButtonBounds.width,
                deleteButtonBounds.height
        );

        if (deleteButtonRect.contains(e.getPoint())) {
            if (canvas.getLayerCount() <= 1) {
                statusMessageHandler.setStatusMessage("Cannot delete the only layer");
                return;
            }

            if (confirmLayerDeletion(layer.getName())) {
                canvas.deleteLayer(listModel.getSize() - 1 - index);
                refreshLayerList();
                statusMessageHandler.setStatusMessage("Layer deleted: " + layer.getName());
            }
        }
    }


    /**
     * Applies the name change to the selected layer.
     * Updates the layer's name and refreshes the layer list.
     *
     * @param newName The new name for the layer
     */
    private void applyNameChange(String newName) {
        if (layerList.getSelectedIndex() != -1) {
            Layer selectedLayer = canvas.getLayers().get(canvas.getCurrentLayerIndex());
            canvas.saveToUndoStack();
            selectedLayer.setName(newName);
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer renamed to: " + newName);
        }
    }

    /**
     * Creates a button with the specified icon, tooltip, and action.
     *
     * @param icon    The icon for the button
     * @param tooltip The tooltip text for the button
     * @param action  The action to perform when the button is clicked
     * @return The created button
     */
    private JButton createButton(Icon icon, String tooltip, Consumer<ActionEvent> action) {
        JButton button = new JButton(icon);
        button.addActionListener(action::accept);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        return button;
    }

    /**
     * Adds a new layer to the layer manager and refreshes the layer list.
     */
    private void addLayer() {
        canvas.addNewLayer();
        refreshLayerList();
        statusMessageHandler.setStatusMessage("New layer added");
    }

    /**
     * Duplicates the current layer and refreshes the layer list.
     */
    private void duplicateLayer() {
        boolean success = canvas.duplicateCurrentLayer();
        if (success) {
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer duplicated");
        } else {
            statusMessageHandler.setStatusMessage("Failed to duplicate layer");
        }
    }

    /**
     * Deletes the current layer from the layer manager and refreshes the layer list.
     * If there is only one layer, it shows a warning message.
     */
    private void deleteLayer() {
        if (canvas.getLayerCount() <= 1) {
            statusMessageHandler.setStatusMessage("Cannot delete the only layer");
            return;
        }

        Layer selectedLayer = canvas.getLayers().get(canvas.getCurrentLayerIndex());
        if (confirmLayerDeletion(selectedLayer.getName())) {
            canvas.deleteCurrentLayer();
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer deleted");
        }
    }

    /**
     * Merges the current layer with the layer below it.
     * If there is no layer below, it shows a warning message.
     */
    private void mergeLayerDown() {
        boolean success = canvas.mergeCurrentLayerDown();
        if (success) {
            refreshLayerList();
            statusMessageHandler.setStatusMessage("Layer merged down");
        } else {
            statusMessageHandler.setStatusMessage("Cannot merge bottom layer");
        }
    }

    /**
     * Flattens all visible layers into a single layer.
     * This operation cannot be undone.
     */
    private void flattenLayers() {
        if (canvas.getLayerCount() <= 1) {
            statusMessageHandler.setStatusMessage("Nothing to flatten - only one layer exists");
            return;
        }

        if (confirmFlattenLayers()) {
            boolean success = canvas.flattenLayers();
            if (success) {
                refreshLayerList();
                statusMessageHandler.setStatusMessage("All layers flattened into one");
            }
        }
    }

    /**
     * Shows a confirmation dialog for flattening layers.
     *
     * @return true if the user confirms the operation, false otherwise
     */
    private boolean confirmFlattenLayers() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);

        int result = JOptionPane.showConfirmDialog(
                parentWindow,
                "Flatten all layers into one? This operation cannot be undone.",
                "Confirm Flatten Layers",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Confirms the deletion of a layer with a dialog.
     *
     * @param layerName The name of the layer to be deleted
     * @return true if the user confirms the deletion, false otherwise
     */
    private boolean confirmLayerDeletion(String layerName) {
        // Get the main frame (or any window ancestor)
        Window parentWindow = SwingUtilities.getWindowAncestor(this);

        int result = JOptionPane.showConfirmDialog(
                parentWindow,  // Use the main window instead of LayerPanel
                "Delete layer \"" + layerName + "\"?",
                "Confirm Layer Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Sets up drag-and-drop functionality for the layer list.
     * Allows layers to be reordered by dragging and dropping.
     */
    private void setupDragAndDrop() {
        layerList.setDragEnabled(true);
        layerList.setDropMode(DropMode.INSERT);

        layerList.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                @SuppressWarnings("unchecked")
                JList<Layer> list = (JList<Layer>) c;
                int index = list.getSelectedIndex();
                if (index < 0) {
                    return null;
                }
                return new LayerTransferable(index);
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                // The actual move is handled in importData
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(LayerTransferable.LAYER_DATA_FLAVOR);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                // For paste operations, redirect to the canvas
                if (!support.isDrop()) {
                    try {
                        canvas.pasteSelection();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }

                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int visualDropIndex = dl.getIndex();
                if (visualDropIndex == -1) {
                    return false;
                }

                try {
                    LayerTransferable transferable = (LayerTransferable) support.getTransferable()
                            .getTransferData(LayerTransferable.LAYER_DATA_FLAVOR);
                    int visualSourceIndex = transferable.getSourceIndex();

                    // Convert visual indices to model indices (reverse order)
                    int modelDropIndex = listModel.getSize() - visualDropIndex - (visualSourceIndex < visualDropIndex ? 0 : 1);
                    int modelSourceIndex = listModel.getSize() - visualSourceIndex - 1;

                    // Move the layer in the layer manager
                    boolean success = canvas.moveLayer(modelSourceIndex, modelDropIndex);
                    if (success) {
                        refreshLayerList();
                        statusMessageHandler.setStatusMessage("Layer reordered");
                    }
                    return success;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public void onLayersChanged() {
        refreshLayerList();
    }
}