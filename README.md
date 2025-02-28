## Pixel Editor Application

### 1. Requirements

#### 1.1. Target Platform:
- Windows (recent versions)
- Linux (recent versions)
- Desktop application (offline functionality)

#### 1.2. Programming Language and Libraries:
- Java
- Swing and Java2D with java2.Geom (widget library and image manipulation library)

#### 1.3. Image Handling:
- Use java2d image structures.
- Transparency handled via RGBA channels.

#### 1.4. User Interface/User Experience (UI/UX):
- **Toolbar:**
  - Fixed toolbar layout.
  - Brush size adjustable for the pencil tool.
  - Line thickness adjustable for line, rectangle, and circle tools.
  - Tool selection feedback: Cursor shape, highlighted/framed tool icon.
- **Color Palette:**
  - Fixed palette size.
  - Colors selectable via swing color picker.
  - No palette load/save in the initial version.
- **Selection:**
  - Transparent selection as a modality (checkbox).
  - Selected area marked with a dotted line border.
- **Undo/Redo:**
  - Fixed undo/redo depth of 16 steps.
- **File I/O:**
  - Support java2d-supported image format variants.
  - Default save format: PNG.
- **Error Handling:**
  - Status bar messages for normal operation results.
  - Modal dialogs for errors.
- **Validation:**
  - Basic input validation (industry best practices).

### 2. Design Specification

#### 2.1. Architecture:
- Layered architecture:
  - **UI Layer:** Handles user input and UI elements.
  - **Drawing Engine Layer:** Manages pixel data and drawing tools.
  - **File I/O Layer:** Handles image loading/saving.
  - **Data Layer:** Stores image data, color palette, and application state.

#### 2.2. Data Structures:
- **Image Data:** Utilize `BufferedImage`.
- **Color Palette:** List of RGB color values.
- **Selection:** Rectangle (x, y, width, height), transparent flag, temporary java2d Image for selection data.
- **History Stack:** List of java2d `BufferedImage` objects.

#### 2.3. User Interface Design:
- **Main Window:** Canvas, toolbar, color palette, status bar, menu bar.
- **Toolbar:** Buttons for pencil, line, rectangle, circle, fill, eraser, color picker, selection.
- **Color Palette:** Grid of color swatches, selected color indicators.
- **Status Bar:** Cursor coordinates, selected color information.

#### 2.4. Drawing Engine Design:
- **Drawing Tools:** Implement drawing algorithms using java2d and Geom.
- **Image Manipulation:**
  - **Selection:** Create selection rectangle.
  - **Cut/Copy/Paste:** Manipulate pixel data and temporary selection image.
  - **Flood Fill:** Implement a flood fill algorithm.
  - **Undo/Redo:** History stack for image state storage.
  - **Respect transparency values during copy, cut and paste operations.

#### 2.5. File I/O Design:
- **Image Loading:** Use java2d to load PNG, JPG, BMP.
- **Image Saving:** Use java2d to save PNG, JPG, BMP. Default to PNG.
- **New Image:** Creates a new `BufferedImage` object with specified dimensions.

#### 2.6. Error Handling:
- **File I/O Errors:** Display error messages via Dialog.
- **Invalid Input:** Basic input validation.
- **Status messages on status bar.

### 3. Implementation Plan

#### Phase 1: Project Setup and Core Functionality
1. Environment Setup: OpenJDK, Maven project.
2. Window and Canvas: java2d window, custom java2d `Canvas`.
3. Basic Drawing (Pencil): Mouse events, pixel updates, rendering.
4. Color Palette (Initial): swing `ColorPicker`, selected color storage.

#### Phase 2: Drawing Tools and UI Enhancements
5. Line, Rectangle, Circle/Ellipse: java2d and geom image functions, line thickness.
6. Fill and Eraser: Flood fill algorithm, pixel transparency.
7. Color Picker: Pixel color retrieval, palette update.
8. Toolbar: java2d `ToolBar`, tool selection logic, visual feedback.
9. Brush Size/Line Thickness: swing `Spinner` controls.

#### Phase 3: Image Manipulation and File I/O
10. Selection Tool: Selection rectangle, dotted line, transparency checkbox.
11. Cut, Copy, Paste: Temporary java2d Image, pixel manipulation.
12. File I/O (Load/Save): Java file operations, "New Image" functionality.
13. Status Bar: Cursor coordinates, status messages.

#### Phase 4: Undo/Redo and Error Handling
14. Undo/Redo (History Stack): Image state storage, stack operations (16 steps).
15. Error Handling: swing dialogs, status bar messages.
16. Input Validation: Basic input validation.

#### Phase 5: Testing and Refinement
17. Unit Testing: Core functionalities.
18. Integration Testing: Application as a whole.
19. User Testing: Usability feedback.
20. Code Refactoring: Code improvement.
21. Documentation: Code and user manual.
