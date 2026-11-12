package com.esgdev.sparkpaint.ui.helpers;

import com.esgdev.sparkpaint.engine.ImageLimits;
import com.esgdev.sparkpaint.engine.ImageLimits.ImageSizeException;

import javax.swing.*;
import java.awt.*;

/**
 * Shows user-facing messages for image size limits and out-of-memory failures.
 */
public final class MemoryDialogs {

    private MemoryDialogs() {
    }

    /**
     * Checks an allocation against {@link ImageLimits}. Shows the reason and returns false when it is refused.
     *
     * @return true if the image may be allocated
     */
    public static boolean verifyAllocation(Component parent, String title, int width, int height, int residentImages) {
        try {
            ImageLimits.verify(width, height, residentImages);
            return true;
        } catch (ImageSizeException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Reports that an operation ran out of memory.
     *
     * @param action a short description of the failed action, such as "resize the image"
     */
    public static void showOutOfMemory(Component parent, String action) {
        JOptionPane.showMessageDialog(parent,
                "Not enough memory to " + action + ". Try a smaller image or close other applications.",
                "Out of Memory",
                JOptionPane.ERROR_MESSAGE);
    }
}
