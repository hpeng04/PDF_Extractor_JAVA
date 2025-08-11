# PDF Extractor Java - Developer Documentation

## Overview

PDF Extractor Java is a comprehensive application designed to extract data from building energy analysis PDF reports and export the processed information to structured Excel files. The application uses Optical Character Recognition (OCR) technology to read PDF content and organizes the extracted data into a hierarchical tree structure for easy analysis and reporting.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Key Components](#key-components)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Data Flow](#data-flow)
- [Core Classes and Interfaces](#core-classes-and-interfaces)
- [Configuration](#configuration)
- [Building and Running](#building-and-running)
- [Testing](#testing)
- [Development Guidelines](#development-guidelines)
- [Troubleshooting](#troubleshooting)

## Architecture Overview

The application follows a modular architecture with clear separation of concerns:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │    Business     │    │      Data       │
│     Layer       │───▶│     Logic       │───▶│     Layer       │
│                 │    │     Layer       │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • PdfSelectorGUI│    │ • DataExtractor │    │ • PdfData       │
│ • ProgressDialog│    │ • TreeBuilder   │    │ • Fields        │
│ • Main          │    │ • ExcelWriter   │    │ • TreeNode      │
└─────────────────┘    │ • PdfReader     │    └─────────────────┘
                       │ • ImageProcessor│
                       └─────────────────┘
```

## Key Components

### 1. User Interface Layer (`app` package)
- **Main.java**: Application entry point that launches the Swing GUI
- **PdfSelectorGUI.java**: Main window providing PDF selection, file type specification, and Excel export functionality
- **ProgressDialog.java**: Modal dialog showing processing progress during PDF extraction and Excel generation

### 2. Service Layer (`service` package)
- **DataExtractor.java**: Core service that orchestrates PDF text extraction using OCR and processes content into structured data
- **TreeBuilder.java**: Builds hierarchical data structures from extracted PDF data for Excel organization
- **ExcelWriter.java**: Handles Excel file creation and data writing using Apache POI

### 3. Data Access Layer (`readers` package)
- **Reader.java**: Base class providing Tesseract OCR configuration and functionality
- **PdfReader.java**: Specialized reader for processing PDF documents and extracting text with confidence scores

### 4. Image Processing (`imgprocessor` package)
- **PdfImageProcessor.java**: Converts PDF pages to images for OCR processing
- **PDFResolutionEnhancer.java**: Enhances image quality to improve OCR accuracy

### 5. Data Models (`model` package)
- **PdfData.java**: Primary data model containing all extracted information from a PDF
- **Fields.java**: Enum defining all extractable fields with metadata for processing and display

### 6. Utilities (`utils` package)
- **TreeNode.java**: Generic tree data structure for organizing hierarchical data
- **ConversionType.java**: Handles unit conversions between Imperial and SI units
- **AtomicDoubleBackedByAtomicLong.java**: Thread-safe double operations for progress tracking

## Project Structure

```
PDF_Extractor_JAVA/
├── PDFConverter/                           # Main Maven project
│   ├── pom.xml                            # Maven configuration and dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── app/                   # UI components
│   │   │   │   │   ├── Main.java          # Application entry point
│   │   │   │   │   ├── PdfSelectorGUI.java # Main GUI window
│   │   │   │   │   └── ProgressDialog.java # Progress tracking dialog
│   │   │   │   ├── imgprocessor/          # Image processing
│   │   │   │   │   ├── PdfImageProcessor.java
│   │   │   │   │   └── PDFResolutionEnhancer.java
│   │   │   │   ├── model/                 # Data models
│   │   │   │   │   ├── PdfData.java       # Main data container
│   │   │   │   │   └── Fields.java        # Field definitions
│   │   │   │   ├── readers/               # OCR and PDF reading
│   │   │   │   │   ├── Reader.java        # Base OCR functionality
│   │   │   │   │   └── PdfReader.java     # PDF-specific reading
│   │   │   │   ├── service/               # Core business logic
│   │   │   │   │   ├── DataExtractor.java # Main extraction service
│   │   │   │   │   ├── TreeBuilder.java   # Hierarchical data organization
│   │   │   │   │   ├── ExcelWriter.java   # Excel file generation
│   │   │   │   │   └── ITreeBuilder.java  # TreeBuilder interface
│   │   │   │   └── utils/                 # Utility classes
│   │   │   │       ├── TreeNode.java      # Tree data structure
│   │   │   │       ├── ConversionType.java # Unit conversions
│   │   │   │       └── AtomicDoubleBackedByAtomicLong.java
│   │   │   └── resources/
│   │   │       └── tessdata/              # Tesseract language data
│   │   │           ├── eng.traineddata    # English language model
│   │   │           └── equ.traineddata    # Equation recognition model
│   │   └── test/
│   │       └── java/                      # Unit tests
│   │           ├── excelTest.java
│   │           ├── mainTest.java
│   │           ├── readerTest.java
│   │           └── treeTest.java
│   └── target/                            # Maven build outputs
├── config.properties                      # Application configuration
└── README.md                             # Basic project information
```

## Dependencies

### Core Libraries

```xml
<!-- PDF Processing -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- OCR (Optical Character Recognition) -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.11.0</version>
</dependency>

<!-- Excel File Manipulation -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.2</version>
</dependency>

<!-- Image Processing -->
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.9.0-0</version>
</dependency>

<!-- Code Generation -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
</dependency>
```

### Java Version
- **Minimum**: Java 17
- **Target**: Java 17

## Data Flow

### 1. PDF Processing Pipeline

```
PDF File → Image Conversion → OCR Processing → Text Extraction → Data Parsing → Tree Building → Excel Export
```

#### Detailed Steps:

1. **PDF to Images** (`PdfImageProcessor.convert()`)
   - Converts each PDF page to high-resolution BufferedImage
   - Applies resolution enhancement for better OCR accuracy

2. **OCR Processing** (`PdfReader.setAllContent()`)
   - Uses Tesseract to extract text with confidence scores
   - Processes images in parallel using thread pools
   - Handles both English text and mathematical equations

3. **Data Extraction** (`DataExtractor.extractData()`)
   - Parses OCR results based on predefined field patterns
   - Handles unit conversions between Imperial and SI units
   - Manages multi-file PDFs and low-confidence content

4. **Tree Structure Building** (`TreeBuilder.buildTreeFromPDF()`)
   - Organizes extracted data into hierarchical tree structure
   - Groups related data fields under parent categories
   - Handles multiple data files and file type classification

5. **Excel Generation** (`ExcelWriter.writeToExcel()`)
   - Creates structured Excel worksheets
   - Applies proper formatting and cell organization
   - Supports both new file creation and existing file updates

### 2. Concurrency and Performance

The application uses several optimization strategies:

- **Parallel OCR Processing**: Multiple CPU cores process different PDF pages simultaneously
- **Memory Management**: Explicit cleanup of large objects and garbage collection hints
- **Background Processing**: Long-running operations use SwingWorker to prevent UI freezing
- **Progress Tracking**: Real-time progress updates during processing

## Core Classes and Interfaces

### DataExtractor.java

The central service class responsible for PDF data extraction.

**Key Methods:**
- `extractData(File file, ProgressDialog dialog, AtomicInteger progress, int numFiles)`: Main extraction orchestrator
- `processData1(DataExtractor extractor)`: Processes primary file data
- `processData2(DataExtractor extractor)`: Processes secondary file data (for multi-file PDFs)
- `processData(List<String> content, List<Float> content_confidence)`: Core data parsing logic

**Key Features:**
- Supports both SI and Imperial unit systems with automatic detection
- Handles multi-file PDF documents
- Tracks low-confidence OCR results for quality assurance
- Uses reflection for dynamic field mapping

### TreeBuilder.java

Organizes extracted data into hierarchical structures for Excel export.

**Key Methods:**
- `buildTreeFromPDF(List<PdfData> pdfDataList, List<String> fileType)`: Builds tree from PDF data
- `buildTreeFromExcel(Sheet sheet)`: Reconstructs tree from existing Excel data
- `writeTree(String parentTitle, String title, Object value, int fileIndex)`: Adds data to tree structure

**Key Features:**
- Automatic file type detection ("Proposed", "Reference", "Automatic", "N/A")
- Project ID generation based on building characteristics
- Content sorting for consistent Excel organization
- Memory-efficient processing with explicit cleanup

### PdfSelectorGUI.java

Main user interface providing comprehensive file management and export functionality.

**Key Features:**
- Multi-file PDF selection with file type specification
- Custom table cell renderers for interactive file type selection
- Background processing with progress indication
- Persistent directory preferences
- Low-confidence content reporting

### Fields.java

Comprehensive enum defining all extractable data fields.

**Structure:**
```java
FIELD_NAME("Parent Category", "Display Title", "Search Keyword", "fieldName", requiresConversion)
```

**Categories:**
- General Information (file, company, address)
- House Characteristics (orientation, occupants, house type)
- Temperature Settings (setpoints, setback duration)
- Window Characteristics (orientation-specific data)
- Building Parameters (thermal properties, R-values)
- HVAC System Performance (efficiency, energy consumption)

## Configuration

### Application Configuration

The application maintains configuration in several locations:

1. **Maven Configuration** (`pom.xml`)
   - Dependencies and versions
   - Build plugins and settings
   - Java version requirements

2. **User Preferences** (Automatically managed)
   - `~/.lastPdfDir.pref`: Last PDF selection directory
   - `~/.lastExcelDir.pref`: Last Excel export directory

3. **OCR Configuration** (`Reader.java`)
   - Tesseract language data path
   - OCR engine mode and page segmentation
   - Recognition confidence thresholds

### Tesseract Setup

The application requires Tesseract language data files:
- `eng.traineddata`: English language recognition
- `equ.traineddata`: Mathematical equation recognition

These files are included in `src/main/resources/tessdata/`.

## Building and Running

### Prerequisites

1. **Java Development Kit**: Java 17 or higher
2. **Maven**: Version 3.6 or higher
3. **Tesseract**: The application includes necessary language data

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Create executable JAR with dependencies
mvn assembly:single
```

### Running the Application

```bash
# Run from Maven
mvn exec:java -Dexec.mainClass="app.Main"

# Run compiled JAR
java -jar target/PDFConverter-1.0.5-SNAPSHOT-jar-with-dependencies.jar

# Run with specific memory settings
java -Xmx4g -jar target/PDFConverter-1.0.5-SNAPSHOT-jar-with-dependencies.jar
```

### IDE Setup

For development in IntelliJ IDEA or Eclipse:

1. Import as Maven project
2. Enable annotation processing for Lombok
3. Set Project SDK to Java 17
4. Configure code style to use 4-space indentation

## Testing

### Test Structure

```
src/test/java/
├── excelTest.java          # Excel generation and manipulation tests
├── mainTest.java           # Integration tests for main workflow
├── readerTest.java         # OCR and PDF reading tests
└── treeTest.java           # Tree structure and data organization tests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=readerTest

# Run tests with coverage report
mvn test jacoco:report
```

### Test Data

Test PDFs should be placed in the project root or test resources directory. The tests are currently disabled (`@Disabled` annotation) and require configuration for specific test environments.

## Development Guidelines

### Code Style

1. **Naming Conventions**
   - Classes: PascalCase
   - Methods/Variables: camelCase
   - Constants: UPPER_SNAKE_CASE
   - Packages: lowercase

2. **Documentation**
   - All public methods should have comprehensive JavaDoc
   - Complex algorithms should include inline comments
   - Use meaningful variable and method names

3. **Error Handling**
   - Use specific exception types
   - Provide user-friendly error messages
   - Log errors appropriately for debugging

### Memory Management

Given the image-intensive nature of PDF processing:

1. **Explicit Cleanup**
   - Null large objects after use
   - Call `System.gc()` after processing large datasets
   - Close streams and resources in finally blocks

2. **Concurrent Processing**
   - Use thread pools for parallel OCR processing
   - Implement proper thread interruption handling
   - Use atomic operations for progress tracking

### Adding New Fields

To add a new extractable field:

1. **Add to Fields.java**
   ```java
   NEW_FIELD("Parent Category", "Display Title", "PDF Keyword", "fieldName", needsConversion)
   ```

2. **Add to PdfData.java**
   ```java
   private String newField;
   ```

3. **Implement in DataExtractor.java**
   ```java
   case NEW_FIELD -> {
       // Extraction logic
   }
   ```

4. **Handle in TreeBuilder.java**
   ```java
   case NEW_FIELD -> {
       // Tree organization logic
   }
   ```

### Unit System Handling

The application automatically detects unit systems from PDF content:
- **SI Units**: Metric system (°C, MJ, m², etc.)
- **Imperial Units**: US customary (°F, BTU, ft², etc.)

Conversion factors are defined in `ConversionType.java` enum.

## Troubleshooting

### Common Issues

1. **OCR Accuracy Problems**
   - **Cause**: Poor image quality or unsupported fonts
   - **Solution**: Adjust PDF resolution enhancement settings in `PDFResolutionEnhancer.java`
   - **Debug**: Check confidence scores in low-confidence content file

2. **Memory Issues**
   - **Cause**: Large PDFs or insufficient heap space
   - **Solution**: Increase JVM heap size with `-Xmx` parameter
   - **Prevention**: Process PDFs in smaller batches

3. **Excel Export Failures**
   - **Cause**: File is open in Excel or permissions issues
   - **Solution**: Close Excel file before export, check file permissions
   - **Debug**: Check exception messages for specific Apache POI errors

4. **Threading Issues**
   - **Cause**: Concurrent access to shared resources
   - **Solution**: Use thread-safe collections (ConcurrentHashMap, CopyOnWriteArrayList)
   - **Debug**: Enable thread safety assertions

### Debugging Tips

1. **Enable Detailed Logging**
   ```java
   // Add to main method or static initializer
   System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
   System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
   ```

2. **OCR Debugging**
   - Save intermediate images for manual inspection
   - Log confidence scores for extracted text
   - Compare results with manual PDF text extraction

3. **Performance Profiling**
   - Use JVisualVM or similar tools to monitor memory usage
   - Profile OCR processing times for optimization
   - Monitor thread pool utilization

### Log Files and Output

The application generates several types of diagnostic output:

1. **Console Output**: Progress messages and error information
2. **LowConfContent.txt**: OCR results with low confidence scores
3. **Excel Files**: Final structured output with extracted data

### Getting Help

For development assistance:

1. Check existing unit tests for usage examples
2. Review JavaDoc documentation in source code
3. Examine the existing PDF processing pipeline
4. Test with sample PDF files to understand expected input formats

## Future Enhancements

Potential areas for improvement:

1. **Performance Optimization**
   - Add support for incremental processing
   - Optimize memory usage for large batches
   - Optimize OCR for precision and speed

2. **Enhanced OCR**
   - Improved confidence score handling

3. **User Interface**
   - Dark mode support
   - Drag-and-drop file selection
   - Real-time preview of extracted data

