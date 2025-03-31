package com.esgdev.sparkpaint.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PaletteManagerTest {

    private PaletteManager paletteManager;
    private List<Color> testColors;
    private File tempFile;

    @Before
    public void setUp() {
        paletteManager = new PaletteManager();
        testColors = Arrays.asList(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.BLACK,
            new Color(128, 128, 128),
            new Color(50, 60, 70)  // Dark teal
        );
        tempFile = new File("test-palette");
    }

    @After
    public void tearDown() {
        // Clean up test files
        if (tempFile.exists()) {
            tempFile.delete();
        }
        File fileWithExt = new File(tempFile.getPath() + ".palette");
        if (fileWithExt.exists()) {
            fileWithExt.delete();
        }
    }

    @Test
    public void testSaveAndLoadPalette() throws IOException {
        // Save the palette
        paletteManager.savePalette(tempFile, testColors);

        // Verify file was created with correct extension
        File fileWithExt = new File(tempFile.getPath() + ".palette");
        assertTrue("Palette file should exist", fileWithExt.exists());

        // Load the palette
        List<Color> loadedColors = paletteManager.loadPalette(fileWithExt);

        // Verify colors match
        assertEquals("Number of colors should match", testColors.size(), loadedColors.size());

        for (int i = 0; i < testColors.size(); i++) {
            assertEquals("Color at index " + i + " should match",
                testColors.get(i).getRGB(), loadedColors.get(i).getRGB());
        }
    }

    @Test
    public void testSavePaletteAddsExtension() throws IOException {
        // Save without extension
        paletteManager.savePalette(tempFile, testColors);

        // Check that file with extension was created
        File fileWithExt = new File(tempFile.getPath() + ".palette");
        assertTrue("File with extension should exist", fileWithExt.exists());
    }

    @Test
    public void testSaveEmptyPalette() throws IOException {
        List<Color> emptyList = new ArrayList<>();
        paletteManager.savePalette(tempFile, emptyList);

        File fileWithExt = new File(tempFile.getPath() + ".palette");
        List<Color> loadedColors = paletteManager.loadPalette(fileWithExt);

        assertTrue("Loaded palette should be empty", loadedColors.isEmpty());
    }

    @Test(expected = IOException.class)
    public void testLoadNonexistentFile() throws IOException {
        File nonExistent = new File("non-existent-palette.palette");
        paletteManager.loadPalette(nonExistent);
    }

    @Test
    public void testColorEncoding() throws IOException {
        // Test just one specific color to verify hex encoding
        Color testColor = new Color(50, 60, 70); // Dark teal
        List<Color> singleColor = Arrays.asList(testColor);

        paletteManager.savePalette(tempFile, singleColor);

        List<Color> loadedColors = paletteManager.loadPalette(
            new File(tempFile.getPath() + ".palette"));

        assertEquals(testColor.getRGB(), loadedColors.get(0).getRGB());
    }
}