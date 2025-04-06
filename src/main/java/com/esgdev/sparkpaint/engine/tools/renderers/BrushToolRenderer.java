package com.esgdev.sparkpaint.engine.tools.renderers;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.engine.tools.BrushTool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * BrushToolRenderer handles the actual implementation of various brush rendering styles
 * used by the BrushTool.
 */
public class BrushToolRenderer extends BaseRenderer {
    private final Random random = new Random();
    private static final int SPRAY_REFERENCE_SIZE = 5;

    public BrushToolRenderer() {
    }

    /**
     * Draws the specified brush shape at the given location.
     *
     * @param targetImage     The image to draw on
     * @param brushShape      The shape of the brush
     * @param x               X coordinate of the brush center
     * @param y               Y coordinate of the brush center
     * @param size            Size of the brush
     * @param paintColor      Color to paint with
     * @param sprayDensity    Density for spray brush
     * @param useAntiAliasing Whether to use antialiasing
     * @param blendStrength   Strength of color blending
     * @param clip            The clipping region to respect when drawing
     */
    public void drawBrush(
            BufferedImage targetImage,
            BrushTool.BrushShape brushShape,
            int x, int y, int size,
            Color paintColor,
            int sprayDensity,
            boolean useAntiAliasing,
            float blendStrength,
            Shape clip) {

        // Set anti-aliasing flag
        setAntiAliasing(useAntiAliasing);

        // If using transparent color, use special handling
        if (paintColor.getAlpha() == 0) {
            handleTransparentPainting(targetImage, brushShape, x, y, size, sprayDensity, blendStrength, clip);
            return;
        }

        // Apply the brush shape at the specified location
        switch (brushShape) {
            case SQUARE:
                drawBlendedShape(targetImage, x - size / 2, y - size / 2, size, size, paintColor, blendStrength, clip, true);
                break;
            case CIRCLE:
                drawBlendedShape(targetImage, x - size / 2, y - size / 2, size, size, paintColor, blendStrength, clip, false);
                break;
            case SPRAY:
                sprayPaint(targetImage, x, y, size, paintColor, sprayDensity, blendStrength, clip);
                break;
        }
    }

