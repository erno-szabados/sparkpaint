package com.esgdev.sparkpaint.ui;

import javax.swing.ImageIcon;
import java.awt.Image;

public class IconLoader {
    public static ImageIcon loadIcon(String iconName) {
        return new ImageIcon(IconLoader.class.getClassLoader().getResource("icons/" + iconName));
    }

    // Load and scale an icon to the desired width and height
    public static ImageIcon loadAndScaleIcon(String iconName, int width, int height) {
        ImageIcon originalIcon = loadIcon(iconName);
        if (originalIcon == null) {
            return null; // Handle missing icon gracefully
        }

        // Get the image from the original icon
        Image originalImage = originalIcon.getImage();

        // Scale the image to the desired size
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        // Return a new ImageIcon with the scaled image
        return new ImageIcon(scaledImage);
    }

}
