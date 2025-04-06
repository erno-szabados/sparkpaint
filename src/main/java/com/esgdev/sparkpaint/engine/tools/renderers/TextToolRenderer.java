package com.esgdev.sparkpaint.engine.tools.renderers;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

/**
 * TextToolRenderer handles the actual rendering of text for the TextTool.
 * This class is responsible for drawing text, handling transparent text,
 * and creating preview visualizations.
 */
public class TextToolRenderer extends BaseRenderer {

    public TextToolRenderer() {
    }

    /**
     * Draws text on the specified image.
     */
    public void drawText(BufferedImage targetImage, Graphics2D g2d, Point position,
                         String text, Font font, Color textColor, boolean isPreview) {
        // Apply rendering settings
        configureGraphics(g2d);
        boolean isTransparent = textColor.getAlpha() == 0;

        if (isPreview) {
            drawTextPreview(g2d, position, text, font, textColor, isTransparent);
        } else if (isTransparent) {
            drawTransparentText(targetImage, position, text, font, g2d.getClip());
        } else {
            drawNormalText(g2d, position, text, font, textColor);
        }
    }

    /**
     * Draws normal (non-transparent) text.
     */
    private void drawNormalText(Graphics2D g2d, Point position, String text, Font font, Color textColor) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setFont(font);
        g2d.setColor(textColor);
        g2d.drawString(text, position.x, position.y);
    }

    /**
     * Draws transparent text by creating a mask and applying transparency.
     */
    private void drawTransparentText(BufferedImage targetImage, Point position,
                                     String text, Font font, Shape clip) {
        // Create a mask image for transparency
        BufferedImage maskImage = new BufferedImage(
                targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = maskImage.createGraphics();

        // Apply rendering hints
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                useAntiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw text onto mask
        maskG2d.setFont(font);
        maskG2d.setColor(Color.WHITE);
        maskG2d.drawString(text, position.x, position.y);
        maskG2d.dispose();

        // Apply transparency mask
        applyTransparencyMask(targetImage, maskImage, clip);
    }

    /**
     * Draws a preview of the text, including special handling for transparent text.
     */
    private void drawTextPreview(Graphics2D g2d, Point position, String text,
                                 Font font, Color textColor, boolean isTransparent) {
        if (isTransparent) {
            // Draw dotted/dashed outline for transparent text
            g2d.setFont(font);

            // Create a TextLayout to get the outline shape
            FontRenderContext frc = g2d.getFontRenderContext();
            TextLayout textLayout = new TextLayout(text, font, frc);
            Shape textShape = textLayout.getOutline(null);

            // Translate to the correct position
            g2d.translate(position.x, position.y);

            // Draw the outline with a dashed pattern
            float[] dashPattern = {3.0f, 3.0f};
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

            // Draw white line first
            g2d.setColor(Color.WHITE);
            g2d.draw(textShape);

            // Draw black line with offset dash pattern
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dashPattern, dashPattern[0]));
            g2d.draw(textShape);
        } else {
            // For non-transparent text, show the text with semi-transparency
            g2d.setColor(new Color(textColor.getRed(), textColor.getGreen(),
                    textColor.getBlue(), 128));
            g2d.setFont(font);
            g2d.drawString(text, position.x, position.y);
        }
    }
}