    /**
     * Draws a blended shape (square or circle) on the target image.
     */
    private void drawBlendedShape(BufferedImage image, int x, int y, int width, int height,
                                  Color paintColor, float blendStrength,
                                  Shape clip, boolean isSquare) {
        // Create a temporary image for the shape
        BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempG2d = tempImage.createGraphics();
        tempG2d.setColor(paintColor);
        configureGraphics(tempG2d);

        // Draw the shape to the temporary image
        if (isSquare) {
            tempG2d.fillRect(x, y, width, height);
        } else {
            tempG2d.fillOval(x, y, width, height);
        }
        tempG2d.dispose();

        // Apply blending to the main image
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(image.getWidth(), x + width);
        int endY = Math.min(image.getHeight(), y + height);

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int tempRGB = tempImage.getRGB(i, j);
                // Only process non-transparent pixels from the shape
                if ((tempRGB & 0xFF000000) != 0) {
                    // Check if this pixel is within the clipping region
                    if (clip == null || clip.contains(i, j)) {
                        int currentRGB = image.getRGB(i, j);
                        Color blendedColor = RenderUtils.getBlendedColor(currentRGB, blendStrength, paintColor);
                        image.setRGB(i, j, blendedColor.getRGB());
                    }
                }
            }
        }
    }

    /**
     * Handles painting with transparency - this erases content by setting pixels to transparent
     */
    private void handleTransparentPainting(BufferedImage targetImage, BrushTool.BrushShape shape,
                                           int x, int y, int size, int sprayDensity,
                                           float alphaStrength, Shape clip) {
        // Create a mask image for the alpha feathering
        BufferedImage maskImage = createMask(targetImage.getWidth(), targetImage.getHeight(), clip);
        Graphics2D maskG2d = maskImage.createGraphics();
        configureGraphics(maskG2d, Color.WHITE, 1.0f);

        int topLeftX = x - size / 2;
        int topLeftY = y - size / 2;

        // Draw the shape to the mask
        switch (shape) {
            case SQUARE:
                maskG2d.fillRect(topLeftX, topLeftY, size, size);
                break;
            case CIRCLE:
                maskG2d.fillOval(topLeftX, topLeftY, size, size);
                break;
            case SPRAY:
                // For spray, do individual feathered dots
                int radius = size / 2;
                double area = Math.PI * radius * radius;
                int effectiveDensity = (int) (sprayDensity * (area / (Math.PI * SPRAY_REFERENCE_SIZE * SPRAY_REFERENCE_SIZE)));

                for (int i = 0; i < effectiveDensity; i++) {
                    double x_offset = (random.nextDouble() * 2 - 1) * radius;
                    double y_offset = (random.nextDouble() * 2 - 1) * radius;

                    if (x_offset * x_offset + y_offset * y_offset > radius * radius) {
                        continue;
                    }

                    int px = x + (int) x_offset;
                    int py = y + (int) y_offset;

                    if (px >= 0 && px < targetImage.getWidth() && py >= 0 && py < targetImage.getHeight()) {
                        // Check if this point is within the clipping region
                        if (clip == null || clip.contains(px, py)) {
                            // Draw a small dot
                            maskG2d.fillOval(px - 1, py - 1, 3, 3);
                        }
                    }
                }
                break;
        }
        maskG2d.dispose();

        // Apply alpha feathering based on the mask
        int startX = Math.max(0, topLeftX);
        int startY = Math.max(0, topLeftY);
        int endX = Math.min(targetImage.getWidth(), topLeftX + size + 1);
        int endY = Math.min(targetImage.getHeight(), topLeftY + size + 1);

        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                int maskRGB = maskImage.getRGB(i, j);
                // Only process visible pixels from the mask
                if ((maskRGB & 0xFF000000) != 0) {
                    // Check if this pixel is within the clipping region
                    if (clip == null || clip.contains(i, j)) {
                        // Get current pixel
                        int currentRGB = targetImage.getRGB(i, j);

                        // Extract current alpha value
                        int alpha = (currentRGB >> 24) & 0xFF;

                        // Calculate new alpha based on blend strength
                        int newAlpha = Math.max(0, (int) (alpha * (1.0f - alphaStrength)));

                        // Apply new alpha, keeping RGB components the same
                        int newRGB = (currentRGB & 0x00FFFFFF) | (newAlpha << 24);
                        targetImage.setRGB(i, j, newRGB);
                    }
                }
            }
        }
    }

    /**
     * Paints using a spray effect at the specified point.
     */
    private void sprayPaint(BufferedImage image, int centerX, int centerY, int size,
                            Color paintColor, int sprayDensity, float blendStrength, Shape clip) {
        int radius = size / 2;
        double area = Math.PI * radius * radius;
        int effectiveDensity = (int) (sprayDensity * (area / (Math.PI * SPRAY_REFERENCE_SIZE * SPRAY_REFERENCE_SIZE)));

        for (int i = 0; i < effectiveDensity; i++) {
            double x_offset = (random.nextDouble() * 2 - 1) * radius;
            double y_offset = (random.nextDouble() * 2 - 1) * radius;

            if (x_offset * x_offset + y_offset * y_offset > radius * radius) {
                continue;
            }

            int x = centerX + (int) x_offset;
            int y = centerY + (int) y_offset;

            if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                // Check if this point is within the clipping region
                if (clip == null || clip.contains(x, y)) {
                    int currentRGB = image.getRGB(x, y);
                    Color blendedColor = RenderUtils.getBlendedColor(currentRGB, random.nextFloat() * blendStrength, paintColor);
                    image.setRGB(x, y, blendedColor.getRGB());
                }
            }
        }
    }
}