package app;

import model.PdfData; // Data model for holding extracted PDF information
import org.apache.poi.ss.usermodel.*; // Apache POI for Excel manipulation (Workbook, Sheet)
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Apache POI for .xlsx Excel format
import service.DataExtractor; // Service class for extracting data from PDFs
import service.ExcelWriter; // Service class for writing data to Excel files
import service.TreeBuilder; // Service class for building a tree structure from data

import javax.swing.*; // Swing components for GUI
import javax.swing.filechooser.FileNameExtensionFilter; // Filter for file chooser dialogs
import javax.swing.table.DefaultTableModel; // Default table model for JTable
import javax.swing.table.TableCellRenderer; // Interface for rendering cells in a JTable
import javax.swing.table.TableColumnModel; // Model for table columns
import java.awt.*; // Abstract Window Toolkit for GUI (BorderLayout, Font, Insets)
import java.awt.Font;
import java.awt.event.ActionEvent; // Event class for action events (e.g., button clicks)
import java.io.*; // Input/Output classes (File, FileInputStream)
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*; // Utility classes (Map, LinkedHashMap, List, ArrayList)
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList; // Thread-safe list for concurrent modifications
import java.util.concurrent.atomic.AtomicInteger; // Atomic integer for thread-safe counter

/**
 * PdfSelectorGUI is the main graphical user interface for the PDF Extractor application.
 * It allows users to select PDF files, specify their type, and export extracted data to an Excel file.
 * It extends {@link javax.swing.JFrame} to provide the main window.
 */
public class PdfSelectorGUI extends JFrame {
    private final int WIDTH = 700; // Width of the main application window
    private final int HEIGHT = 525; // Height of the main application window
    private final int PDF = 0; // Constant to identify PDF file operations
    private final int EXCEL = 1; // Constant to identify Excel file operations
    private DefaultTableModel tableModel; // Model for the JTable displaying PDF files
    private JTable table; // Table to display selected PDF files and their types
    private File lastPdfDirectory; // Stores the last directory from which a PDF was selected
    private File lastExcelDirectory; // Stores the last directory where an Excel file was saved/selected
    private Map<String, String> typeSelection = new LinkedHashMap<>(); // Map to store PDF file paths and their user-selected types

    /**
     * Constructor for PdfSelectorGUI.
     * Initializes the main frame, sets up UI components like buttons, table, and layout.
     */
    public PdfSelectorGUI() {
        super("PDF Extractor"); // Sets the title of the JFrame
        // Setting up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Ensures the application exits when the window is closed
        setSize(WIDTH, HEIGHT); // Sets the dimensions of the window
        setLayout(new BorderLayout(10, 10)); // Uses BorderLayout with spacing between components

        // Button to select PDFs
        JButton selectPdfButton = new JButton("Select PDF");
        selectPdfButton.addActionListener(this::selectPdfAction); // Adds an action listener to handle PDF selection
        selectPdfButton.setFont(new Font("Arial", Font.BOLD, 14)); // Sets font for the button

        // Button to delete selected PDFs from the table
        JButton deletePdfButton = new JButton("Delete PDF");
        deletePdfButton.addActionListener(this::deletePdfAction); // Adds an action listener to handle PDF deletion
        deletePdfButton.setFont(new Font("Arial", Font.BOLD, 14)); // Sets font for the button

        // Initialize table model and table
        // The table model defines the columns ("PDF Name", "File Type") and manages table data.
        tableModel = new DefaultTableModel(new Object[]{"PDF Name", "File Type"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Makes only the "File Type" column editable (to allow button interaction).
                return column == 1;
            }
        };
        table = new JTable(tableModel); // Creates the JTable with the defined model
        // Sets a custom renderer and editor for the "File Type" column to display and handle buttons.
        table.getColumn("File Type").setCellRenderer(new FileTypeButtonRenderer());
        table.getColumn("File Type").setCellEditor(new FileTypeButtonEditor(new JCheckBox()));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disables automatic column resizing to maintain set widths
        table.setRowHeight(20); // Sets the height for each row in the table

        TableColumnModel columnModel = table.getColumnModel(); // Gets the column model to adjust column widths
        columnModel.getColumn(0).setPreferredWidth((int) (WIDTH * 0.69)); // Sets preferred width for "PDF Name" column
        columnModel.getColumn(1).setPreferredWidth((int) (WIDTH * 0.3)); // Sets preferred width for "File Type" column

        JScrollPane scrollPane = new JScrollPane(table); // Adds the table to a scroll pane to handle many entries

        // Export to Excel button
        JButton exportToExcelButton = new JButton("Export to Excel");
        exportToExcelButton.addActionListener(this::exportToExcelAction); // Adds action listener for Excel export
        exportToExcelButton.setFont(new Font("Arial", Font.BOLD, 14)); // Sets font for the button
        exportToExcelButton.setMargin(new Insets(10, 0, 10, 0)); // Sets margin for the button

        // Panel for top buttons (Select PDF, Delete PDF)
        JPanel buttonPanel = new JPanel(); // Uses FlowLayout by default
        buttonPanel.add(selectPdfButton);
        buttonPanel.add(deletePdfButton);

        // Adding components to frame
        add(scrollPane, BorderLayout.CENTER); // Adds the table (in a scroll pane) to the center
        add(buttonPanel, BorderLayout.NORTH); // Adds the button panel to the top
        add(exportToExcelButton, BorderLayout.SOUTH); // Adds the export button to the bottom
    }

