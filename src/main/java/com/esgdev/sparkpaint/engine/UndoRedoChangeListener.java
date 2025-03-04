package com.esgdev.sparkpaint.engine;

public interface UndoRedoChangeListener {
    void undoRedoStateChanged(boolean canUndo, boolean canRedo);
}