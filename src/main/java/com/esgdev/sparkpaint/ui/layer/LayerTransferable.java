package com.esgdev.sparkpaint.ui.layer;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

// Add the inner class for the transferable
class LayerTransferable implements Transferable {
    public static final DataFlavor LAYER_DATA_FLAVOR = new DataFlavor(Layer.class, "Layer");
    private final int sourceIndex;

    public LayerTransferable(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{LAYER_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(LAYER_DATA_FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor.equals(LAYER_DATA_FLAVOR)) {
            return this;
        }
        throw new UnsupportedFlavorException(flavor);
    }

    public int getSourceIndex() {
        return sourceIndex;
    }
}
