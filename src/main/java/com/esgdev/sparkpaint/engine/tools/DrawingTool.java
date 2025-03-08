package com.esgdev.sparkpaint.engine.tools;

import java.awt.event.MouseEvent;

public interface DrawingTool {
    void mouseMoved(MouseEvent e);
    void mousePressed(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void mouseReleased(MouseEvent e);
    void setCursor();
    String statusMessage();
}