package service;

import enums.Fields;
import net.sourceforge.tess4j.ITesseract;
import readers.LineReader;
import readers.Reader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import static imgprocessor.PdfImageProcessor.convert;

public class DataExtractor {

    public void extractData(String filePath) {
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";

        try {
            Map<Integer, BufferedImage> imgs = convert(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LineReader lineReader = new LineReader();
        int curr_page_num = 0;

        for (Fields field : Fields.values()) {
            switch (field) {
                case FILE:

            }
        }
    }
}
