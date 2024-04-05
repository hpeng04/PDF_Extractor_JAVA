import model.PdfData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import service.DataExtractor;
import service.TreeBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Disabled
public class treeTest {

    @Test
    public void test1() {

        int[] list = {1, 2, 3, 4, 6, 7, 8};
        int[] temp = new int[list.length - 4];
        System.arraycopy(list, 4, temp, 0, list.length - 4);
        list[4] = 5;
        System.arraycopy(temp, 0, list, 5, temp.length - 1);
        System.out.println(Arrays.toString(list));
    }

    @Test void buildTreeTest() {
        String[] filePath = {"C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Test 3_imperial.pdf",
                "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Perf Path A.pdf",
                "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Perf Path B.pdf",
                "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Perf Path C.pdf",


        };
        List<PdfData> pdfList = new ArrayList<>();
        List<String> fileTypeList = new ArrayList<>();
        for (String path : filePath) {
            DataExtractor extractor = new DataExtractor();
            extractor.extractData(new File(path));
            try {
                pdfList.add(extractor.processData1(extractor));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            fileTypeList.add("ref");

        }
        TreeBuilder treeBuilder = new TreeBuilder();
        try {
            treeBuilder.buildTreeFromPDF(pdfList, fileTypeList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
