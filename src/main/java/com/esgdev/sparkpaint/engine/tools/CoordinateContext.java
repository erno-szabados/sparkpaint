package com.esgdev.sparkpaint.engine.tools;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

/**
 * Helper class to store coordinate context information for gradient operations
 */
public class CoordinateContext {
    public BufferedImage currentImage;
    public GeneralPath clipPath;
    public Rectangle bounds;
    public Point adjustedClickPoint;
    public Point adjustedStart;
    public Point adjustedEnd;
}