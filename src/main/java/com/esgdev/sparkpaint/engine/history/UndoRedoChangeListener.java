package com.esgdev.sparkpaint.engine.history;

public interface UndoRedoChangeListener {
    void undoRedoStateChanged(boolean canUndo, boolean canRedo);
}