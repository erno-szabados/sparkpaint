package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface that exposes file management functionality.
 * Provides methods to save, load, and manage file operations.
 */
public interface FileManagement {


    /**
     * Saves the current canvas to a file.
     *
     * @param file The file to save to
     * @throws IOException If an error occurs during saving
     */
    void saveToFile(File file) throws IOException;

    /**
     * Loads content from a file into the canvas.
     *
     * @param file The file to load
     * @return
     * @throws IOException If an error occurs during loading
     */
    LayerState loadFromFile(File file) throws IOException;

    // Add this to the FileManagement interface
    /**
     * Loads a layered file format.
     *
     * @param file The file to load
     * @return The LayerState containing layers and current layer index
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If the class of a serialized object cannot be found
     */
    LayerState loadFromLayeredFile(File file) throws IOException, ClassNotFoundException;
    /**
     * Gets the path of the currently loaded or saved file.
     *
     * @return The current file path, or null if no file is currently associated
     */
    String getCurrentFilePath();

    /**
     * Resets the current file path, typically used when creating a new document.
     */
    void resetCurrentFilePath();

    /**
     * Sets the current file path to the specified path.
     *
     * @param path The new file path
     */
    void setCurrentFilePath(String path);

    /**
     * Exports visible layers as separate PNG files.
     *
     * @param directory      The directory to save the layers to
     * @param fileNamePrefix The prefix for each layer file name
     * @param layers         The layers to export
     * @return Number of layers successfully exported
     * @throws IOException If an error occurs during exporting
     */
    int exportLayersAsPNG(File directory, String fileNamePrefix, List<Layer> layers) throws IOException;

    /**
     * Generates a unique file name prefix based on current time
     *
     * @return A unique file name prefix
     */
    String generateFileNamePrefix();
}