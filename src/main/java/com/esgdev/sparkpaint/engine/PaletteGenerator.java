package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.layer.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Generates color palettes from images using k-means clustering.
 * This class analyzes the colors in an image and creates a palette of
 * representative colors that characterize the image.
 */
public class PaletteGenerator {
    private static final int MAX_PIXELS_TO_SAMPLE = 10000;
    private static final int MAX_ITERATIONS = 20;
    private static final double CONVERGENCE_THRESHOLD = 0.01;
    private PaletteGenerationProgressListener progressListener;

    public enum ColorSortMethod {
        NONE,       // No sorting, use cluster order
        HUE,        // Sort by hue (rainbow order)
        LUMINANCE,  // Sort by brightness/luminance
        SATURATION  // Sort by color saturation
    }

    private ColorSortMethod sortMethod = ColorSortMethod.HUE; // Default sort method


    public interface PaletteGenerationProgressListener {
        void onProgressUpdate(int progress, int max);
    }

    public void setProgressListener(PaletteGenerationProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Sets the color sorting method for generated palettes
     *
     * @param method The sorting method to use
     */
    public void setSortMethod(ColorSortMethod method) {
        this.sortMethod = method;
    }

    /**
     * Gets the current color sorting method
     *
     * @return The current sorting method
     */
    public ColorSortMethod getSortMethod() {
        return sortMethod;
    }

    /**
     * Creates a color palette from all visible layers in the canvas.
     *
     * @param canvas The drawing canvas containing the layers
     * @param k The number of colors to generate in the palette
     * @return A list of representative colors
     */
    public List<Color> generatePaletteFromCanvas(DrawingCanvas canvas, int k) {
        BufferedImage flattenedImage = createFlattenedImage(canvas);
        return generatePalette(flattenedImage, k);
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
     * Generates a palette of representative colors from an image using k-means clustering.
     *
     * @param image The image to analyze
     * @param k The number of colors to generate
     * @return A list of representative colors, sorted according to the current sort method
     */
    public List<Color> generatePalette(BufferedImage image, int k) {
        if (image == null) {
            return Collections.emptyList();
        }

        // Extract colors from image (sampling to avoid performance issues)
        List<Color> imageColors = sampleColors(image);
        if (imageColors.isEmpty()) {
            return Collections.emptyList();
        }

        // Perform k-means clustering to find representative colors
        List<Color> centroids = kMeansClustering(imageColors, k);

        // Sort the colors if a sort method is specified
        if (sortMethod != ColorSortMethod.NONE) {
            return sortColors(centroids, sortMethod);
        }

        return centroids;
    }

    /**
     * Sorts a list of colors according to the specified sort method
     *
     * @param colors The colors to sort
     * @param method The sorting method to use
     * @return A new list containing the sorted colors
     */
    private List<Color> sortColors(List<Color> colors, ColorSortMethod method) {
        List<Color> sorted = new ArrayList<>(colors);

        switch (method) {
            case HUE:
                sorted.sort(Comparator.comparingDouble(this::getHue));
                break;
            case LUMINANCE:
                sorted.sort(Comparator.comparingDouble(this::getLuminance));
                break;
            case SATURATION:
                sorted.sort(Comparator.comparingDouble(this::getSaturation));
                break;
            case NONE:
            default:
                // No sorting
                break;
        }

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
     * Gets the luminance value of a color (0-1)
     * Using the perceptual luminance formula: 0.299*R + 0.587*G + 0.114*B
     */
    private double getLuminance(Color color) {
        return (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
    }

    /**
     * Gets the saturation value of a color (0-1)
     */
    private float getSaturation(Color color) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        return hsb[1];
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
     * @param k The number of clusters (palette size)
     * @return A list of representative colors (centroids)
     */
    private List<Color> kMeansClustering(List<Color> colors, int k) {
        if (colors.size() <= k) {
            return new ArrayList<>(colors);
        }

        // Initialize centroids randomly from input colors
        List<Color> centroids = initializeCentroids(colors, k);

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
                    continue;
                }

                Color newCentroid = calculateCentroid(clusterColors);
                newCentroids.add(newCentroid);

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

    private List<Color> initializeCentroids(List<Color> colors, int k) {
        List<Color> centroids = new ArrayList<>();
        Random random = new Random();

        // Pick first centroid randomly
        if (!colors.isEmpty()) {
            centroids.add(colors.get(random.nextInt(colors.size())));
        }

        // Pick remaining centroids using maximin approach (maximizing minimum distance)
        while (centroids.size() < k && centroids.size() < colors.size()) {
            Color farthestColor = null;
            double maxMinDistance = -1;

            for (Color color : colors) {
                if (centroids.contains(color)) continue;

                // Find minimum distance to any existing centroid
                double minDistance = Double.MAX_VALUE;
                for (Color centroid : centroids) {
                    double distance = colorDistance(color, centroid);
                    minDistance = Math.min(minDistance, distance);
                }

                // Keep track of color with maximum minimum distance
                if (minDistance > maxMinDistance) {
                    maxMinDistance = minDistance;
                    farthestColor = color;
                }
            }

            if (farthestColor != null) {
                centroids.add(farthestColor);
            } else {
                break; // No more colors to add
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
     * @param color The color to find the closest centroid for
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
            (int)(redSum / count),
            (int)(greenSum / count),
            (int)(blueSum / count)
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