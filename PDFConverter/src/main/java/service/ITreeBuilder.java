package service;

import model.PdfData;

import java.util.List;

public interface ITreeBuilder {
    void buildTreeFromPDF(List<PdfData> pdfDataList, List<String> fileType) throws Exception;
//    void buildTreeFromExcel(Sheet sheet);
}
