package com.esgdev.sparkpaint.engine.layer;

/**
 * Interface for listening to layer changes.
 */
public interface LayerChangeListener {
    /**
     * Called when layers have changed.
     */
    void onLayersChanged();

}
