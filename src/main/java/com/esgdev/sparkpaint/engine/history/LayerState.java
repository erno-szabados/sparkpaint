package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;
import com.esgdev.sparkpaint.engine.selection.Selection;

import java.util.List;

public class LayerState {
    private final List<Layer> layers;
    private final int currentLayerIndex;
    private final Selection floatingSelection;

    public LayerState(List<Layer> layers, int currentLayerIndex) {
        this(layers, currentLayerIndex, null);
    }

    /**
     * @param floatingSelection a floating paste that was active with this state, or null
     */
    public LayerState(List<Layer> layers, int currentLayerIndex, Selection floatingSelection) {
        this.layers = layers;
        this.currentLayerIndex = currentLayerIndex;
        this.floatingSelection = floatingSelection;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }

    public Selection getFloatingSelection() {
        return floatingSelection;
    }
}
