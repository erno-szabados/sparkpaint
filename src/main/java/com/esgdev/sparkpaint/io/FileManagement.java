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
}