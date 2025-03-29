package com.esgdev.sparkpaint.io;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ImageSelection is a class that implements Transferable to handle image data for clipboard operations.
 * It allows copying and pasting images to and from the system clipboard.
 */
public class ImageSelection implements Transferable {

    private ByteArrayInputStream bais;
    public static final DataFlavor PNG_FLAVOR = new DataFlavor("image/png", "PNG Image");

    /**
     * Constructor that creates a new ImageSelection object.
     * It takes a BufferedImage and creates a copy of it for clipboard operations.
     *
     * @param image The BufferedImage to be copied to the clipboard.
     */
    public ImageSelection(BufferedImage image) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageInBytes = baos.toByteArray();
            baos.close();
            bais = new ByteArrayInputStream(imageInBytes);
    }

    public ImageSelection(ByteArrayInputStream byteArrayInputStream) {
        bais = byteArrayInputStream;
    }

    /**
     * Returns the data flavors supported by this selection.
     * This method returns an array containing the image flavor.
     *
     * @return An array of DataFlavor objects supported by this selection.
     */
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{PNG_FLAVOR};
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
            return flavor.equals(PNG_FLAVOR);
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
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }

        return bais;
    }

    /**
     * Copies the provided image to the system clipboard.
     * This method creates a compatible copy of the image and sets it to the clipboard.
     *
     * @param image The BufferedImage to copy to the clipboard.
     */
    // Static helper method to copy an image to the clipboard
    public static void copyImage(BufferedImage image) throws IOException {

            // Convert the image to a format that supports transparency (e.g., PNG)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageInBytes = baos.toByteArray();
            baos.close();

            // Create an ImageSelection object with the PNG image
            ImageSelection selection = new ImageSelection(new ByteArrayInputStream(imageInBytes));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            clipboard.setContents(selection, null);
    }

    /**
     * Pastes an image from the system clipboard.
     * This method retrieves the image data from the clipboard and returns it as a BufferedImage.
     *
     * @return The pasted BufferedImage, or null if no image is available.
     * @throws IOException                  If there is an error reading the image data.
     * @throws UnsupportedFlavorException   If the clipboard content is not supported.
     */
    public static BufferedImage pasteImage() throws IOException, UnsupportedFlavorException {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard.isDataFlavorAvailable(PNG_FLAVOR)) {
            Transferable transferable = clipboard.getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(PNG_FLAVOR)) {
                ByteArrayInputStream bais = (ByteArrayInputStream) transferable.getTransferData(PNG_FLAVOR);
                bais.reset();
                BufferedImage image = ImageIO.read(bais);
                return image;
            }
        }
        return null;
    }
}
