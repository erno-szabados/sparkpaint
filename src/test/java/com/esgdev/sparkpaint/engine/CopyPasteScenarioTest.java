package com.esgdev.sparkpaint.engine;

import com.esgdev.sparkpaint.engine.tools.ToolManager;
import com.esgdev.sparkpaint.io.ImageSelection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Copy, cut and paste scenarios on the real canvas. The system clipboard is
 * replaced by a static mock of {@link ImageSelection}, so the paste source is
 * a known 10x10 red block.
 */
public class CopyPasteScenarioTest {

    private static final int LEFT = MouseEvent.BUTTON1_DOWN_MASK;
    private static final int BLOCK = 10;

    private DrawingCanvas canvas;
    private MockedStatic<ImageSelection> clipboard;
    private final JPanel src = new JPanel();

    @Before
    public void setUp() throws Exception {
        canvas = DrawingCanvas.create();
        canvas.createNewCanvas(40, 40, Color.WHITE);
        canvas.setCurrentTool(ToolManager.Tool.PENCIL);
        canvas.setDrawingColor(Color.RED);

        BufferedImage red = new BufferedImage(BLOCK, BLOCK, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < BLOCK; x++) {
            for (int y = 0; y < BLOCK; y++) {
                red.setRGB(x, y, Color.RED.getRGB());
            }
        }
        clipboard = Mockito.mockStatic(ImageSelection.class);
        clipboard.when(ImageSelection::pasteImage).thenReturn(red);
    }

    @After
    public void tearDown() {
        clipboard.close();
    }

    private MouseEvent ev(int id, int x, int y) {
        return new MouseEvent(src, id, 0L, LEFT, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private void click(int x, int y) {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, x, y));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, x, y));
    }

    private void drag(int fromX, int fromY, int toX, int toY) {
        canvas.getActiveTool().mousePressed(ev(MouseEvent.MOUSE_PRESSED, fromX, fromY));
        canvas.getActiveTool().mouseDragged(ev(MouseEvent.MOUSE_DRAGGED, toX, toY));
        canvas.getActiveTool().mouseReleased(ev(MouseEvent.MOUSE_RELEASED, toX, toY));
    }

    private int layerPixel(int layer, int x, int y) {
        return canvas.getLayers().get(layer).getImage().getRGB(x, y);
    }

    private int pixel(int x, int y) {
        return layerPixel(canvas.getCurrentLayerIndex(), x, y);
    }

    private void paste() throws Exception {
        canvas.pasteSelection();
    }

    /** Commits the floating paste by clicking outside it. */
    private void commitByClickingOutside() {
        click(35, 35);
    }

    @Test
    public void pasteStaysFloatingUntilCommitted() throws Exception {
        paste();

        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
        assertTrue(canvas.hasSelection());
    }

    @Test
    public void committedPasteLandsOnLayer() throws Exception {
        paste();
        commitByClickingOutside();

        assertEquals(Color.RED.getRGB(), pixel(5, 5));
    }

    @Test
    public void undoAfterCommitRemovesPaste() throws Exception {
        paste();
        commitByClickingOutside();

        canvas.undo();

        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
    }

    @Test
    public void undoWhileFloatingRemovesSelectionOutline() throws Exception {
        paste();

        canvas.undo();

        assertFalse(canvas.hasSelection());
        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
    }

    @Test
    public void dragFloatingPasteThenCommitMovesIt() throws Exception {
        paste();
        drag(5, 5, 25, 25);
        commitByClickingOutside();

        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
        assertEquals(Color.RED.getRGB(), pixel(25, 25));
    }

    /** Paste and commit are one user action, so one undo should remove the whole paste. */
    @Test
    public void onePasteCommitIsOneUndoStep() throws Exception {
        paste();
        commitByClickingOutside();

        canvas.undo();

        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
        assertFalse(canvas.canUndo());
    }

    /** Redo after undoing a paste should bring the paste back. */

    @Test
    public void redoRestoresUndonePaste() throws Exception {
        paste();
        canvas.undo();
        canvas.redo();
        commitByClickingOutside();

        assertEquals(Color.RED.getRGB(), pixel(5, 5));
    }

    /** Accepted behaviour: a floating paste commits to the layer current at commit time. */
    @Test
    public void floatingPasteCommitsToLayerCurrentAtCommit() throws Exception {
        paste();
        canvas.addNewLayer();
        commitByClickingOutside();

        assertEquals(Color.WHITE.getRGB(), layerPixel(0, 5, 5));
        assertEquals(Color.RED.getRGB(), layerPixel(1, 5, 5));
    }

    @Test
    public void cutSelectionRemovesCommittedPixelsAndUndoRestoresThem() throws Exception {
        paste();
        commitByClickingOutside();
        canvas.selectAll();

        canvas.cutSelection();
        assertEquals(0, pixel(5, 5));

        canvas.undo();
        assertEquals(Color.RED.getRGB(), pixel(5, 5));
    }

    /** Cutting a floating paste should discard the paste and leave the layer alone. */
    @Test
    public void cutOfFloatingPasteDiscardsItAndKeepsLayer() throws Exception {
        paste();
        canvas.cutSelection();
        commitByClickingOutside();

        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
    }

    /** Discarding a floating paste should not leave a no-op undo step behind. */
    @Test
    public void cutOfFloatingPasteLeavesNoUndoStep() throws Exception {
        paste();
        canvas.cutSelection();

        assertFalse(canvas.canUndo());
    }

    @Test
    public void deleteOfFloatingPasteLeavesNoUndoStep() throws Exception {
        paste();
        canvas.deleteCurrentSelection();

        assertFalse(canvas.canUndo());
        assertEquals(Color.WHITE.getRGB(), pixel(5, 5));
    }

    /** A layer op after the paste is a real edit, so the paste's undo step must stay. */
    @Test
    public void cutOfFloatingPasteKeepsUndoStepWhenLayerChangedAfterPaste() throws Exception {
        paste();
        canvas.addNewLayer();
        canvas.cutSelection();

        assertTrue(canvas.canUndo());
    }

    @Test
    public void copyDoesNotChangeLayersOrUndo() throws Exception {
        paste();
        commitByClickingOutside();
        boolean undoBefore = canvas.canUndo();
        canvas.selectAll();

        canvas.copySelection();

        assertEquals(Color.RED.getRGB(), pixel(5, 5));
        assertEquals(undoBefore, canvas.canUndo());
    }
}
