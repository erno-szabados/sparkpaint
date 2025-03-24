package com.esgdev.sparkpaint.io;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ImageSelection is a class that implements Transferable to handle image data for clipboard operations.
 * It allows copying and pasting images to and from the system clipboard.
 */
public class ImageSelection implements Transferable {

    private final BufferedImage image;

    /**
     * Constructor that creates a new ImageSelection object.
     * It takes a BufferedImage and creates a copy of it for clipboard operations.
     *
     * @param image The BufferedImage to be copied to the clipboard.
     */
    public ImageSelection(BufferedImage image) {
        // We're losing alpha channel here, but java clipboard doesn't support it for the
        // image flavor, so we're preventing the ioexception.
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        this.image = copy;

    }

    /**
     * Returns the data flavors supported by this selection.
     * This method returns an array containing the image flavor.
     *
     * @return An array of DataFlavor objects supported by this selection.
     */
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    /**
     * Checks if the provided DataFlavor is supported.
     * This method ensures that only image flavors are accepted.
     *
     * @param flavor The DataFlavor to check.
     * @return True if the flavor is supported, false otherwise.
     */
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        try {
            return DataFlavor.imageFlavor.equals(flavor);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the image data if the requested flavor is supported.
     *
     * @param flavor The DataFlavor to check.
     * @return The image data if supported, null otherwise.
     */
    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (!isDataFlavorSupported(flavor)) {
            return null;
        }
        return image;
    }

    /**
     * Copies the provided image to the system clipboard.
     * This method creates a compatible copy of the image and sets it to the clipboard.
     *
     * @param image The BufferedImage to copy to the clipboard.
     */
    // Static helper method to copy an image to the clipboard
    public static void copyImage(BufferedImage image) {
        ImageSelection selection = new ImageSelection(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    /**
     * Attempts to paste an image from the system clipboard.
     * This method checks for the image flavor and retrieves the image data.
     *
     * @return A BufferedImage if available, null otherwise.
     * @throws IOException                If an error occurs while accessing the clipboard.
     * @throws UnsupportedFlavorException If the clipboard does not contain an image.
     */
    public static BufferedImage pasteImage() throws IOException, UnsupportedFlavorException {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // Get all available data flavors
        DataFlavor[] flavors = clipboard.getAvailableDataFlavors();

        // Only proceed if an image flavor is available
        if (flavors != null) {
            for (DataFlavor flavor : flavors) {
                // Strictly check for image flavor
                if (flavor != null && DataFlavor.imageFlavor.equals(flavor)) {
                    // Safely attempt to get image data
                    Transferable transferable = clipboard.getContents(null);
                    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Object data = transferable.getTransferData(DataFlavor.imageFlavor);

                        // Ensure it's a BufferedImage
                        if (data instanceof BufferedImage) {
                            return (BufferedImage) data;
                        }
                    }
                }
            }
        }

        return null;
    }
}
