package com.esgdev.sparkpaint.io;

import com.esgdev.sparkpaint.engine.history.LayerState;
import com.esgdev.sparkpaint.engine.layer.Layer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager is responsible for saving and loading images to and from files.
 */
public class FileManager implements FileManagement {
    private String currentFilePath;

    /**
     * Saves the given layers as a flattened image to the specified file.
     *
     * @param file              The file to save to.
     * @param layers            The layers to flatten and save.
     * @throws IOException If an error occurs during saving.
     */
    public void saveToFile(File file, List<Layer> layers) throws IOException {
        // Create a flattened image from all visible layers
        BufferedImage flattened = createFlattenedImage(layers);

        String fileName = file.getName().toLowerCase();
        String formatName = "png";

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            formatName = "jpeg";
            flattened = convertToRGB(flattened);
        } else if (fileName.endsWith(".bmp")) {
            formatName = "bmp";
            flattened = convertToRGB(flattened);
        }

        if (!ImageIO.write(flattened, formatName, file)) {
            throw new IOException("No appropriate writer found for format: " + formatName);
        }

        currentFilePath = file.getAbsolutePath();
    }

    /**
     * Creates a flattened image from all visible layers.
     *
     * @param layers The layers to flatten.
     * @return The flattened image.
     */
    private BufferedImage createFlattenedImage(List<Layer> layers) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("No layers to flatten");
        }

        // Get dimensions from the first layer
        int width = layers.get(0).getImage().getWidth();
        int height = layers.get(0).getImage().getHeight();

        // Create a new image to composite all layers
        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        // Draw all visible layers from bottom to top
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }
        g2d.dispose();

        return composite;
    }

    @Override
    public void saveToFile(File file) throws IOException {
        throw  new UnsupportedEncodingException("Direct calls unsupported. Use saveToFile(File file, List<Layer> layers) instead.");
    }

    /**
     * Loads a file, creating a single layer from the image.
     *
     * @param file The file to load.
     * @return A LayerState containing a single layer with the loaded image.
     * @throws IOException If an error occurs during loading.
     */
    public LayerState loadFromFile(File file) throws IOException {
        BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Failed to load image: " + file.getName());
        }

        // Create a single layer with the loaded image
        List<Layer> layers = new ArrayList<>();
        Layer layer = new Layer(loadedImage);
        layer.setName("Background");
        layers.add(layer);

        currentFilePath = file.getAbsolutePath();
        return new LayerState(layers, 0);
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

    @Override
    public void resetCurrentFilePath() {

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