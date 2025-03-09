package com.esgdev.sparkpaint.ui;

import com.esgdev.sparkpaint.engine.DrawingCanvas;
import com.esgdev.sparkpaint.ui.HelpMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;



public class MainFrame extends JFrame {

    private DrawingCanvas canvas;
    private JLabel statusMessage;
    private JLabel cursorPositionLabel;
    private ColorPalette palette;

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

    public ColorPalette getColorPalette() {
        return palette;
    }

    // Method to initialize the UI components
    private void initializeUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        // Set a reasonable minimum size for the frame
        setMinimumSize(new Dimension(800, 600));


        // Add main canvas (at the center). Must precede menu, toolbar and tool settings!
        canvas = new DrawingCanvas();

        // Create and set up the menu bar
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new FileMenu(this));
        menuBar.add(new EditMenu(this));
        menuBar.add(new ImageMenu(this));
        menuBar.add(new HelpMenu());
        //menuBar.add(new HelpMenu(this)); // Uncomment after implementing HelpMenu class
        setJMenuBar(menuBar);

        // Create constraints for the canvas
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;  // Don't expand horizontally
        gbc.weighty = 0;  // Don't expand vertically
        gbc.anchor = GridBagConstraints.CENTER;  // Center the canvas
        //gbc.fill = GridBagConstraints.BOTH;

        JPanel canvasContainer = new JPanel(new GridBagLayout());
        // Add the canvas to the container with constraints
        canvasContainer.add(canvas, gbc);

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(canvasContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);



        // Add toolbar
        JToolBar toolbar = new DrawingToolbar(canvas, this::setStatusMessage);
        contentPane.add(toolbar, BorderLayout.WEST);

        // Add tool settings
        Box toolBox = new DrawingSettingsToolBox(canvas, this::setStatusMessage);
        // Create the split pane with your existing components
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scrollPane,                // left component
                toolBox                    // right component
        );
        splitPane.setOneTouchExpandable(true);  // Adds a small button to collapse/expand
        splitPane.setDividerLocation(getWidth() - 200);  // Initial divider location
        splitPane.setResizeWeight(1.0);
        // Add a ComponentListener to the right component
        toolBox.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Check the size of the right component
                if (toolBox.getWidth() > DrawingSettingsToolBox.MaxWidth) { // Set to desired maximum width
                    splitPane.setDividerLocation(splitPane.getWidth() - DrawingSettingsToolBox.MaxWidth); // Maintain the right component's maximum width
                }
            }
        });

        // Add the container to the center of the BorderLayout
        add(splitPane, BorderLayout.CENTER);
        //contentPane.add(splitPane, BorderLayout.EAST);

        // Add color palette (just before status bar)
        palette = new ColorPalette(canvas);
        contentPane.add(palette, BorderLayout.NORTH);

        // Add status bar (at the bottom)
        JPanel statusBar = new JPanel(new BorderLayout());
        statusMessage = new JLabel("Ready");
        statusBar.add(statusMessage, BorderLayout.WEST);

        // Center: Add zoom factor display
        JLabel zoomLabel = new JLabel("100%");
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusBar.add(zoomLabel, BorderLayout.CENTER);

        // Add cursor position label on the right side of the status bar
        cursorPositionLabel = new JLabel("Cursor: (0, 0)");
        cursorPositionLabel.setHorizontalAlignment(SwingConstants.RIGHT); // Align text to the right
        statusBar.add(cursorPositionLabel, BorderLayout.EAST);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        // Update zoom label when zoom changes
        canvas.addMouseWheelListener(e -> {
            int zoomPercentage = Math.round(canvas.getZoomFactor() * 100);
            zoomLabel.setText(String.format("%d%%", zoomPercentage));
        });

        // Attach a listener to update the cursor position label
        addCursorTracking();
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    private void addCursorTracking() {
        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                float zoomFactor = canvas.getZoomFactor();
                int x = (int) (e.getX() / zoomFactor);
                int y = (int) (e.getY() / zoomFactor);
                cursorPositionLabel.setText(String.format("Cursor: (%d, %d)", x, y));
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                float zoomFactor = canvas.getZoomFactor();
                int x = (int) (e.getX() / zoomFactor);
                int y = (int) (e.getY() / zoomFactor);
                cursorPositionLabel.setText(String.format("Cursor: (%d, %d)", x, y));
            }
        });

        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cursorPositionLabel.setText("Cursor: (out of bounds)");
            }
        });
    }
}