    /**
     * Custom TableCellRenderer to display a JButton in a table cell.
     * This class extends {@link javax.swing.JButton} and implements {@link javax.swing.table.TableCellRenderer}.
     */
    class FileTypeButtonRenderer extends JButton implements TableCellRenderer {
        /**
         * Configures the button to be displayed in the table cell.
         * @param table The JTable that is asking the renderer to draw.
         * @param value The value of the cell to be rendered.
         * @param isSelected True if the cell is to be rendered with selection highlighting.
         * @param hasFocus True if the cell has focus.
         * @param row The row index of the cell being drawn.
         * @param column The column index of the cell being drawn.
         * @return The component (this button) to be used for drawing the cell.
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString()); // Sets button text from cell value
            return this;
        }
    }

    /**
     * Custom TableCellEditor to handle button clicks within a table cell and provide a selection dialog.
     * This class extends {@link javax.swing.DefaultCellEditor}.
     */
    class FileTypeButtonEditor extends DefaultCellEditor {
        protected JButton button; // The button used as the editor component
        private boolean isPushed; // Flag to track if the button was clicked
        private String pdfName; // Name of the PDF file for which the type is being selected
        private String label; // The selected file type label
        private int editingRow; // The row index currently being edited

        /**
         * Constructor for FileTypeButtonEditor.
         * @param checkBox A JCheckBox (required by DefaultCellEditor constructor, but not directly used for button logic).
         */
        public FileTypeButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            // When the button is clicked, stop editing to trigger getCellEditorValue
            button.addActionListener(e -> fireEditingStopped());
        }

