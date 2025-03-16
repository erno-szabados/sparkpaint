package com.esgdev.sparkpaint.engine.selection;

import java.awt.Graphics2D;

public interface SelectionRenderer {
    void drawSelectionContent(Graphics2D g2d);
    void drawSelectionOutline(Graphics2D g2d);
}