import model.Fields;
import model.PdfData;
import net.sourceforge.tess4j.TesseractException;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import service.DataExtractor;
import service.TreeBuilder;
import util.TreeNode;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Disabled
public class mainTest {

    @Test
    public void extractorTest() {

        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Perf Path A.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Test 3_imperial.pdf";

        File pdf = new File(filePath);

        ConcurrentHashMap<String, Object> map2;
        DataExtractor extractor = new DataExtractor();
        PdfData pdfData = new PdfData();
        extractor.extractData(pdf);
        List<String> lowConfContent = extractor.getLowConfContent();
        try {
            pdfData = extractor.processData1(extractor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<PdfData> list = new ArrayList<>();
        list.add(pdfData);

        TreeBuilder treeBuilder = new TreeBuilder();
        try {
            treeBuilder.buildTreeFromPDF(list, Collections.singletonList("ref"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TreeNode<Object> tree = treeBuilder.getTree();
//        pdfData.getFile();

        HashMap<String, List<String>> buildingAssemblyDetails = pdfData.getBuildingAssemblyDetails();
//        if (extractor.isMultipleFiles()) {
//            map2 = extractor.processData2(extractor);
//        }
//        ConcurrentHashMap<String, Object> map = extractor.processData1(extractor);
        System.out.println(extractor.isSIUnit());
        System.out.println(extractor.isMultipleFiles());

//        for (Fields field : Fields.values()) {
//            if (map.containsKey(field.getTitle())) {
//                System.out.println(field.getTitle());
//                System.out.println(map.get(field.getTitle()));
//                System.out.println("-----------------");
//            }
//        }

//        map.forEach((key, value) -> System.out.println(key + ": " + value + "\n---------------"));
//        map.forEach((key, value) -> System.out.println(value + "\n---------------"));
    }


    @Test
    public void allContentTest() throws TesseractException {
        float a = 1234;
        System.out.println((double) (a/=Math.pow(10, 1)));
    }
}
