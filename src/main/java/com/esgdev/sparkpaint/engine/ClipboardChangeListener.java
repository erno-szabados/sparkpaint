package com.esgdev.sparkpaint.engine;

public interface ClipboardChangeListener {
    void clipboardStateChanged(boolean canCopy, boolean canPaste);
}