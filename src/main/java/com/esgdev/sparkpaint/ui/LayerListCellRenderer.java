package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.Layer;
import com.esgdev.sparkpaint.engine.LayerManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class LayerListCellRenderer extends JPanel implements ListCellRenderer<Layer> {
    private final JCheckBox visibilityCheckBox;
    private final JTextField nameField;
    private final JButton deleteButton;

    public LayerListCellRenderer() {

        setLayout(new BorderLayout(5, 0));
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(5, 5, 5, 5)));

        // Create left controls panel
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftControls.setOpaque(false);

        // Visibility checkbox
        visibilityCheckBox = new JCheckBox();
        visibilityCheckBox.setToolTipText("Toggle layer visibility");
        leftControls.add(visibilityCheckBox);

        // Name field
        nameField = new JTextField();
        nameField.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));

        // Delete button
        deleteButton = new JButton(IconLoader.loadAndScaleIcon("delete.png", 16, 16));
        deleteButton.setToolTipText("Delete this layer");

        add(leftControls, BorderLayout.WEST);
        add(nameField, BorderLayout.CENTER);
        add(deleteButton, BorderLayout.EAST);
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public  JCheckBox getVisibilityCheckBox() {
        return visibilityCheckBox;
    }

    public JTextField getNameField() {
        return nameField;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Layer> list,
                                                  Layer layer,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        // Get the model
        ListModel<? extends Layer> model = list.getModel();

        int adjustedIndex = model.getSize() - 1 - index;
        // Setup name field
        nameField.setText(layer.getName());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        visibilityCheckBox.setSelected(model.getElementAt(adjustedIndex).isVisible());

        // Setup visibility checkbox
        visibilityCheckBox.setSelected(layer.isVisible());

        return this;
    }
}