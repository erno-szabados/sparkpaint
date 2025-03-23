package com.esgdev.sparkpaint.engine.layer;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Interface that exposes layer management functionality.
 * Provides methods to manage and interact with drawing layers.
 */
public interface LayerManagement {

    /**
     * Adds a new layer to the layer stack.
     */
    void addNewLayer();

    /**
     * Duplicates the current layer and adds it above the current layer.
     *
     * @return true if the operation was successful
     */
    boolean duplicateCurrentLayer();

    /**
     * Deletes the current layer.
     */
    void deleteCurrentLayer();

    /**
     * Deletes a layer at the specified index.
     *
     * @param index the index of the layer to delete
     */
    void deleteLayer(int index);

    /**
     * Moves a layer from one index to another.
     *
     * @param fromIndex the index of the layer to move
     * @param toIndex the index to move the layer to
     * @return true if the operation was successful
     */
    boolean moveLayer(int fromIndex, int toIndex);

    /**
     * Gets the current list of layers.
     *
     * @return the list of layers
     */
    List<Layer> getLayers();

    /**
     * Sets the list of layers.
     *
     * @param layers the new list of layers
     */
    void setLayers(List<Layer> layers);

    /**
     * Gets the index of the current layer.
     *
     * @return the current layer index
     */
    int getCurrentLayerIndex();

    /**
     * Sets the current layer index.
     *
     * @param index the index to set as current
     */
    void setCurrentLayerIndex(int index);

    /**
     * Gets the image of the current layer.
     *
     * @return the current layer's image
     */
    BufferedImage getCurrentLayerImage();

    /**
     * Gets the number of layers.
     *
     * @return the layer count
     */
    int getLayerCount();

    /**
     * Merges the current layer with the layer below it.
     *
     * @return true if the operation was successful
     */
    boolean mergeCurrentLayerDown();

    /**
     * Flattens all visible layers into a single layer.
     *
     * @return true if the operation was successful
     */
    boolean flattenLayers();

    /**
     * Checks if transparency visualization is enabled.
     *
     * @return true if transparency visualization is enabled
     */
    boolean isTransparencyVisualizationEnabled();

    /**
     * Sets whether transparency visualization is enabled.
     *
     * @param enabled true to enable transparency visualization
     */
    void setTransparencyVisualizationEnabled(boolean enabled);
}