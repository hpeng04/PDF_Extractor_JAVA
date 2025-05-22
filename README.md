\
<!-- filepath: /Users/hhpeng/Documents/Work/PDF_Extractor_JAVA/README.md -->
# PDF_Extractor_JAVA

## Project Overview
PDF_Extractor_JAVA is a Java application designed to extract data from PDF files and export it into an Excel format. It provides a graphical user interface (GUI) for users to select PDF files, specify the type of content within the PDFs (e.g., "Proposed", "Reference"), and then process these files to extract relevant information. The extracted data is then organized and written to an `.xlsx` file. The application utilizes OCR capabilities for image-based PDFs and can handle PDFs with embedded text.

## File Structure

The project is organized into several key directories and files:

```
PDF_Extractor_JAVA/
├── PDFConverter/                     # Main Maven module for the PDF conversion logic
│   ├── pom.xml                       # Maven project configuration, dependencies, and build settings
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── app/              # Contains UI and application entry point
│   │   │   │   │   ├── Main.java     # Main entry point of the application
│   │   │   │   │   └── PdfSelectorGUI.java # Implements the main GUI for PDF selection and interaction
│   │   │   │   │   └── ProgressDialog.java # UI for showing progress of PDF processing
│   │   │   │   ├── imgprocessor/     # Image processing utilities for PDFs
│   │   │   │   │   ├── PdfImageProcessor.java    # Handles conversion of PDF pages to images
│   │   │   │   │   └── PDFResolutionEnhancer.java # (Likely for enhancing image resolution for OCR)
│   │   │   │   ├── model/            # Data model classes
│   │   │   │   │   ├── Fields.java   # (Likely represents specific fields to be extracted)
│   │   │   │   │   └── PdfData.java  # (Likely a data structure to hold extracted PDF content)
│   │   │   │   ├── readers/          # Classes responsible for reading and parsing PDF content
│   │   │   │   │   ├── PdfReader.java # Core PDF reading and text/image extraction logic
│   │   │   │   │   └── Reader.java    # (Likely an interface or abstract class for readers)
│   │   │   │   ├── service/          # Business logic and services
│   │   │   │   │   ├── DataExtractor.java # Extracts specific data from the raw PDF content
│   │   │   │   │   ├── ExcelWriter.java   # Writes extracted data to Excel files
│   │   │   │   │   ├── ITreeBuilder.java  # (Interface for tree building logic, if used)
│   │   │   │   │   └── TreeBuilder.java   # (Implementation for tree building, if used)
│   │   │   │   ├── utils/            # Utility classes
│   │   │   │   │   ├── AtomicDoubleBackedByAtomicLong.java # Custom utility class
│   │   │   │   │   ├── ConversionType.java # Enum or utility for PDF content types
│   │   │   │   │   └── TreeNode.java       # (Node for a tree structure, if used)
│   │   │   ├── resources/
│   │   │   │   └── tessdata/         # Tesseract OCR training data (e.g., eng.traineddata)
│   │   └── test/                     # Unit tests
│   │       └── java/
│   │           ├── excelTest.java
│   │           ├── mainTest.java
│   │           ├── readerTest.java
│   │           └── treeTest.java
│   └── target/                       # Compiled output and packaged JARs
│       ├── PDFConverter-1.0.5-SNAPSHOT-jar-with-dependencies.jar # Executable JAR
│       └── ... (other build artifacts)
├── config.properties                 # Application configuration file (e.g., last used directory)
└── README.md                         # This file

## Core Components and Workflow

### 1. Application Entry (`app.Main`)
- The `main` method in `Main.java` initializes and displays the `PdfSelectorGUI`.
- It uses `SwingUtilities.invokeLater` to ensure GUI operations are on the Event Dispatch Thread (EDT).

### 2. User Interface (`app.PdfSelectorGUI`)
- **Window Setup**: Extends `JFrame` to create the main application window.
- **PDF Selection**:
    - "Select PDF" button opens a `JFileChooser` to allow users to select one or more PDF files.
    - Selected PDFs are added to a `JTable`.
    - The last used directory for PDF selection is stored in `config.properties` and reloaded on next use.
- **File Type Specification**:
    - The `JTable` has two columns: "PDF Name" and "File Type".
    - The "File Type" column contains a button that, when clicked, opens a dialog (`JOptionPane.showOptionDialog`) allowing the user to classify the PDF as "Proposed", "Reference", or "N/A".
    - This selection is stored in the `typeSelection` map.
- **PDF Deletion**:
    - "Delete PDF" button allows users to remove selected PDFs from the table.
- **Export to Excel**:
    - "Export to Excel" button triggers the data extraction and Excel generation process.
    - It prompts the user to choose a save location for the `.xlsx` file.
    - The last used directory for Excel export is also managed via `config.properties`.

### 3. Data Extraction Process (`service.DataExtractor`, `readers.PdfReader`, `imgprocessor.PdfImageProcessor`)
- When "Export to Excel" is clicked:
    - A `ProgressDialog` is shown to the user.
    - For each selected PDF:
        - `PdfImageProcessor.convert()`: Converts PDF pages into `BufferedImage` objects. This is crucial for scanned or image-based PDFs.
        - `PdfReader.setAllContent()`: Uses Tesseract OCR (via `tess4j`) to extract text and word confidence levels from the buffered images.
            - It processes images page by page.
            - Stores extracted words and their confidence scores.
        - `DataExtractor.extractData()`:
            - Orchestrates the reading and initial processing.
            - It attempts to identify if a single PDF file might represent multiple concatenated documents by looking for page number patterns (e.g., "Page XX of YY").
            - Separates content if multiple internal documents are detected.
            - Collects text content and confidence scores. Low confidence words might be flagged or handled specifically.
            - (Further specific data extraction logic based on patterns, keywords, or structure would reside here or be called from here).
- The extracted data, likely structured into `PdfData` and `Fields` objects, is collected for all processed PDFs.

### 4. Excel Generation (`service.ExcelWriter`)
- `ExcelWriter.writeToExcel()`:
    - Takes the list of `PdfData` objects (and potentially the `typeSelection` map).
    - Creates an Apache POI `XSSFWorkbook`.
    - Organizes the extracted data into sheets and cells.
    - Writes the workbook to the user-specified `.xlsx` file.

### 5. Configuration (`config.properties`)
- Stores the `lastPdfDirectory` and `lastExcelDirectory` to improve user experience by remembering the last accessed folders.

## Program Workflow

```
[User Starts Application]
    |
    V
