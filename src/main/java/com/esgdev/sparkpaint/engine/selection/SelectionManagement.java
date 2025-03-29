package com.esgdev.sparkpaint.engine.selection;

import java.awt.*;

/**
 * Interface that exposes selection management functionality.
 * Provides methods to manage and interact with canvas selections.
 */
public interface SelectionManagement {

    /**
     * Gets the current selection.
     *
     * @return the current selection, or null if no selection exists
     */
    Selection getSelection();

    /**
     * Sets the current selection.
     *
     * @param selection the selection to set
     */
    void setSelection(Selection selection);

    /**
     * Clears the current selection.
     */
    void clearSelection();

    /**
     * Selects all visible content on the canvas.
     */
    void selectAll();

    /**
     * Deletes the content within the current selection.
     */
    void deleteSelectionAreaFromCurrentLayer();

    /**
     * Rotates the selection content by the specified degrees.
     *
     * @param degrees The angle in degrees to rotate the selection.
     */
    void rotateSelection(int degrees);

    /**
     * Flips the selection content either horizontally or vertically.
     *
     * @param horizontal true to flip horizontally, false to flip vertically
     */
    void flipSelection(boolean horizontal);

    /**
     * Checks if the given world point is within the current selection.
     *
     * @param worldPoint Point in world coordinates
     * @return true if the point is within a selection, false otherwise
     */
    boolean isWithinSelection(Point worldPoint);

    /**
     * Gets a graphics context appropriate for drawing - either for the current selection or current layer.
     *
     * @return Graphics2D context configured with proper transforms and clipping
     */
    public Graphics2D getDrawingGraphics();

    /**
     * Translates screen coordinates to selection-local coordinates if there's a selection,
     * or to canvas world coordinates otherwise.
     *
     * @param screenPoint Point in screen coordinates
     * @param zoomFactor Current canvas zoom factor
     * @return Point in the appropriate coordinate system
     */
    Point getDrawingCoordinates(Point screenPoint, float zoomFactor);
}