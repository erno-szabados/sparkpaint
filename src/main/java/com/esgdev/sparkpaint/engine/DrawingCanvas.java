package com.esgdev.sparkpaint.engine;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DrawingCanvas extends JPanel {
    private String currentFilePath;

    public enum Tool {
        PENCIL,
        LINE,
        RECTANGLE
    }

    private Image image;
    private Graphics2D graphics;
    private BufferedImage tempCanvas; // Temporary canvas for drawing previews
    private int startX, startY;
    private Tool currentTool = Tool.PENCIL;

    public DrawingCanvas() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();

                // Initialize the canvas if not done already
                if (graphics == null) {
                    initializeCanvas();
                }

                // If tool is LINE or RECTANGLE, save the current canvas state
                if (currentTool == Tool.LINE || currentTool == Tool.RECTANGLE) {
                    saveCanvasState();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int endX = e.getX();
                int endY = e.getY();

                // Finalize the shape based on the selected tool
                if (graphics != null) {
                    switch (currentTool) {
                        case LINE:
                            // Finalize the line
                            graphics.drawLine(startX, startY, endX, endY);
                            repaint();
                            break;
                        case RECTANGLE:
                            // Finalize the rectangle
                            int rectX = Math.min(startX, endX);
                            int rectY = Math.min(startY, endY);
                            int rectWidth = Math.abs(endX - startX);
                            int rectHeight = Math.abs(endY - startY);
                            graphics.drawRect(rectX, rectY, rectWidth, rectHeight);
                            repaint();
                            break;
                        default:
                            break;
                    }

                    // Clear the temporary canvas
                    tempCanvas = null;
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool == Tool.PENCIL) {
                    // Pencil drawing
                    int x = e.getX();
                    int y = e.getY();

                    if (graphics != null) {
                        graphics.drawLine(startX, startY, x, y);
                        repaint();
                        startX = x; // Update start point for continuous freehand drawing
                        startY = y;
                    }
                } else if (currentTool == Tool.LINE || currentTool == Tool.RECTANGLE) {
                    // Draw a temporary preview of the shape
                    if (tempCanvas != null) {
                        Graphics2D tempGraphics = tempCanvas.createGraphics();
                        tempGraphics.drawImage(image, 0, 0, null); // Restore the original canvas

                        // Preview the appropriate shape
                        if (currentTool == Tool.LINE) {
                            tempGraphics.setColor(Color.BLACK);
                            tempGraphics.setStroke(new BasicStroke(2));
                            tempGraphics.drawLine(startX, startY, e.getX(), e.getY());
                        } else if (currentTool == Tool.RECTANGLE) {
                            tempGraphics.setColor(Color.BLACK);
                            tempGraphics.setStroke(new BasicStroke(2));
                            int rectX = Math.min(startX, e.getX());
                            int rectY = Math.min(startY, e.getY());
                            int rectWidth = Math.abs(e.getX() - startX);
                            int rectHeight = Math.abs(e.getY() - startY);
                            tempGraphics.drawRect(rectX, rectY, rectWidth, rectHeight);
                        }

                        tempGraphics.dispose();
                        repaint(); // Update the canvas to show the temporary shape
                    }
                }
            }
        });
    }

    public void createNewCanvas(int width, int height, Color backgroundColor) {
        // Create new buffered images with new dimensions
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage newTempCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get graphics context from new image
        Graphics2D newGraphics = (Graphics2D) newImage.getGraphics();

        // Copy existing graphics settings if they exist
        if (graphics != null) {
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
            newGraphics.setColor(graphics.getColor());
            newGraphics.setStroke(graphics.getStroke());
        } else {
            // Initialize default settings for first creation
            newGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // Fill with background color
        newGraphics.setColor(backgroundColor);
        newGraphics.fillRect(0, 0, width, height);

        // Restore previous drawing color if it exists
        if (graphics != null) {
            newGraphics.setColor(graphics.getColor());
        }

        // Update class fields
        if (graphics != null) {
            graphics.dispose(); // Clean up old graphics context
        }

        image = newImage;
        graphics = newGraphics;
        tempCanvas = newTempCanvas;

        // Update panel size
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }


    public void saveToFile(File file) throws IOException {
        // Get the image from the canvas
        BufferedImage imageToSave = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );

        // Draw the current canvas state to the new image
        Graphics2D g = imageToSave.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Get the file extension
        String fileName = file.getName().toLowerCase();
        String formatName = "png"; // default format

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            formatName = "jpeg";
            // Convert to RGB for JPEG (no alpha channel)
            BufferedImage rgbImage = new BufferedImage(
                    imageToSave.getWidth(),
                    imageToSave.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D rgbGraphics = rgbImage.createGraphics();
            rgbGraphics.drawImage(imageToSave, 0, 0, Color.WHITE, null);
            rgbGraphics.dispose();
            imageToSave = rgbImage;
        }

        // Save the image
        if (!ImageIO.write(imageToSave, formatName, file)) {
            throw new IOException("No appropriate writer found for format: " + formatName);
        }

        currentFilePath = file.getAbsolutePath();
    }

    public void loadFromFile(File file) throws IOException {
        BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Failed to load image: Unsupported or corrupted file");
        }

        // Store current graphics settings before creating new image
        Color currentColor = graphics != null ? graphics.getColor() : Color.BLACK;
        Stroke currentStroke = graphics != null ? graphics.getStroke() : new BasicStroke(1);

        // Create a new buffered image with ARGB support
        image = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        graphics = (Graphics2D) image.getGraphics();

        // Restore graphics settings
        graphics.setColor(currentColor);
        graphics.setStroke(currentStroke);

        // Draw the loaded image
        graphics.drawImage(loadedImage, 0, 0, null);

        currentFilePath = file.getAbsolutePath();
        setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        repaint();
    }


    public String getCurrentFilePath() {
        return currentFilePath;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the permanent canvas
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }

        // Draw the temporary canvas on top
        if (tempCanvas != null) {
            g.drawImage(tempCanvas, 0, 0, null);
        }
    }

    // Initialize the canvas image and graphics object
    private void initializeCanvas() {
        image = createImage(getWidth(), getHeight());
        graphics = (Graphics2D) image.getGraphics();

        // Set initial properties for the graphics object
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(2));
    }

    // Save the current canvas state to a temporary buffer
    private void saveCanvasState() {
        tempCanvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempGraphics = tempCanvas.createGraphics();
        tempGraphics.drawImage(image, 0, 0, null); // Copy the permanent canvas to the temporary canvas
        tempGraphics.dispose();
    }

    // Setter for the current tool
    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
        // Update the cursor based on the selected tool
        if (tool == Tool.PENCIL || tool == Tool.LINE || tool == Tool.RECTANGLE) {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); // Crosshair cursor for drawing tools
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // Default cursor
        }
    }

    // Getter for the current tool
    public Tool getCurrentTool() {
        return currentTool;
    }
}