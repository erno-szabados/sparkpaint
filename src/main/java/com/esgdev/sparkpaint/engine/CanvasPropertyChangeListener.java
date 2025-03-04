package com.esgdev.sparkpaint.engine;

import java.awt.*;

public interface CanvasPropertyChangeListener {
    void onDrawingColorChanged(Color newColor);
    void onFillColorChanged(Color newColor);
    void onBackgroundColorChanged(Color newColor);
}