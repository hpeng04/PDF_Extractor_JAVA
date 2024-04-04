package app;

import model.PdfData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import service.DataExtractor;
import service.ExcelWriter;
import service.TreeBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PdfSelectorGUI extends JFrame {
    private final int WIDTH = 700;
    private final int HEIGHT = 525;
    private final int PDF = 0;
    private final int EXCEL = 1;
    private DefaultTableModel tableModel;
    private JTable table;
    private File lastPdfDirectory;
    private File lastExcelDirectory;
    private Map<String, String> typeSelection = new LinkedHashMap<>();

    public PdfSelectorGUI() {
        super("PDF Extractor");
        // Setting up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLayout(new BorderLayout(10, 10)); // Add some spacing between components

        // Button to select PDFs
        JButton selectPdfButton = new JButton("Select PDF");
        selectPdfButton.addActionListener(this::selectPdfAction);
        selectPdfButton.setFont(new Font("Arial", Font.BOLD, 14));
        // Button to delete selected PDFs
        JButton deletePdfButton = new JButton("Delete PDF");
        deletePdfButton.addActionListener(this::deletePdfAction);
        deletePdfButton.setFont(new Font("Arial", Font.BOLD, 14));

        // Initialize table model and table
        tableModel = new DefaultTableModel(new Object[]{"PDF Name", "File Type"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only File Type column is editable for buttons
            }
        };
        table = new JTable(tableModel);
        table.getColumn("File Type").setCellRenderer(new FileTypeButtonRenderer());
        table.getColumn("File Type").setCellEditor(new FileTypeButtonEditor(new JCheckBox()));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Disable auto-resizing
        table.setRowHeight(20);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth((int) (WIDTH * 0.69)); // Set width of "PDF Name"
        columnModel.getColumn(1).setPreferredWidth((int) (WIDTH * 0.3)); // Set width of "Action"
        JScrollPane scrollPane = new JScrollPane(table);
        // Export to Excel button
        JButton exportToExcelButton = new JButton("Export to Excel");
        exportToExcelButton.addActionListener(this::exportToExcelAction);
        exportToExcelButton.setFont(new Font("Arial", Font.BOLD, 14)); // Increase font size and set style
        exportToExcelButton.setMargin(new Insets(10, 0, 10, 0));

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectPdfButton);
        buttonPanel.add(deletePdfButton);

        // Adding components to frame
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.NORTH);
        add(exportToExcelButton, BorderLayout.SOUTH);
    }
    class FileTypeButtonRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class FileTypeButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private String pdfName;
        private String label;
        private int editingRow;

        public FileTypeButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            pdfName = table.getModel().getValueAt(row, 0).toString();
            String currentValue = (value != null) ? value.toString() : "Select File Type";
            button.setText(currentValue);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                String[] options = {"Proposed", "Reference", "N/A"};
                int selection = JOptionPane.showOptionDialog(button, "Select type for " + pdfName,
                        "Selection", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (selection >= 0) {
                    label = options[selection];
                    typeSelection.put(pdfName, label); // Update the selection map
                } else {
                    label = button.getText();
                }
                tableModel.setValueAt(label, editingRow, 1); // Update the button text in the table model
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    public void exportToExcelAction(ActionEvent event) {
        loadLastSelectedDirectory(EXCEL);
        JFileChooser fileChooser = new JFileChooser();
        if (lastExcelDirectory != null) {fileChooser.setCurrentDirectory(lastExcelDirectory);}
        fileChooser.setDialogTitle("Select an Excel File or Type Name for New");
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files", "xls", "xlsx");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        // Enable the "All Files" option to let users type in a new filename
        fileChooser.setAcceptAllFileFilterUsed(true);

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // Check if the user has provided an extension; if not, append ".xlsx"
            if (!filter.accept(fileToSave)) {
                fileToSave = new File(fileToSave.toString() + ".xlsx");
            }
            lastExcelDirectory = fileToSave.getParentFile();
            saveLastSelectedDirectory(EXCEL);

            ProgressDialog progressDialog = new ProgressDialog(null, "Processing...");
            progressDialog.showDialog();

            File finalFileToSave = fileToSave;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Workbook workbook = null;
                    Sheet sheet;
                    TreeBuilder treeBuilder = new TreeBuilder();
                    ExcelWriter writer = new ExcelWriter();
                    List<PdfData> pdfList = new ArrayList<>();
                    List<String> fileTypeList = new ArrayList<>();
                    List<String> lowConfList = new ArrayList<>();
                    // Check if the file exists to decide between creating a new Workbook or opening an existing one
                    if (finalFileToSave.exists()) {
                        // Load the existing workbook
                        try (FileInputStream fis = new FileInputStream(finalFileToSave)) {
                            workbook = WorkbookFactory.create(fis);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Failed to load the Excel file.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        // Create a new workbook
                        workbook = new XSSFWorkbook();
                    }
                    if (workbook == null) {
                        JOptionPane.showMessageDialog(null, "Error creating Excel workbook.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }

                    // Check if the "Data" sheet exists, create if it doesn't
                    if (workbook.getSheetIndex("Data") != -1) {
                        sheet = workbook.getSheet("Data");
                        treeBuilder.buildTreeFromExcel(sheet);
                        writer.clearSheet(sheet);
                    } else {
                        sheet = workbook.createSheet("Data");
                        writer.writeBasic(workbook);
                    }


                    typeSelection.forEach((pdfName, fileType) -> {
                        DataExtractor extractor = new DataExtractor();
                        extractor.extractData(new File(pdfName));
                        // 1st data
                        try {
                            pdfList.add(extractor.processData1(extractor));
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Error extracting data." +"\nError:"+ e, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        fileTypeList.add(fileType);
                        if (extractor.getLowConfContent().size() > 0) {
                            lowConfList.add("-------------------\n" + pdfName + "\n");
                            lowConfList.addAll(extractor.getLowConfContent());
                            // reset the lowConfContent list to avoid duplication
                            extractor.setLowConfContent(new CopyOnWriteArrayList<>());
                        }

                        // Check if the PDF contains multiple files
                        if (extractor.isMultipleFiles()) {
                            try {
                                pdfList.add(extractor.processData2(extractor));
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, "Error extracting data." +"\nError:"+ e, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            fileTypeList.add("N/A");
                            if (extractor.getLowConfContent().size() > 0) {
                                lowConfList.add("-------------------\n" + pdfName + "\n");
                                lowConfList.addAll(extractor.getLowConfContent());
                            }
                        }

                    });

                    try {
                        treeBuilder.buildTreeFromPDF(pdfList, fileTypeList);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Error Writing data to Tree." +"\nError:"+ e, "Error", JOptionPane.ERROR_MESSAGE);

                    }
                    // clean memory
                    pdfList.clear();
                    System.gc();

                    try {
                        writer.writeToExcel(sheet, treeBuilder);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Error Writing data to Excel." +"\nError:"+ e, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    treeBuilder = null;

                    if (lowConfList.size() > 0) {
                        String lowConfFilePath = finalFileToSave.getParent() + "/LowConfContent.txt";
                        Path path = Paths.get(lowConfFilePath);
                        try {
                            Files.write(path, lowConfList,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Error writing to Low Confidence file.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }



                    // Save the workbook
                    try (FileOutputStream out = new FileOutputStream(finalFileToSave)) {
                        workbook.write(out);
                        if (lowConfList.size() > 0) {
                            JOptionPane.showMessageDialog(null, "Excel file saved! However, " +
                                    "there are low confidence contents in the PDFs. Please check the LowConfContent.txt file for details.");
                        } else {
                            JOptionPane.showMessageDialog(null, "Excel file saved: " + finalFileToSave.getAbsolutePath());
                        }

                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Error writing to Excel file, please close the Excel file and retry.", "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        try {
                            workbook.close();
                            // clean memory
                            fileTypeList.clear();
                            lowConfList.clear();
                            System.gc();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    progressDialog.closeDialog();

                }
            };
            worker.execute();


        }
    }
    private void selectPdfAction(ActionEvent event) {
        loadLastSelectedDirectory(PDF);
        JFileChooser fileChooser = new JFileChooser();
        if (lastPdfDirectory != null) {fileChooser.setCurrentDirectory(lastPdfDirectory);}
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));


        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                String path = file.getAbsolutePath();
                // Check if the file path is already in the table
                if (!isPathInTable(path)) {
                    tableModel.addRow(new Object[]{path, "Select File Type"});
                    typeSelection.put(path, "");
                }
            }

            // Update lastDirectory with the directory of the last file selected
            if (files.length > 0) {
                lastPdfDirectory = files[files.length - 1].getParentFile();
            }
            saveLastSelectedDirectory(PDF);
        }
    }

    private boolean isPathInTable(String path) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String existingPath = (String) tableModel.getValueAt(i, 0); // Assuming path is in the first column
            if (existingPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void deletePdfAction(ActionEvent event) {
        // Get selected indices
        int[] selectedRows  = table.getSelectedRows();

        // Remove elements in reverse order to avoid index shifting
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            String pdfName = (String) tableModel.getValueAt(selectedRows[i], 0);
            typeSelection.remove(pdfName); // Update the map by removing the entry for the deleted PDF
            tableModel.removeRow(selectedRows[i]); // Remove the row from the table model
        }
    }
    private void saveLastSelectedDirectory(int mode) {
        Properties prop = new Properties();
        try (OutputStream output = new FileOutputStream("config.properties")) {
            if (lastPdfDirectory != null) prop.setProperty("lastPdfDirectory", lastPdfDirectory.getAbsolutePath());
            if (lastExcelDirectory != null) prop.setProperty("lastExcelDirectory", lastExcelDirectory.getAbsolutePath());
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    private void loadLastSelectedDirectory(int mode) {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            switch (mode) {
                case 0 -> { // PDF
                    String lastPdfDirPath = prop.getProperty("lastPdfDirectory");
                    if (lastPdfDirPath != null && !lastPdfDirPath.isEmpty()) {
                        lastPdfDirectory = new File(lastPdfDirPath);
                    }
                }
                case 1 -> { // Excel
                    String lastExcelDirPath = prop.getProperty("lastExcelDirectory");
                    if (lastExcelDirPath != null && !lastExcelDirPath.isEmpty()) {
                        lastExcelDirectory = new File(lastExcelDirPath);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }


}