        /**
         * Sets up the editor button when a cell editing starts.
         * @param table The JTable that is asking the editor to edit.
         * @param value The value of the cell to be edited.
         * @param isSelected True if the cell is to be rendered with selection highlighting.
         * @param row The row index of the cell being edited.
         * @param column The column index of the cell being edited.
         * @return The component (this button) to be used for editing the cell.
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row; // Store the current editing row
            pdfName = table.getModel().getValueAt(row, 0).toString(); // Get PDF name from the first column
            // Set button text to current value or default if null
            String currentValue = (value != null) ? value.toString() : "Select File Type";
            button.setText(currentValue);
            isPushed = true; // Mark button as pushed to trigger dialog in getCellEditorValue
            return button;
        }

        /**
         * Returns the value selected by the user after the button is clicked.
         * This method is called when editing is stopped. It shows an option dialog for type selection.
         * @return The selected file type as a String.
         */
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Options for the file type selection dialog
                String[] options = {"Proposed", "Reference", "N/A"};
                // Show an option dialog to the user. JOptionPane is from javax.swing.JOptionPane.
                int selection = JOptionPane.showOptionDialog(button, "Select type for " + pdfName,
                        "Selection", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (selection >= 0) { // If the user made a selection (not closed/cancelled)
                    label = options[selection];
                    typeSelection.put(pdfName, label); // Store the selection in the map (pdfName -> type)
                } else {
                    label = button.getText(); // If no selection, keep the current label
                }
                // Update the table model directly to reflect the change on the button's text immediately.
                // tableModel is the DefaultTableModel instance of the PdfSelectorGUI class.
                tableModel.setValueAt(label, editingRow, 1);
            }
            isPushed = false; // Reset pushed state
            return label; // Return the selected label
        }

