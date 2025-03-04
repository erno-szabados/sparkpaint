package com.esgdev.sparkpaint.engine;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

class ImageSelection implements Transferable {

    private final BufferedImage image;

    public ImageSelection(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        this.image = copy;

    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        try {
            return DataFlavor.imageFlavor.equals(flavor);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (!isDataFlavorSupported(flavor)) {
            return null;
        }
        return image;
    }

    // Static helper method to copy an image to the clipboard
    public static void copyImage(BufferedImage image) {
        ImageSelection selection = new ImageSelection(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    // Static helper method to try to paste an image from the clipboard.
    // Returns null if no image is available.
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
