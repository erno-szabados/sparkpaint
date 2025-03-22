package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.util.List;

public class LayerState {
    private final List<Layer> layers;
    private final int currentLayerIndex;

    public LayerState(List<Layer> layers, int currentLayerIndex) {
        this.layers = layers;
        this.currentLayerIndex = currentLayerIndex;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }
}
