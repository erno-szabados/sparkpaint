package com.esgdev.sparkpaint.io;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PaletteManager {
    private static final String DEFAULT_EXTENSION = ".palette";

    public void savePalette(File file, List<Color> colors) throws IOException {
        Properties props = new Properties();
        for (int i = 0; i < colors.size(); i++) {
            Color c = colors.get(i);
            props.setProperty("color." + i,
                String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
        }

        // Ensure file has correct extension
        String path = file.getPath();
        if (!path.toLowerCase().endsWith(DEFAULT_EXTENSION)) {
            file = new File(path + DEFAULT_EXTENSION);
        }

        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "SparkPaint Color Palette");
        }
    }

    public List<Color> loadPalette(File file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }

        List<Color> colors = new ArrayList<>();
        int i = 0;
        String colorStr;
        while ((colorStr = props.getProperty("color." + i)) != null) {
            colors.add(Color.decode(colorStr));
            i++;
        }
        return colors;
    }
}