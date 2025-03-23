package com.esgdev.sparkpaint.engine.history;

/**
 * Interface that exposes undo/redo functionality.
 * Provides methods to manage and interact with the editing history.
 */
public interface HistoryManagement {

    /**
     * Saves the current state to the undo stack.
     */
    void saveToUndoStack();

    /**
     * Undoes the last action.
     */
    void undo();

    /**
     * Redoes the previously undone action.
     */
    void redo();

    /**
     * Checks if there are any actions that can be undone.
     *
     * @return true if there are actions to undo, false otherwise.
     */
    boolean canUndo();

    /**
     * Checks if there are any actions that can be redone.
     *
     * @return true if there are actions to redo, false otherwise.
     */
    boolean canRedo();

    /**
     * Clears the undo and redo history.
     */
    void clearHistory();

    /**
     * Adds a listener to be notified of undo/redo state changes.
     *
     * @param listener The listener to be added.
     */
    void addUndoRedoChangeListener(UndoRedoChangeListener listener);
}