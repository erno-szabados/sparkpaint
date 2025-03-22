package com.esgdev.sparkpaint.engine.history;

import java.util.List;

public class CompressedLayerState {
    private final List<CompressedLayer> compressedLayers;
    private final int currentLayerIndex;

    public CompressedLayerState(List<CompressedLayer> compressedLayers, int currentLayerIndex) {
        this.compressedLayers = compressedLayers;
        this.currentLayerIndex = currentLayerIndex;
    }

    public List<CompressedLayer> getCompressedLayers() {
        return compressedLayers;
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }
}
