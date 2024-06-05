import app.ProgressDialog;
import model.PdfData;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import service.DataExtractor;
import service.TreeBuilder;
import utils.TreeNode;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger progress = new AtomicInteger(0);
        extractor.extractData(pdf, new ProgressDialog(null, "Processing..."), progress, 1);
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
        String[] a = {"abc", "2", "10", "0"};
        List<String> l = Arrays.asList(a);
        Collections.sort(l);
        String b = "strawberries";
        System.out.println(b.substring(2,5));
    }
}
