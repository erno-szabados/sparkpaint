package com.esgdev.sparkpaint.io;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Interface that exposes clipboard management functionality.
 * Provides methods to interact with clipboard operations for selections and images.
 */
public interface ClipboardManagement {

    /**
     * Cuts the current selection and places it on the clipboard.
     */
    void cutSelection();

    /**
     * Copies the current selection to the clipboard.
     */
    void copySelection();

    /**
     * Pastes content from the clipboard to the canvas.
     *
     * @throws IOException if there is an error accessing the clipboard
     * @throws UnsupportedFlavorException if the clipboard content is not supported
     */
    void pasteSelection() throws IOException, UnsupportedFlavorException;

    /**
     * Erases the current selection from the canvas.
     */
    void deleteSelectionAreaFromCurrentLayer();

    /**
     * Checks if there is a valid selection that can be copied/cut.
     *
     * @return true if there is a valid selection, false otherwise
     */
    boolean hasSelection();

    /**
     * Checks if there is pasteable content in the clipboard.
     *
     * @return true if there is pasteable content, false otherwise
     */
    boolean canPaste();

    /**
     * Adds a listener to be notified of clipboard state changes.
     *
     * @param listener The listener to be added.
     */
    void addClipboardChangeListener(ClipboardChangeListener listener);

    /**
     * Removes a clipboard change listener.
     *
     * @param listener The listener to be removed.
     */
    void removeClipboardChangeListener(ClipboardChangeListener listener);

    /**
     * Notifies listeners that the clipboard state has changed.
     */
    void notifyClipboardStateChanged();

    /**
     * Deletes the current selection.
     */
    void deleteCurrentSelection();
}