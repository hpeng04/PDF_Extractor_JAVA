package service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import utils.TreeNode;

public class ExcelWriter {
    private final int START_ROW = 2; // row 3
    private final int DATA_START_ROW = 4; // row 5
    private final int START_COL = 2; // col C
    private int currRow;
    private int currCol;

    public void writeToExcel(Sheet sheet, TreeBuilder treeBuilder) throws Exception {

        for (TreeNode<Object> parentTitle : treeBuilder.getTree().getChildren()) {
            // write the parent title at row 3
            Row row = sheet.getRow(START_ROW) == null ? sheet.createRow(START_ROW) : sheet.getRow(START_ROW);
            Cell cell = row.createCell(currCol);
            cell.setCellValue(parentTitle.getData().toString());

            if (parentTitle.getChildren() != null) {
                for (TreeNode<Object> childTitle : parentTitle.getChildren()) {
                    int currRow = START_ROW + 1;
                    // write the child title at the next row
                    row = sheet.getRow(currRow) == null ? sheet.createRow(currRow) : sheet.getRow(currRow);
                    cell = row.createCell(currCol);
                    cell.setCellValue(childTitle.getData().toString());
                    // write the data at the next row
                    if (childTitle.getChildren() != null) {
                        for (TreeNode<Object> data : childTitle.getChildren()) {
                            int dataRow = data.getFileIndex() + DATA_START_ROW;
                            row = sheet.getRow(dataRow) == null ? sheet.createRow(dataRow) : sheet.getRow(dataRow);
                            cell = row.createCell(currCol);
                            if (data.getData() != null) {
                                cell.setCellValue(data.getData().toString());
                            } else {
                                cell.setCellValue("");
                            }

                        }
                    }
                    currCol++;
                }
            }

        }

    }

    public void writeBasic(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Data");
        // Create a row. Row numbers are zero-based.
        Row row1 = sheet.createRow(0);

        // Create a cell style for the header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.LEFT);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        Font headerFont = workbook.createFont();
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setFontName("Arial");
        headerStyle.setFont(headerFont);

        // Merge A1-E1 and apply the style
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        Cell cellA1 = row1.createCell(0);
        cellA1.setCellValue("Permit # is user entered, all other fields are extracted using the tool");
        cellA1.setCellStyle(headerStyle);
    }

    public void clearSheet(Sheet sheet) {
        // Iterate backwards to avoid ConcurrentModificationException
        for (int i = sheet.getLastRowNum(); i > 1; i--) {
            sheet.removeRow(sheet.getRow(i));
        }
    }

}

