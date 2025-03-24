package com.esgdev.sparkpaint.io;

            import com.esgdev.sparkpaint.engine.DrawingCanvas;
            import com.esgdev.sparkpaint.engine.layer.Layer;
            import com.esgdev.sparkpaint.engine.selection.Selection;
            import org.junit.Before;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.mockito.ArgumentCaptor;
            import org.mockito.Mock;
            import org.mockito.MockedStatic;
            import org.mockito.junit.MockitoJUnitRunner;

            import java.awt.*;
            import java.awt.datatransfer.Clipboard;
            import java.awt.datatransfer.DataFlavor;
            import java.awt.image.BufferedImage;
            import java.util.ArrayList;
            import java.util.List;

            import static org.junit.Assert.*;
            import static org.mockito.Mockito.*;

            @RunWith(MockitoJUnitRunner.class)
            public class ClipboardManagerTest {

                @Mock
                private DrawingCanvas mockCanvas;

                @Mock
                private Selection mockSelection;

                @Mock
                private ClipboardChangeListener mockListener;

                private ClipboardManager clipboardManager;
                private BufferedImage selectionImage;

                @Before
                public void setUp() {
                    // Setup standard selection rectangle and image
                    Rectangle selectionRect = new Rectangle(10, 10, 50, 50);
                    selectionImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);

                    // Configure the mock canvas to return our mocked selection
                    when(mockCanvas.getSelection()).thenReturn(mockSelection);

                    when(mockSelection.getBounds()).thenReturn(selectionRect);
                    when(mockSelection.getContent()).thenReturn(selectionImage);

                    clipboardManager = new ClipboardManager(mockCanvas);
                    clipboardManager.addClipboardChangeListener(mockListener);
                }

                @Test
                public void testCutSelection() {
                    // Setup
                    when(mockSelection.hasOutline()).thenReturn(true);

                    // Mocking static methods requires try-with-resources
                    try (MockedStatic<ImageSelection> imageSelectionMock = mockStatic(ImageSelection.class)) {
                        // Execute
                        clipboardManager.cutSelection();

                        // Verify
                        verify(mockCanvas).deleteSelection();
                        verify(mockSelection).clearOutline();
                        imageSelectionMock.verify(() -> ImageSelection.copyImage(selectionImage));
                    }
                }

                @Test
                public void testCutSelectionWithNoSelection() {
                    // Setup
                    when(mockCanvas.getSelection()).thenReturn(null);

                    // Execute
                    clipboardManager.cutSelection();

                    // Verify - nothing should happen
                    verify(mockCanvas, never()).deleteSelection();
                }

                @Test
                public void testCutSelectionWithInvalidBounds() {
                    // Setup
                    when(mockSelection.getBounds()).thenReturn(new Rectangle(0, 0, 0, 0));

                    // Execute
                    clipboardManager.cutSelection();

                    // Verify - nothing should happen
                    verify(mockCanvas, never()).deleteSelection();
                }

                @Test
                public void testCopySelection() {
                    // Setup
                    when(mockSelection.hasOutline()).thenReturn(true);

                    // Mocking static methods requires try-with-resources
                    try (MockedStatic<ImageSelection> imageSelectionMock = mockStatic(ImageSelection.class)) {
                        // Execute
                        clipboardManager.copySelection();

                        // Verify
                        imageSelectionMock.verify(() -> ImageSelection.copyImage(selectionImage));
                        verify(mockListener).clipboardStateChanged(anyBoolean(), anyBoolean());
                    }
                }

                @Test
                public void testCopySelectionWithNoSelection() {
                    // Setup
                    when(mockCanvas.getSelection()).thenReturn(null);

                    // Execute
                    clipboardManager.copySelection();

                    // Verify - nothing should happen
                    verifyNoInteractions(mockListener);
                }

                @Test
                public void testPasteSelection() throws Exception {
                    // Setup
                    BufferedImage pastedImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
                    List<Layer> layers = new ArrayList<>();
                    layers.add(mock(Layer.class));

                    when(mockCanvas.getLayers()).thenReturn(layers);

                    try (MockedStatic<ImageSelection> imageSelectionMock = mockStatic(ImageSelection.class)) {
                        // Setup mock for static method
                        imageSelectionMock.when(ImageSelection::pasteImage).thenReturn(pastedImage);

                        // Execute
                        clipboardManager.pasteSelection();

                        // Verify
                        ArgumentCaptor<Selection> selectionCaptor = ArgumentCaptor.forClass(Selection.class);
                        verify(mockCanvas).setSelection(selectionCaptor.capture());
                        Selection capturedSelection = selectionCaptor.getValue();

                        assertEquals(pastedImage, capturedSelection.getContent());
                        assertNotNull(capturedSelection.getBounds());
                    }
                }

                @Test
                public void testPasteSelectionWithNoLayers() throws Exception {
                    // Setup
                    BufferedImage pastedImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
                    List<Layer> layers = new ArrayList<>();

                    when(mockCanvas.getLayers()).thenReturn(layers);

                    try (MockedStatic<ImageSelection> imageSelectionMock = mockStatic(ImageSelection.class)) {
                        // Setup mock for static method
                        imageSelectionMock.when(ImageSelection::pasteImage).thenReturn(pastedImage);

                        // Execute
                        clipboardManager.pasteSelection();
                    }
                }

                @Test
                public void testPasteSelectionWithNullImage() throws Exception {
                    try (MockedStatic<ImageSelection> imageSelectionMock = mockStatic(ImageSelection.class)) {
                        // Setup
                        imageSelectionMock.when(ImageSelection::pasteImage).thenReturn(null);

                        // Execute
                        clipboardManager.pasteSelection();

                        // Verify
                        verify(mockCanvas, never()).setSelection(any(Selection.class));
                    }
                }

                @Test
                public void testHasSelection() {
                    // Test with selection
                    when(mockSelection.hasOutline()).thenReturn(true);
                    assertTrue(clipboardManager.hasSelection());

                    // Test without outline
                    when(mockSelection.hasOutline()).thenReturn(false);
                    assertFalse(clipboardManager.hasSelection());

                    // Test with null selection
                    when(mockCanvas.getSelection()).thenReturn(null);
                    assertFalse(clipboardManager.hasSelection());
                }

                @Test
                public void testCanPaste() {
                    // Setup for Toolkit
                    Toolkit mockToolkit = mock(Toolkit.class);
                    Clipboard mockClipboard = mock(Clipboard.class);
                    DataFlavor[] flavors = new DataFlavor[] {DataFlavor.imageFlavor};

                    try (MockedStatic<Toolkit> toolkitMock = mockStatic(Toolkit.class)) {
                        toolkitMock.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkit);
                        when(mockToolkit.getSystemClipboard()).thenReturn(mockClipboard);
                        when(mockClipboard.getAvailableDataFlavors()).thenReturn(flavors);

                        assertTrue(clipboardManager.canPaste());
                    }
                }

                @Test
                public void testCanPasteWithNoImageFlavor() {
                    // Setup for Toolkit
                    Toolkit mockToolkit = mock(Toolkit.class);
                    Clipboard mockClipboard = mock(Clipboard.class);
                    DataFlavor[] flavors = new DataFlavor[] {DataFlavor.stringFlavor}; // No image flavor

                    try (MockedStatic<Toolkit> toolkitMock = mockStatic(Toolkit.class)) {
                        toolkitMock.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkit);
                        when(mockToolkit.getSystemClipboard()).thenReturn(mockClipboard);
                        when(mockClipboard.getAvailableDataFlavors()).thenReturn(flavors);

                        assertFalse(clipboardManager.canPaste());
                    }
                }

                @Test
                public void testCanPasteWithException() {
                    // Setup for Toolkit
                    Toolkit mockToolkit = mock(Toolkit.class);

                    try (MockedStatic<Toolkit> toolkitMock = mockStatic(Toolkit.class)) {
                        toolkitMock.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkit);
                        when(mockToolkit.getSystemClipboard()).thenThrow(new HeadlessException());

                        assertFalse(clipboardManager.canPaste());
                    }
                }

                @Test
                public void testNotifyClipboardStateChanged() {
                    // Setup for canPaste
                    try (MockedStatic<Toolkit> toolkitMock = mockStatic(Toolkit.class)) {
                        Toolkit mockToolkit = mock(Toolkit.class);
                        Clipboard mockClipboard = mock(Clipboard.class);
                        DataFlavor[] flavors = new DataFlavor[] {DataFlavor.imageFlavor};

                        toolkitMock.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkit);
                        when(mockToolkit.getSystemClipboard()).thenReturn(mockClipboard);
                        when(mockClipboard.getAvailableDataFlavors()).thenReturn(flavors);

                        // Setup for hasSelection
                        when(mockSelection.hasOutline()).thenReturn(true);

                        // Execute
                        clipboardManager.notifyClipboardStateChanged();

                        // Verify
                        verify(mockListener).clipboardStateChanged(true, true);
                    }
                }

                @Test
                public void testAddAndRemoveClipboardChangeListener() {
                    // Setup
                    ClipboardChangeListener listener = mock(ClipboardChangeListener.class);

                    // First verify the add works by directly calling with known parameters
                    clipboardManager.addClipboardChangeListener(listener);
                    clipboardManager.clipboardStateChanged(true, true);
                    verify(listener).clipboardStateChanged(true, true);

                    // Reset mock and remove listener
                    reset(listener);
                    clipboardManager.removeClipboardChangeListener(listener);

                    // Verify removed listener is not called
                    clipboardManager.clipboardStateChanged(true, true);
                    verifyNoInteractions(listener);
                }

                @Test
                public void testEraseSelection() {
                    // Execute
                    clipboardManager.eraseSelection();

                    // Verify
                    verify(mockCanvas).deleteSelection();
                }
            }