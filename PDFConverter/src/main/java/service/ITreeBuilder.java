package service;

import model.PdfData;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

public interface ITreeBuilder {
    void buildTreeFromPDF(List<PdfData> pdfDataList, List<String> fileType);
//    void buildTreeFromExcel(Sheet sheet);
}