        /**
         * Stops cell editing.
         * @return True if editing was stopped, false otherwise.
         */
        @Override
        public boolean stopCellEditing() {
            isPushed = false; // Reset pushed state
            return super.stopCellEditing(); // Calls DefaultCellEditor.stopCellEditing()
        }
    }

    /**
     * Handles the action of exporting data to an Excel file.
     * This method is triggered when the "Export to Excel" button is clicked.
     * It orchestrates the data extraction from selected PDFs, processing, and writing to an Excel sheet.
     * Uses a {@link javax.swing.SwingWorker} to perform lengthy operations in a background thread,
     * preventing the GUI from freezing and providing progress updates via a {@link ProgressDialog}.
     * @param event The {@link java.awt.event.ActionEvent} triggered by the button click.
     */
    public void exportToExcelAction(ActionEvent event) {
        loadLastSelectedDirectory(EXCEL); // Load the last directory used for saving Excel files
        JFileChooser fileChooser = new JFileChooser(); // Standard Swing file chooser dialog
        if (lastPdfDirectory != null) {fileChooser.setCurrentDirectory(lastPdfDirectory);}
        fileChooser.setDialogTitle("Select an Excel File or Type Name for New");
        fileChooser.setAcceptAllFileFilterUsed(false); // Do not show "All Files" by default
        // Filter for Excel files (.xls, .xlsx)
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files", "xls", "xlsx");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter); // Set this filter as the default
        fileChooser.setAcceptAllFileFilterUsed(true); // Re-enable "All Files" to allow typing new filenames

        // Show the save dialog. JFileChooser is from javax.swing.JFileChooser.
        int userSelection = fileChooser.showSaveDialog(null); // Parent component is null (centers on screen or parent if specified)

        if (userSelection == JFileChooser.APPROVE_OPTION) { // If user clicked "Save" or "Open"
            File fileToSave = fileChooser.getSelectedFile();

            // Ensure the file has an .xlsx extension if not provided by the user.
            if (!filter.accept(fileToSave)) { // filter.accept checks if file matches the filter (i.e., has a valid extension)
                fileToSave = new File(fileToSave.toString() + ".xlsx");
            }

            // ProgressDialog is a custom class (app.ProgressDialog)
            ProgressDialog progressDialog = new ProgressDialog(null, "Processing...");
            progressDialog.showDialog(); // Makes the progress dialog visible
            AtomicInteger progress = new AtomicInteger(0); // Thread-safe counter for progress updates

            File finalFileToSave = fileToSave; // Effectively final variable for use in lambda/inner class

            // SwingWorker (from javax.swing.SwingWorker) is used for background tasks.
            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception { // Runs on a background thread
                    Workbook workbook = null; // org.apache.poi.ss.usermodel.Workbook
                    Sheet sheet; // org.apache.poi.ss.usermodel.Sheet
                    TreeBuilder treeBuilder = new TreeBuilder(); // service.TreeBuilder
                    ExcelWriter writer = new ExcelWriter(); // service.ExcelWriter
                    List<PdfData> pdfList = new ArrayList<>(); // java.util.List of model.PdfData
                    List<String> fileTypeList = new ArrayList<>(); // java.util.List of String (file types)
                    List<String> lowConfList = new ArrayList<>(); // java.util.List for low confidence OCR content

                    // Check if the Excel file exists to decide whether to load or create it.
                    if (finalFileToSave.exists()) {
                        try (FileInputStream fis = new FileInputStream(finalFileToSave)) { // java.io.FileInputStream
                            // WorkbookFactory (from org.apache.poi.ss.usermodel.WorkbookFactory) creates a Workbook from an InputStream.
                            workbook = WorkbookFactory.create(fis);
                        } catch (Exception e) {
                            // JOptionPane (from javax.swing.JOptionPane) for showing error messages.
                            JOptionPane.showMessageDialog(null, "Failed to load the Excel file.", "Error", JOptionPane.ERROR_MESSAGE);
                            return null; // Exit if loading fails
                        }
                    } else {
                        // Create a new workbook if file doesn't exist. XSSFWorkbook is for .xlsx format.
                        workbook = new XSSFWorkbook(); // org.apache.poi.xssf.usermodel.XSSFWorkbook
                    }
                    if (workbook == null) {
                        JOptionPane.showMessageDialog(null, "Error creating/loading Excel workbook.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }

                    // Check if the "Data" sheet exists; create it if it doesn't.
                    if (workbook.getSheetIndex("Data") != -1) { // getSheetIndex returns -1 if sheet not found
                        sheet = workbook.getSheet("Data");
                        treeBuilder.buildTreeFromExcel(sheet); // Populate tree from existing Excel data
                        writer.clearSheet(sheet); // Clear the sheet before writing new data
                    } else {
                        sheet = workbook.createSheet("Data"); // Create new "Data" sheet
                        writer.writeBasic(workbook); // Write basic structure/headers to the new sheet
                    }

                    // Iterate through the selected PDFs and their types stored in typeSelection map.
                    // typeSelection is a Map<String, String> in PdfSelectorGUI.
                    typeSelection.forEach((pdfName, fileType) -> {
                        DataExtractor extractor = new DataExtractor(); // service.DataExtractor
                        // extractor.extractData processes a single PDF file.
                        extractor.extractData(new File(pdfName), progressDialog, progress, typeSelection.size());

                        try {
                            // extractor.processData1 likely extracts primary data from the PDF.
                            pdfList.add(extractor.processData1(extractor));
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error extracting data from " + pdfName + ".\nError:" + e, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        fileTypeList.add(fileType);

                        // Handle low confidence OCR content if any.
                        if (!extractor.getLowConfContent().isEmpty()) {
                            lowConfList.add("-------------------\n" + pdfName + "\n");
                            lowConfList.addAll(extractor.getLowConfContent());
                            // Reset the lowConfContent list to avoid duplication in case of multiple calls or files
                            extractor.setLowConfContent(new CopyOnWriteArrayList<>()); // java.util.concurrent.CopyOnWriteArrayList
                        }

                        // Check if the PDF was identified as containing multiple logical files/documents.
                        if (extractor.isMultipleFiles()) {
                            try {
                                // extractor.processData2 likely extracts data related to the second logical file.
                                pdfList.add(extractor.processData2(extractor));
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, "Error extracting secondary data from " + pdfName + ".\nError:" + e, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            fileTypeList.add("N/A"); // Add N/A type for the secondary part
                            if (!extractor.getLowConfContent().isEmpty()) { // Check again for low confidence from secondary processing
                                lowConfList.add("-------------------\n" + pdfName + " (secondary part)\n");
                                lowConfList.addAll(extractor.getLowConfContent());
                            }
                        }
                    });

                    try {
                        // treeBuilder.buildTreeFromPDF builds the hierarchical data structure from extracted PDF data.
                        treeBuilder.buildTreeFromPDF(pdfList, fileTypeList);
                        progressDialog.updateProgress(90); // Update progress towards completion
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Error writing data to Tree structure.\nError:" + e, "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    // Clean up memory by clearing large lists and suggesting garbage collection.
                    pdfList.clear();
                    fileTypeList.clear(); // Also clear fileTypeList
                    System.gc(); // Suggests that the Java Virtual Machine expend effort toward recycling unused objects

                    // Write the processed data to the Excel sheet.
                    // writer.writeToSheet is a method in service.ExcelWriter.
                    writer.writeToExcel(sheet, treeBuilder);
                    progressDialog.updateProgress(95); // Update progress

                    // Write low confidence content to a separate file.
                    if (!lowConfList.isEmpty()) {
                        Path lowConfPath = Paths.get(finalFileToSave.getParent(), "low_confidence_output.txt"); // java.nio.file.Paths, java.nio.file.Path
                        try {
                            // Files.write (java.nio.file.Files) writes lines to a file.
                            // StandardOpenOption.CREATE creates the file if it doesn't exist.
                            // StandardOpenOption.APPEND appends to the file if it exists.
                            // StandardOpenOption.WRITE opens the file for writing.
                            Files.write(lowConfPath, lowConfList, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Error writing low confidence output to file: " + e.getMessage(), "File Write Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    // Save the workbook to the selected file path.
                    try (FileOutputStream fos = new FileOutputStream(finalFileToSave)) { // java.io.FileOutputStream
                        workbook.write(fos); // Writes workbook content to the FileOutputStream
                        workbook.close(); // Closes the workbook, releasing resources (important!)
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Failed to save the Excel file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    progressDialog.updateProgress(100); // Final progress update
                    return null;
                }

                @Override
                protected void done() { // Runs on the Event Dispatch Thread after doInBackground completes
                    progressDialog.closeDialog(); // Close the progress dialog
                    // tableModel.getRowCount() is from javax.swing.table.DefaultTableModel
                    if (tableModel.getRowCount() > 0) { // If there are still PDFs listed
                        JOptionPane.showMessageDialog(null, "Excel file has been created/updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "No PDF files were processed.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    saveLastSelectedDirectory(EXCEL); // Save the directory for next time
                }
            };
            worker.execute(); // Executes the SwingWorker, starting the background task
        }
    }

    /**
     * Handles the action of selecting PDF files.
     * This method is triggered when the "Select PDF" button is clicked.
     * It opens a file chooser dialog allowing the user to select one or more PDF files.
     * Selected files are added to the JTable if they are not already present.
     * @param event The {@link java.awt.event.ActionEvent} triggered by the button click.
     */
    private void selectPdfAction(ActionEvent event) {
        loadLastSelectedDirectory(PDF); // Load the last directory used for selecting PDFs
        JFileChooser fileChooser = new JFileChooser(); // javax.swing.JFileChooser
        if (lastPdfDirectory != null) {
            fileChooser.setCurrentDirectory(lastPdfDirectory); // Start in the last used directory
        }
        // Allow selection of multiple files
        fileChooser.setMultiSelectionEnabled(true);
        // Filter for PDF files (.pdf)
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf")); // javax.swing.filechooser.FileNameExtensionFilter

        int returnValue = fileChooser.showOpenDialog(null); // Show the open dialog
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles(); // Get all selected files
            for (File file : selectedFiles) {
                if (!isPathInTable(file.getAbsolutePath())) { // Check if PDF is already in the table
                    // tableModel.addRow is from javax.swing.table.DefaultTableModel
                    // Adds a new row with PDF name and a default "Select File Type" button text.
                    tableModel.addRow(new Object[]{file.getAbsolutePath(), "Select File Type"});
                    typeSelection.put(file.getAbsolutePath(), "N/A"); // Default type to N/A
                } else {
                    // JOptionPane (from javax.swing.JOptionPane) for showing info messages.
                    JOptionPane.showMessageDialog(this, "File " + file.getName() + " is already in the list.", "File Exists", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            if (selectedFiles.length > 0) {
                lastPdfDirectory = selectedFiles[0].getParentFile(); // Update last PDF directory
                saveLastSelectedDirectory(PDF);
            }
        }
    }

    /**
     * Checks if a given file path is already present in the "PDF Name" column of the table.
     * @param path The absolute file path string to check.
     * @return True if the path exists in the table, false otherwise.
     */
    private boolean isPathInTable(String path) {
        // tableModel.getRowCount and tableModel.getValueAt are from javax.swing.table.DefaultTableModel
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(path)) {
                return true; // Path found
            }
        }
        return false; // Path not found
    }

    /**
     * Handles the action of deleting selected PDF files from the JTable.
     * This method is triggered when the "Delete PDF" button is clicked.
     * It removes the selected row(s) from the table and the corresponding entry from the typeSelection map.
     * @param event The {@link java.awt.event.ActionEvent} triggered by the button click.
     */
    private void deletePdfAction(ActionEvent event) {
        // table.getSelectedRows is from javax.swing.JTable
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            // Iterate backwards to avoid issues with row indices changing during removal
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int rowIndex = selectedRows[i];
                // Get PDF name from the table model before removing the row
                String pdfName = tableModel.getValueAt(rowIndex, 0).toString();
                typeSelection.remove(pdfName); // Remove from the type selection map
                // tableModel.removeRow is from javax.swing.table.DefaultTableModel
                tableModel.removeRow(rowIndex); // Remove row from the table model
            }
        } else {
            // JOptionPane (from javax.swing.JOptionPane) for showing warning messages.
            JOptionPane.showMessageDialog(this, "No PDF selected to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Saves the last selected directory for a given mode (PDF or Excel) to a preferences file.
     * This helps remember the user's last used folder for convenience.
     * The directory path is stored in a file named ".lastPdfDir.pref" or ".lastExcelDir.pref"
     * in the user's home directory.
     * @param mode Integer constant representing the type of directory (PDF or EXCEL).
     */
    private void saveLastSelectedDirectory(int mode) {
        String homeDir = System.getProperty("user.home"); // Gets the user's home directory path
        String fileName = (mode == PDF) ? ".lastPdfDir.pref" : ".lastExcelDir.pref";
        File prefsFile = new File(homeDir, fileName); // java.io.File

        File directoryToSave = (mode == PDF) ? lastPdfDirectory : lastExcelDirectory;

        if (directoryToSave != null) {
            // try-with-resources ensures the writer is closed. BufferedWriter and FileWriter are from java.io.
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(prefsFile))) {
                writer.write(directoryToSave.getAbsolutePath());
            } catch (IOException e) {
                // Log error or notify user if necessary, here it fails silently
                System.err.println("Error saving last directory preference: " + e.getMessage());
            }
        }
    }

    /**
     * Loads the last selected directory for a given mode (PDF or Excel) from a preferences file.
     * If the preference file exists and contains a valid directory path, it updates
     * `lastPdfDirectory` or `lastExcelDirectory`.
     * @param mode Integer constant representing the type of directory (PDF or EXCEL).
     */
    private void loadLastSelectedDirectory(int mode) {
        String homeDir = System.getProperty("user.home");
        String fileName = (mode == PDF) ? ".lastPdfDir.pref" : ".lastExcelDir.pref";
        File prefsFile = new File(homeDir, fileName);

        if (prefsFile.exists()) {
            // try-with-resources ensures the reader is closed. BufferedReader and FileReader are from java.io.
            try (BufferedReader reader = new BufferedReader(new FileReader(prefsFile))) {
                String path = reader.readLine();
                if (path != null && !path.trim().isEmpty()) {
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory()) {
                        if (mode == PDF) {
                            lastPdfDirectory = dir;
                        } else {
                            lastExcelDirectory = dir;
                        }
                    }
                }
            } catch (IOException e) {
                // Log error or notify user if necessary, here it fails silently
                System.err.println("Error loading last directory preference: " + e.getMessage());
            }
        }
    }
}

