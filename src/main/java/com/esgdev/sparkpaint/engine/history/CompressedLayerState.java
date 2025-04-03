package com.esgdev.sparkpaint.engine.history;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CompressedLayerState represents the state of a set of compressed layers.
 * It contains a list of compressed layers and the index of the current layer.
 */
public class CompressedLayerState implements Serializable {
    private static final long serialVersionUID = 1L;

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

    /**
     * Converts this CompressedLayerState to a regular LayerState by decompressing all layers.
     *
     * @return A new LayerState with all layers decompressed
     * @throws IOException If any layer fails to decompress
     */
    public LayerState toLayerState() throws IOException {
        List<Layer> layers = new ArrayList<>(compressedLayers.size());

        for (CompressedLayer compressedLayer : compressedLayers) {
            layers.add(compressedLayer.toLayer());
        }

        return new LayerState(layers, currentLayerIndex);
    }
}