[Main.java: main()] --> [PdfSelectorGUI: Constructor()]
    |
    V
[PdfSelectorGUI: UI Displayed (JFrame visible)]
    |
    +----------------------------------------------------+
    | User Action                                        |
    +----------------------------------------------------+
        |                                                |
        V (Clicks "Select PDF")                          V (Clicks "File Type" button in table)
[PdfSelectorGUI: selectPdfAction()]                  [PdfSelectorGUI.FileTypeButtonEditor: getCellEditorValue()]
    |                                                |
    V                                                V
[JFileChooser: Shows Dialog]                         [JOptionPane: Shows "Select type" dialog]
    |                                                |
    V (User selects PDF(s))                          V (User selects type: "Proposed", "Reference", "N/A")
[PdfSelectorGUI: Adds PDF to JTable & typeSelection map (default type)] |
    |                                                [PdfSelectorGUI: Updates JTable & typeSelection map with chosen type]
    |<-----------------------------------------------(User can repeat PDF selection/type assignment)
    |
    V (Clicks "Export to Excel")
[PdfSelectorGUI: exportToExcelAction()]
    |
    V
[JFileChooser: Shows "Save Excel" Dialog]
    |
    V (User selects save location)
[ProgressDialog: Displayed]
    |
    V
[Loop for each PDF in JTable]:
    |
    |--> [DataExtractor: extractData()]
    |       |
    |       |--> [PdfImageProcessor: convert(pdfFile)] --> Returns Map<Integer, BufferedImage>
    |       |       | (Converts PDF pages to images)
    |       |
    |       |--> [PdfReader: setAllContent(images)]
    |       |       | (Performs OCR using Tesseract on each image)
    |       |       |--> Stores words and confidence in `pagesWithInfo`
    |       |
    |       |--> (DataExtractor further processes `pagesWithInfo` to build `content1`, `content2`, `lowConfContent`)
    |       |       | (Identifies multi-document PDFs, unit types if applicable)
    |       |
    |       |--> Returns structured data (e.g., List<PdfData>)
    |
    |<--(Collects all PdfData)
    |
    V
[ExcelWriter: writeToExcel(allPdfData, outputPath)]
    |
    V (Creates XSSFWorkbook, populates with data)
[Excel File Saved to Disk]
    |
    V
[ProgressDialog: Closed]
    |
    V
[User notified of completion/failure]
```

## Dependencies
The project uses Maven for dependency management. Key dependencies include:
- **Apache PDFBox (`org.apache.pdfbox:pdfbox`):** For low-level PDF manipulation, text extraction from text-based PDFs, and rendering pages to images.
- **Tess4J (`net.sourceforge.tess4j:tess4j`):** A Java JNA wrapper for the Tesseract OCR engine, used to extract text from images.
- **SLF4J & Log4j2 (`org.slf4j:slf4j-simple`, `org.apache.logging.log4j:*`):** For logging.
- **OpenCV (`org.openpnp:opencv`):** (Potentially used for advanced image processing before OCR, though its direct use isn't fully detailed in the provided snippets).
- **Apache POI (`org.apache.poi:poi-ooxml`):** For creating and writing `.xlsx` Excel files.
- **OpenJFX (`org.openjfx:javafx`):** (Listed in `pom.xml`, but the GUI is Swing-based. This might be a remnant or for a different module/feature not immediately apparent).
- **Lombok (`org.projectlombok:lombok`):** To reduce boilerplate code (e.g., `@Getter`, `@Setter`).
- **JUnit (`junit:junit`):** For unit testing.

## Configuration
The application uses a `config.properties` file located in the root directory (and copied to `target/classes` during build).
- `lastPdfDirectory`: Stores the path of the last directory from which PDFs were selected.
- `lastExcelDirectory`: Stores the path of the last directory where an Excel file was saved.
This file is automatically created or updated by the application.

## Build and Run

### Prerequisites
- Java Development Kit (JDK) version 17 or higher.
- Apache Maven.
- Tesseract OCR engine installed and configured on the system, with its `tessdata` path correctly set up for Tess4J to find (or ensure `tessdata` is in the classpath, as it appears in `src/main/resources`).

### Build
Navigate to the `PDFConverter` directory and run:
```bash
mvn clean package
```
This will compile the project and create a JAR file with dependencies in the `PDFConverter/target/` directory (e.g., `PDFConverter-1.0.5-SNAPSHOT-jar-with-dependencies.jar`).

### Run
Execute the JAR file:
```bash
java -jar PDFConverter/target/PDFConverter-1.0.5-SNAPSHOT-jar-with-dependencies.jar
```
This will launch the PDF Extractor GUI.

## Important Notes
- **Tesseract OCR**: The quality of data extraction heavily depends on the Tesseract OCR engine's performance, the quality of the PDF/images, and the correctness of the `tessdata` (language files).
- **Memory Usage**: Processing large PDFs or many PDFs with high-resolution images can be memory-intensive.

