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

public class ImageSelection implements Transferable {

    private ByteArrayInputStream bais;
    public static final DataFlavor PNG_FLAVOR = new DataFlavor("image/png", "PNG Image");

    // Only store the bytes for PNG data to avoid transparency issues
    public ImageSelection(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] imageInBytes = baos.toByteArray();
        baos.close();
        bais = new ByteArrayInputStream(imageInBytes);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        // Only offer PNG flavor when copying from our app to preserve transparency
        return new DataFlavor[]{PNG_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(PNG_FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }

        return bais;
    }

    public static void copyImage(BufferedImage image) throws IOException {
        ImageSelection selection = new ImageSelection(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    public static BufferedImage pasteImage() throws IOException, UnsupportedFlavorException {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);

        // First try with PNG_FLAVOR for best transparency support (from our app)
        if (transferable != null && transferable.isDataFlavorSupported(PNG_FLAVOR)) {
            ByteArrayInputStream bais = (ByteArrayInputStream) transferable.getTransferData(PNG_FLAVOR);
            bais.reset();
            return ImageIO.read(bais);
        }

        // Fall back to standard image flavor (from other apps)
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            Image img = (Image) transferable.getTransferData(DataFlavor.imageFlavor);

            // Convert Image to BufferedImage with transparency support
            if (img instanceof BufferedImage) {
                return (BufferedImage) img;
            } else {
                // Create a BufferedImage with transparency
                BufferedImage bufferedImage = new BufferedImage(
                        img.getWidth(null),
                        img.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB
                );

                // Draw the image into the BufferedImage
                Graphics2D g2d = bufferedImage.createGraphics();
                g2d.drawImage(img, 0, 0, null);
                g2d.dispose();

                return bufferedImage;
            }
        }

        return null;
    }
}