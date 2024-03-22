import model.PdfData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import service.DataExtractor;
import service.ExcelWriter;
import service.TreeBuilder;
import util.TreeNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Disabled
public class excelTest {

//    @Test
//    public void test1() {
//        String outPath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\java.xlsx";
//        ExcelWriter writer = new ExcelWriter();
//        boolean success = false;
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
////        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Perf Path A.pdf";
////        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";
//        String filePath1 = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Test 3_imperial.pdf";
//        String[] filePaths = {filePath, filePath1};
//        List<PdfData> list = new ArrayList<>();
//        for (String path : filePaths) {
//            File pdf = new File(path);
//            DataExtractor extractor = new DataExtractor();
//            extractor.extractData(pdf);
//            list.add(extractor.processData1(extractor));
//        }
//
//        TreeBuilder treeBuilder = new TreeBuilder();
//        treeBuilder.buildTreeFromPDF(list, "ref");
//
//
//        success = writer.writeToExcel(outPath, treeBuilder);
//    }

    @Test
    public void readTest() {
        String excelFilePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\java.xlsx";
        File file = new File(excelFilePath);
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    // Switch on cell type to handle different types
                    System.out.println(cell.getStringCellValue());

                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Test
//    public void writeTest() {
//        String readPath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\java.xlsx";
//        String outPath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\java_out.xlsx";
//        ExcelWriter writer = new ExcelWriter();
//        boolean success = false;
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath1 = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";
//        String[] filePaths = {filePath1};
//        TreeBuilder treeBuilder = new TreeBuilder();
//        treeBuilder.buildTreeFromExcel(readPath);
//        TreeNode<Object> tree = treeBuilder.getTree();
//
//        List<PdfData> list = new ArrayList<>();
//        for (String path : filePaths) {
//            File pdf = new File(path);
//            DataExtractor extractor = new DataExtractor();
//            extractor.extractData(pdf);
//            list.add(extractor.processData1(extractor));
//        }
//        treeBuilder.buildTreeFromPDF(list, "ref");
//        success = writer.writeToExcel(outPath, treeBuilder);
//    }

}
