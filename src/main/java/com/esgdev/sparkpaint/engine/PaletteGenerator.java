package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Generates color palettes from images using k-means clustering and color scaling.
 * This class analyzes the colors in an image and creates a palette of
 * representative colors that characterize the image.
 */
public class PaletteGenerator {
    private static final int MAX_PIXELS_TO_SAMPLE = 10000;
    private static final int MAX_ITERATIONS = 20;
    private static final int BASE_CENTROIDS = 8; // Fixed number of base centroids
    private static final double CONVERGENCE_THRESHOLD = 0.01;
    private PaletteGenerationProgressListener progressListener;

    /**
     * Style options for palette generation
     */
    public enum PaletteStyle {
        BALANCED,  // Default with varied saturation and brightness
        VIVID,     // Higher brightness and saturation
        PASTEL,    // Higher brightness, lower saturation
        MUTED,     // Lower brightness, lower saturation
        DEEP,      // Lower brightness, higher saturation
        PLAYFUL    // Wide range of variations
    }

    public interface PaletteGenerationProgressListener {
        void onProgressUpdate(int progress, int max);
    }

    public void setProgressListener(PaletteGenerationProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Creates a color palette from all visible layers in the canvas.
     *
     * @param canvas The drawing canvas containing the layers
     * @param k      The number of colors to generate in the palette
     * @return A list of representative colors
     */
    public List<Color> generatePaletteFromCanvas(DrawingCanvas canvas, int k, PaletteStyle style) {
        BufferedImage flattenedImage = createFlattenedImage(canvas);
        return generatePalette(flattenedImage, k, style);
    }

    // Add an overload with default style
    public List<Color> generatePaletteFromCanvas(DrawingCanvas canvas, int k) {
        return generatePaletteFromCanvas(canvas, k, PaletteStyle.BALANCED);
    }

    /**
     * Creates a flattened image from all visible layers.
     *
     * @param canvas The drawing canvas with layers
     * @return A BufferedImage containing the flattened image
     */
    private BufferedImage createFlattenedImage(DrawingCanvas canvas) {
        List<Layer> layers = canvas.getLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }

        // Get dimensions from first layer
        BufferedImage firstLayer = layers.get(0).getImage();
        int width = firstLayer.getWidth();
        int height = firstLayer.getHeight();

        // Create new image for flattened result
        BufferedImage flattened = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = flattened.createGraphics();

        // Draw background if transparency visualization is disabled
        if (!canvas.isTransparencyVisualizationEnabled()) {
            g2d.setColor(canvas.getCanvasBackground());
            g2d.fillRect(0, 0, width, height);
        }

        // Draw all visible layers from bottom to top
        for (Layer layer : layers) {
            if (layer.isVisible()) {
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        }

        g2d.dispose();
        return flattened;
    }

    /**
     * Generates a palette of representative colors from an image using k-means clustering
     * followed by color scaling.
     *
     * @param image The image to analyze
     * @param k     The number of colors to generate
     * @return A list of representative colors, sorted by hue
     */
    public List<Color> generatePalette(BufferedImage image, int k, PaletteStyle style) {
        if (image == null) {
            return Collections.emptyList();
        }

        // Extract colors from image (sampling to avoid performance issues)
        List<Color> imageColors = sampleColors(image);
        if (imageColors.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Find base centroids (always 8)
        List<Color> baseCentroids = kMeansClustering(imageColors, BASE_CENTROIDS);

        // Step 2: Generate scaled variations to reach the desired palette size
        return generateScaledVariations(baseCentroids, k, style);
    }

    /**
     * Generates variations of base colors by scaling saturation and brightness.
     *
     * @param baseColors The base colors extracted from clustering
     * @param targetSize The desired palette size
     * @return An expanded palette with variations
     */
    private List<Color> generateScaledVariations(List<Color> baseColors, int targetSize, PaletteStyle style) {
        // Sort base colors by hue before equalizing
        List<Color> sortedBaseColors = sortColorsByHue(baseColors);
        List<Color> equalizedBaseColors = equalizeColorAttributes(sortedBaseColors);
        List<Color> filteredBaseColors = filterSimilarColors(equalizedBaseColors);
        List<Color> expandedPalette = new ArrayList<>();

        // Calculate variations needed per base color
        int variationsPerBase = (int) Math.ceil((double) targetSize / filteredBaseColors.size());

        // Select variation factors based on palette style
        List<float[]> variationFactors = getVariationFactorsByStyle(style);

        // Generate variations for each base color
        for (Color baseColor : filteredBaseColors) {
            if (expandedPalette.size() >= targetSize) break;

            // Generate variations for this base color
            List<Color> colorVariations = new ArrayList<>();
            for (float[] factors : variationFactors) {
                Color variation = adjustColorSaturationBrightness(baseColor, factors[0], factors[1]);
                colorVariations.add(variation);
            }

            // Sort variations by brightness
            colorVariations.sort(Comparator.comparingDouble(this::getBrightness));

            // Add variations to palette
            int variationsToAdd = Math.min(variationsPerBase, targetSize - expandedPalette.size());
            int step = Math.max(1, colorVariations.size() / variationsToAdd);
            for (int i = 0; i < variationsToAdd && i * step < colorVariations.size(); i++) {
                expandedPalette.add(colorVariations.get(i * step));
            }
        }

        // Add extreme variations if needed
        if (expandedPalette.size() < targetSize) {
            float[] extremeFactors = getExtremeFactorsByStyle(style);
            for (Color baseColor : filteredBaseColors) {
                if (expandedPalette.size() >= targetSize) break;
                expandedPalette.add(adjustColorSaturationBrightness(baseColor, extremeFactors[0], extremeFactors[1]));
            }
        }

        return expandedPalette.subList(0, Math.min(targetSize, expandedPalette.size()));
    }

    /**
     * Gets variation factors appropriate for the selected palette style
     */
    private List<float[]> getVariationFactorsByStyle(PaletteStyle style) {
        List<float[]> factors = new ArrayList<>();

        switch (style) {
            case VIVID:
                // Higher brightness and saturation
                factors.add(new float[]{1.2f, 1.0f});   // More saturated
                factors.add(new float[]{1.3f, 1.1f});   // More saturated, brighter
                factors.add(new float[]{1.0f, 1.2f});   // Original saturation, brighter
                factors.add(new float[]{1.4f, 1.0f});   // Even more saturated
                break;

            case PASTEL:
                // Higher brightness, lower saturation
                factors.add(new float[]{0.6f, 1.2f});   // Desaturated, brighter
                factors.add(new float[]{0.5f, 1.3f});   // More desaturated, brighter
                factors.add(new float[]{0.7f, 1.1f});   // Slightly desaturated, slightly brighter
                factors.add(new float[]{0.4f, 1.2f});   // Very desaturated, brighter
                break;

            case MUTED:
                // Lower brightness and saturation
                factors.add(new float[]{0.7f, 0.9f});   // Desaturated, slightly darker
                factors.add(new float[]{0.6f, 0.8f});   // More desaturated, darker
                factors.add(new float[]{0.8f, 0.85f});  // Slightly desaturated, darker
                factors.add(new float[]{0.5f, 0.75f});  // Very desaturated, very dark
                break;

            case DEEP:
                // Lower brightness, higher saturation
                factors.add(new float[]{1.2f, 0.8f});   // More saturated, darker
                factors.add(new float[]{1.3f, 0.7f});   // Even more saturated, darker
                factors.add(new float[]{1.1f, 0.9f});   // Slightly more saturated, slightly darker
                factors.add(new float[]{1.4f, 0.75f});  // Very saturated, darker
                break;

            case PLAYFUL:
                // Wide range of variations
                factors.add(new float[]{0.5f, 1.3f});   // Desaturated, brighter
                factors.add(new float[]{1.3f, 0.7f});   // More saturated, darker
                factors.add(new float[]{0.7f, 1.1f});   // Slightly desaturated, slightly brighter
                factors.add(new float[]{1.2f, 1.2f});   // More saturated, brighter
                factors.add(new float[]{0.6f, 0.8f});   // More desaturated, darker
                factors.add(new float[]{1.4f, 1.0f});   // Very saturated, original brightness
                break;

            case BALANCED:
            default:
                // Default balance of variations
                factors.add(new float[]{1.0f, 0.8f});   // Original saturation, darker
                factors.add(new float[]{0.7f, 0.9f});   // Desaturated, slightly darker
                factors.add(new float[]{1.3f, 0.9f});   // More saturated, slightly darker
                factors.add(new float[]{0.7f, 1.2f});   // Desaturated, brighter
                factors.add(new float[]{1.0f, 1.2f});   // Original saturation, brighter
                factors.add(new float[]{1.3f, 1.1f});   // More saturated, slightly brighter
                break;
        }

        return factors;
    }

    /**
     * Gets extreme variation factors for when additional colors are needed
     */
    private float[] getExtremeFactorsByStyle(PaletteStyle style) {
        switch (style) {
            case VIVID:
                return new float[]{1.5f, 1.3f};  // Very saturated, very bright
            case PASTEL:
                return new float[]{0.3f, 1.4f};  // Very desaturated, very bright
            case MUTED:
                return new float[]{0.4f, 0.6f};  // Very desaturated, very dark
            case DEEP:
                return new float[]{1.5f, 0.6f};  // Very saturated, very dark
            case PLAYFUL:
                return new float[]{1.6f, 1.4f};  // Extremely saturated and bright
            case BALANCED:
            default:
                return new float[]{0.5f, 1.5f};  // Default extreme variation
        }
    }

    /**
     * Gets the brightness value of a color (0-1)
     */
    private double getBrightness(Color color) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        return hsb[2];
    }

    /**
     * Equalizes color attributes across the base colors for more uniform scaling.
     *
     * @param colors Colors to equalize
     * @return A new list with equalized colors
     */
    private List<Color> equalizeColorAttributes(List<Color> colors) {
        if (colors.isEmpty()) return new ArrayList<>();

        // Calculate average saturation and brightness
        float avgSaturation = 0;
        float avgBrightness = 0;

        for (Color color : colors) {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            avgSaturation += hsb[1];
            avgBrightness += hsb[2];
        }

        avgSaturation /= colors.size();
        avgBrightness /= colors.size();

        // Target saturation and brightness (slightly enhanced)
        float targetSaturation = Math.min(1.0f, avgSaturation * 1.1f);
        float targetBrightness = Math.min(1.0f, avgBrightness * 1.05f);

        // Create equalized colors
        List<Color> equalizedColors = new ArrayList<>();
        for (Color color : colors) {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

            // Partially equalize saturation and brightness (keep some of the original characteristics)
            float newSat = hsb[1] * 0.7f + targetSaturation * 0.3f;
            float newBri = hsb[2] * 0.7f + targetBrightness * 0.3f;

            equalizedColors.add(Color.getHSBColor(hsb[0], newSat, newBri));
        }

        return equalizedColors;
    }

    /**
     * Filters base colors to remove very similar ones and ensure greater diversity.
     *
     * @param colors The colors to filter
     * @return A filtered list with similar colors removed
     */
    private List<Color> filterSimilarColors(List<Color> colors) {
        if (colors.size() <= 1) return new ArrayList<>(colors);

        List<Color> filtered = new ArrayList<>();
        filtered.add(colors.get(0)); // Always include the first color

        // Similarity threshold - adjust as needed
        double similarityThreshold = 15.0;

        // Check each color against already filtered colors
        for (int i = 1; i < colors.size(); i++) {
            Color candidate = colors.get(i);
            boolean isSimilar = false;

            for (Color existingColor : filtered) {
                if (colorDistance(candidate, existingColor) < similarityThreshold) {
                    isSimilar = true;
                    break;
                }
            }

            if (!isSimilar) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    /**
     * Adjusts the saturation and brightness of a color.
     *
     * @param color            The original color
     * @param saturationFactor Factor to adjust saturation (>1 increases, <1 decreases)
     * @param brightnessFactor Factor to adjust brightness (>1 increases, <1 decreases)
     * @return The adjusted color
     */
    private Color adjustColorSaturationBrightness(Color color, float saturationFactor, float brightnessFactor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsb[1] = Math.min(1.0f, hsb[1] * saturationFactor); // Scale saturation
        hsb[2] = Math.min(1.0f, hsb[2] * brightnessFactor); // Scale brightness
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Sorts colors by hue (rainbow order)
     *
     * @param colors The colors to sort
     * @return A new list containing the sorted colors
     */
    private List<Color> sortColorsByHue(List<Color> colors) {
        List<Color> sorted = new ArrayList<>(colors);
        sorted.sort(Comparator.comparingDouble(this::getHue));
        return sorted;
    }

    /**
     * Gets the hue value of a color (0-360)
     */
    private float getHue(Color color) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        return hsb[0] * 360;
    }

    /**
     * Samples colors from the image, limiting the number of pixels processed.
     *
     * @param image The image to sample colors from
     * @return A list of sampled colors
     */
    private List<Color> sampleColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<Color> colors = new ArrayList<>();
        Random random = new Random();

        int totalPixels = width * height;
        int pixelsToSample = Math.min(totalPixels, MAX_PIXELS_TO_SAMPLE);

        for (int i = 0; i < pixelsToSample; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int rgb = image.getRGB(x, y);
            // Skip fully transparent pixels
            if ((rgb >>> 24) == 0) {
                continue;
            }
            colors.add(new Color(rgb, true));
        }

        return colors;
    }

    /**
     * Performs k-means clustering on a list of colors to find representative colors.
     *
     * @param colors The colors to cluster
     * @param k      The number of clusters (palette size)
     * @return A list of representative colors (centroids)
     */
    private List<Color> kMeansClustering(List<Color> colors, int k) {
        if (colors.size() <= k) {
            return new ArrayList<>(colors);
        }

        // Initialize centroids randomly from input colors
        List<Color> centroids = initializeCentroids(colors, k);

        // Create a map to store cluster sizes (for weighting)
        Map<Color, Integer> clusterSizes = new HashMap<>();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Report progress
            if (progressListener != null) {
                progressListener.onProgressUpdate(iteration, MAX_ITERATIONS);
            }

            // Assign colors to clusters
            Map<Color, List<Color>> clusters = new HashMap<>();
            for (Color centroid : centroids) {
                clusters.put(centroid, new ArrayList<>());
            }

            for (Color color : colors) {
                Color closestCentroid = findClosestCentroid(color, centroids);
                clusters.get(closestCentroid).add(color);
            }

            // Update centroids
            List<Color> newCentroids = new ArrayList<>();
            boolean converged = true;

            for (Color oldCentroid : centroids) {
                List<Color> clusterColors = clusters.get(oldCentroid);
                if (clusterColors.isEmpty()) {
                    // Keep the old centroid if the cluster is empty
                    newCentroids.add(oldCentroid);
                    clusterSizes.put(oldCentroid, 0);
                    continue;
                }

                Color newCentroid = calculateCentroid(clusterColors);
                newCentroids.add(newCentroid);
                clusterSizes.put(newCentroid, clusterColors.size());

                // Check for convergence
                double distance = colorDistance(oldCentroid, newCentroid);
                if (distance > CONVERGENCE_THRESHOLD) {
                    converged = false;
                }
            }

            centroids = newCentroids;

            if (converged) {
                if (progressListener != null) {
                    progressListener.onProgressUpdate(MAX_ITERATIONS, MAX_ITERATIONS);
                }
                break;
            }
        }

        // Add this after the loop to ensure 100% progress
        if (progressListener != null) {
            progressListener.onProgressUpdate(MAX_ITERATIONS, MAX_ITERATIONS);
        }

        return centroids;
    }

    /**
     * Initializes centroids using the k-means++ algorithm.
     * This provides better initial centroids than random selection.
     *
     * @param colors The set of colors to choose centroids from
     * @param k      The number of centroids to initialize
     * @return A list of initialized centroids
     */
    private List<Color> initializeCentroids(List<Color> colors, int k) {
        List<Color> centroids = new ArrayList<>();
        Random random = new Random();

        // Select first centroid randomly
        if (!colors.isEmpty()) {
            centroids.add(colors.get(random.nextInt(colors.size())));
        }

        // Select remaining centroids using k-means++ approach
        while (centroids.size() < k && centroids.size() < colors.size()) {
            // Array to hold the squared distances
            double[] distances = new double[colors.size()];
            double totalDistance = 0;

            // Calculate squared distance from each point to nearest centroid
            for (int i = 0; i < colors.size(); i++) {
                Color color = colors.get(i);
                // Skip if already a centroid
                if (centroids.contains(color)) {
                    distances[i] = 0;
                    continue;
                }

                // Find minimum squared distance to any existing centroid
                double minDist = Double.MAX_VALUE;
                for (Color centroid : centroids) {
                    double dist = colorDistance(color, centroid);
                    minDist = Math.min(minDist, dist * dist);
                }

                distances[i] = minDist;
                totalDistance += minDist;
            }

            // If all distances are zero, break
            if (totalDistance <= 0.0001) {
                break;
            }

            // Select next centroid with probability proportional to squared distance
            double threshold = random.nextDouble() * totalDistance;
            double cumulativeDistance = 0;
            int selectedIndex = -1;

            for (int i = 0; i < distances.length; i++) {
                cumulativeDistance += distances[i];
                if (cumulativeDistance >= threshold) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex >= 0) {
                centroids.add(colors.get(selectedIndex));
            } else {
                // Fallback - add random color
                centroids.add(colors.get(random.nextInt(colors.size())));
            }
        }

        // If we couldn't find enough distinct colors, fill remaining slots randomly
        while (centroids.size() < k && !colors.isEmpty()) {
            centroids.add(colors.get(random.nextInt(colors.size())));
        }

        return centroids;
    }

    /**
     * Finds the closest centroid to a given color.
     *
     * @param color     The color to find the closest centroid for
     * @param centroids The list of centroids to compare against
     * @return The closest centroid color
     */
    private Color findClosestCentroid(Color color, List<Color> centroids) {
        Color closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Color centroid : centroids) {
            double distance = colorDistance(color, centroid);
            if (distance < minDistance) {
                minDistance = distance;
                closest = centroid;
            }
        }

        return closest;
    }

    /**
     * Calculates the centroid (average color) of a cluster.
     *
     * @param colors The colors in the cluster
     * @return The centroid color
     */
    private Color calculateCentroid(List<Color> colors) {
        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;

        for (Color color : colors) {
            redSum += color.getRed();
            greenSum += color.getGreen();
            blueSum += color.getBlue();
        }

        int count = colors.size();
        return new Color(
                (int) (redSum / count),
                (int) (greenSum / count),
                (int) (blueSum / count)
        );
    }

    /**
     * Calculates the Euclidean distance between two colors in RGB space.
     *
     * @param c1 The first color
     * @param c2 The second color
     * @return The distance between the colors
     */
    private double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}