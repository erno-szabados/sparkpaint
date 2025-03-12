package com.esgdev.sparkpaint.io;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * FileManager is responsible for saving and loading images to and from files.
 */
public class FileManager {
    private String currentFilePath;

    /**
     * Saves the given image to the specified file.
     *
     * @param file        The file to save the image to.
     * @param canvasImage The image to save.
     * @throws IOException If an error occurs during saving.
     */
    public void saveToFile(File file, Image canvasImage) throws IOException {
        BufferedImage imageToSave = new BufferedImage(
                canvasImage.getWidth(null),
                canvasImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = imageToSave.createGraphics();
        g.drawImage(canvasImage, 0, 0, null);
        g.dispose();

        String fileName = file.getName().toLowerCase();
        String formatName = "png";

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            formatName = "jpeg";
            imageToSave = convertToRGB(imageToSave);
        }

        if (!ImageIO.write(imageToSave, formatName, file)) {
            throw new IOException("No appropriate writer found for format: " + formatName);
        }

        currentFilePath = file.getAbsolutePath();
    }

    /**
     * Loads an image from the specified file.
     *
     * @param file The file to load the image from.
     * @return The loaded image.
     * @throws IOException If an error occurs during loading.
     */
    public BufferedImage loadFromFile(File file) throws IOException {
        BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Failed to load image: " + file.getName());
        }

        currentFilePath = file.getAbsolutePath();
        return loadedImage;
    }

    /**
     * Converts a BufferedImage to RGB format.
     *
     * @param input The input image.
     * @return The converted image in RGB format.
     */
    private BufferedImage convertToRGB(BufferedImage input) {
        BufferedImage rgbImage = new BufferedImage(
                input.getWidth(),
                input.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D rgbGraphics = rgbImage.createGraphics();
        rgbGraphics.drawImage(input, 0, 0, Color.WHITE, null);
        rgbGraphics.dispose();
        return rgbImage;
    }

    /**
     * Returns the current file path.
     *
     * @return The current file path.
     */
    public String getCurrentFilePath() {
        return currentFilePath;
    }

    /**
     * Sets the current file path.
     *
     * @param currentFilePath The new current file path.
     */
    public void setCurrentFilePath(String currentFilePath) {
        this.currentFilePath = currentFilePath;
    }
}