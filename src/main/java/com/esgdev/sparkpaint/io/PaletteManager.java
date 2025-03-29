package com.esgdev.sparkpaint.io;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PaletteManager {
    private static final String DEFAULT_EXTENSION = ".palette";
    private List<Color> activePalette = new ArrayList<>();
    private final List<PaletteChangeListener> listeners = new ArrayList<>();


    /**
     * Interface for listeners that want to be notified of palette changes
     */
    public interface PaletteChangeListener {
        void onPaletteChanged(List<Color> newPalette);
    }


    /**
     * Add a listener to be notified when the palette changes
     */
    public void addPaletteChangeListener(PaletteChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


    /**
     * Remove a previously added listener
     */
    public void removePaletteChangeListener(PaletteChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all registered listeners about a palette change
     */
    private void notifyListeners() {
        for (PaletteChangeListener listener : listeners) {
            listener.onPaletteChanged(new ArrayList<>(activePalette));
        }
    }


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

        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            props.store(out, "SparkPaint Color Palette");
        }
    }

    public List<Color> loadPalette(File file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file.toPath())) {
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

    public void setActivePalette(List<Color> palette) {
        this.activePalette = new ArrayList<>(palette);
        notifyListeners();
    }

    public List<Color> getActivePalette() {
        return new ArrayList<>(activePalette);
    }
}