package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;

import javax.swing.*;
import java.awt.*;


public class MainFrame extends JFrame {
    private static final int IconWidth = 24;
    private static final int IconHeight = 24;
    private DrawingCanvas canvas;
    private JLabel statusMessage;
    private JLabel cursorPositionLabel;

    public MainFrame() {
        // Set up the JFrame properties
        super("SparkPaint Pixel Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null); // Centers the window on the screen

        // Initialize the main components
        initializeUI();
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    // Method to initialize the UI components
    private void initializeUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Create and set up the menu bar
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(new FileMenu(this));  // Add our new FileMenu
        setJMenuBar(menuBar);  // Add menubar to the frame

        // Add toolbar
        JToolBar toolbar = createToolBar();
        contentPane.add(toolbar, BorderLayout.WEST);

        // Add main canvas (at the center)
        canvas = new DrawingCanvas();
        // Create a container panel with GridBagLayout
        JPanel canvasContainer = new JPanel(new GridBagLayout());

        // Create constraints for the canvas
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;  // Don't expand horizontally
        gbc.weighty = 0;  // Don't expand vertically
        gbc.anchor = GridBagConstraints.CENTER;  // Center the canvas

        // Add the canvas to the container with constraints
        canvasContainer.add(canvas, gbc);

        // Add the container to the center of the BorderLayout
        add(canvasContainer, BorderLayout.CENTER);


        // Add status bar (at the bottom)
        JPanel statusBar = new JPanel(new BorderLayout());
        statusMessage = new JLabel("Ready");
        statusBar.add(statusMessage, BorderLayout.WEST);


        // Add cursor position label on the right side of the status bar
        cursorPositionLabel = new JLabel("Cursor: (0, 0)");
        cursorPositionLabel.setHorizontalAlignment(SwingConstants.RIGHT); // Align text to the right
        statusBar.add(cursorPositionLabel, BorderLayout.EAST);

        contentPane.add(statusBar, BorderLayout.SOUTH);

        // Attach a listener to update the cursor position label
        addCursorTracking();

    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    // Method to create a basic toolbar
    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
        toolbar.setFloatable(false); // Disable floating toolbar

        // Load and scale icons
        ImageIcon pencilIcon = IconLoader.loadAndScaleIcon("pencil.png", IconWidth, IconHeight);
        ImageIcon lineIcon = IconLoader.loadAndScaleIcon("line.png", IconWidth, IconHeight);
        ImageIcon rectangleIcon = IconLoader.loadAndScaleIcon("rect-outline.png", IconWidth, IconHeight);

        // Create Pencil button
        JButton pencilButton = new JButton();
        pencilButton.setIcon(pencilIcon);
        pencilButton.setToolTipText("Draw with Pencil");
        pencilButton.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.PENCIL));
        pencilButton.addActionListener(e -> statusMessage.setText("Pencil selected."));
        toolbar.add(pencilButton);

        // Create Line button
        JButton lineButton = new JButton();
        lineButton.setIcon(lineIcon);
        lineButton.setToolTipText("Draw a Line");
        lineButton.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.LINE));
        lineButton.addActionListener(e -> statusMessage.setText("Line selected."));
        toolbar.add(lineButton);

        // Create Rectangle button
        JButton rectangleButton = new JButton();
        rectangleButton.setIcon(rectangleIcon);
        rectangleButton.setToolTipText("Draw a Rectangle");
        rectangleButton.addActionListener(e -> canvas.setCurrentTool(DrawingCanvas.Tool.RECTANGLE));
        rectangleButton.addActionListener(e -> statusMessage.setText("Rectangle selected."));
        toolbar.add(rectangleButton);

        JButton colorButton = getColorButton();

        toolbar.add(colorButton);

        return toolbar;
    }

    private JButton getColorButton() {
        JButton colorButton = new JButton();
        //colorButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        Icon colorIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(canvas.getDrawingColor()); // Assuming canvas provides the current color
                g.fillRect(x, y, IconWidth, IconHeight);
            }

            @Override
            public int getIconWidth() {
                return IconWidth;
            }

            @Override
            public int getIconHeight() {
                return IconHeight;
            }
        };

        // Set the icon to the button
        colorButton.setIcon(colorIcon);
        //colorButton.setMargin(new Insets(0, 0, 0, 0));
        colorButton.setBackground(Color.BLACK); // Initial color
        colorButton.setToolTipText("Choose Drawing Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color",
                    colorButton.getBackground());
            if (newColor != null) {
                colorButton.setBackground(newColor);
                // Update the hex color label
                colorButton.setToolTipText(String.format("#%02X%02X%02X",
                        newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                // Update the canvas drawing color
                canvas.setDrawingColor(newColor);
            }
        });
        return colorButton;
    }


    // Add a mouse motion listener to track and update cursor position
    private void addCursorTracking() {
        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                // Update the label with current cursor position
                cursorPositionLabel.setText("Cursor: (" + e.getX() + ", " + e.getY() + ")");
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                // Keep updating the label while dragging
                cursorPositionLabel.setText("Cursor: (" + e.getX() + ", " + e.getY() + ")");
            }
        });

        // Optional: Add a listener for when the mouse exits the canvas
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cursorPositionLabel.setText("Cursor: (out of bounds)");
            }
        });
    }
}