package com.esgdev.sparkpaint.io;

public interface ClipboardChangeListener {
    void clipboardStateChanged(boolean canCopy, boolean canPaste);
}