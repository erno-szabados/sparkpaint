package com.esgdev.sparkpaint.engine.tools.renderers;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Base class for all tool renderers providing common functionality.
 */
public abstract class BaseRenderer {
    protected boolean useAntiAliasing = true;

    /**
     * Sets whether to use antialiasing for rendering.
     */
    public void setAntiAliasing(boolean useAntiAliasing) {
        this.useAntiAliasing = useAntiAliasing;
    }

    /**
     * Configures basic rendering hints for the graphics context.
     */
    protected void configureGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Configures graphics with stroke settings.
     */
    protected void configureGraphics(Graphics2D g2d, float lineThickness) {
        configureGraphics(g2d);
        g2d.setStroke(new BasicStroke(lineThickness));
    }

    /**
     * Configures graphics with color and stroke settings.
     */
    protected void configureGraphics(Graphics2D g2d, Color color, float lineThickness) {
        configureGraphics(g2d, lineThickness);
        g2d.setColor(color);
    }

    /**
     * Creates a transparent mask for applying effects.
     */
    protected BufferedImage createMask(int width, int height, Shape clip) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = mask.createGraphics();

        // Initialize as transparent
        maskG2d.setComposite(AlphaComposite.Clear);
        maskG2d.fillRect(0, 0, width, height);

        // Apply clipping if needed
        if (clip != null) {
            maskG2d.setClip(clip);
        }

        // Configure quality settings
        configureGraphics(maskG2d);

        maskG2d.setComposite(AlphaComposite.SrcOver);
        maskG2d.setColor(Color.WHITE);

        return mask;
    }

    /**
     * Applies full transparency to pixels where the mask is non-zero.
     */
    protected void applyTransparencyMask(BufferedImage targetImage, BufferedImage maskImage, Shape clip) {
        for (int y = 0; y < targetImage.getHeight(); y++) {
            for (int x = 0; x < targetImage.getWidth(); x++) {
                // Check if this pixel is within clip region
                if (clip == null || clip.contains(x, y)) {
                    int maskRGB = maskImage.getRGB(x, y);
                    // Only process pixels where the mask is non-zero
                    if ((maskRGB & 0xFF000000) != 0) {
                        // Set full transparency (alpha = 0)
                        int newRGB = targetImage.getRGB(x, y) & 0x00FFFFFF;
                        targetImage.setRGB(x, y, newRGB);
                    }
                }
            }
        }
    }
}